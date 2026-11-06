package com.ranny.fotografia.arquivo.service;

import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.arquivo.ArquivoStatus;
import com.ranny.fotografia.arquivo.ArquivoUpload;
import com.ranny.fotografia.arquivo.model.Arquivo;
import com.ranny.fotografia.arquivo.repository.ArquivoRepository;
import com.ranny.fotografia.arquivo.storage.ObjectStorage;
import com.ranny.fotografia.arquivo.storage.StorageProperties;
import com.ranny.fotografia.shared.exception.ArquivoInvalidoException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Orquestra o ciclo de vida dos arquivos e dos uploads pre-assinados.
 *
 * <p>PATTERN: Facade + State Machine + Transaction Synchronization (Unit of Work).
 *
 * <p>O navegador envia os bytes direto ao object storage. O service apenas
 * valida metadados, cria a sessao, assina URLs e confirma o objeto ao final. Em
 * caso de rollback, o objeto recem-criado e removido para nao deixar lixo.
 */
@Service
public class ArquivoService implements ArquivoUpload {

    private static final Logger log = LoggerFactory.getLogger(ArquivoService.class);

    private final ArquivoRepository repository;
    private final ObjectStorage storage;
    private final StorageProperties properties;
    private final ValidadorArquivo validador;

    public ArquivoService(ArquivoRepository repository, ObjectStorage storage,
                          StorageProperties properties, ValidadorArquivo validador) {
        this.repository = repository;
        this.storage = storage;
        this.properties = properties;
        this.validador = validador;
    }

    @Override
    @Transactional
    public SessaoUpload iniciar(IniciarUpload comando) {
        ValidadorArquivo.ArquivoValidado validado = validador.validarInicio(
                comando.finalidade(), comando.nomeOriginal(), comando.mimeType(),
                comando.tamanhoBytes(), comando.checksumSha256(), comando.tamanhoMaximoBytes());

        String checksum = comando.checksumSha256().toLowerCase(Locale.ROOT);
        Optional<Arquivo> existente = repository
                .findFirstByFinalidadeAndReferenciaIdAndChecksumSha256AndStatus(
                        comando.finalidade(), comando.referenciaId(), checksum, ArquivoStatus.DISPONIVEL);
        if (existente.isPresent()) {
            Arquivo arquivo = existente.get();
            return new SessaoUpload(arquivo.getId(), null, true, EstrategiaUpload.REUTILIZADO,
                    arquivo.getTamanhoBytes(), 0, List.of(), null);
        }

        String arquivoId = java.util.UUID.randomUUID().toString();
        String objectKey = montarChave(comando.prefixo(), arquivoId, validado.extensao());
        boolean multipart = comando.tamanhoBytes() > properties.getMultipartThresholdBytes();
        String uploadId = multipart
                ? storage.criarMultipart(objectKey, validado.mimeType())
                : null;

        Arquivo arquivo = Arquivo.iniciar(
                arquivoId,
                comando.finalidade(),
                comando.referenciaId(),
                properties.getProvider(),
                properties.getBucket(),
                objectKey,
                validado.nomeOriginal(),
                validado.mimeType(),
                comando.tamanhoBytes(),
                checksum,
                uploadId);
        repository.save(arquivo);

        Duration validade = Duration.ofSeconds(properties.getPresignSeconds());
        Instant expiraEm = Instant.now().plus(validade);
        if (!multipart) {
            String url = storage.urlPut(objectKey, validado.mimeType(), validade);
            return new SessaoUpload(arquivo.getId(), null, false, EstrategiaUpload.PUT,
                    comando.tamanhoBytes(), 0,
                    List.of(new UrlParte(1, url)), expiraEm);
        }

        int totalPartes = (int) Math.ceil((double) comando.tamanhoBytes() / properties.getPartSizeBytes());
        List<String> urls = storage.urlsPartes(objectKey, uploadId, totalPartes, validade);
        List<UrlParte> partes = java.util.stream.IntStream.range(0, urls.size())
                .mapToObj(indice -> new UrlParte(indice + 1, urls.get(indice)))
                .toList();
        return new SessaoUpload(arquivo.getId(), uploadId, false, EstrategiaUpload.MULTIPART,
                comando.tamanhoBytes(), properties.getPartSizeBytes(), partes, expiraEm);
    }

    @Override
    @Transactional
    public ArquivoResumo concluir(String arquivoId, ArquivoFinalidade finalidade,
                                  String referenciaId, List<ParteEnviada> partes) {
        Arquivo arquivo = obter(arquivoId, finalidade, referenciaId);
        if (arquivo.estaDisponivel()) {
            return resumo(arquivo);
        }
        if (!arquivo.estaPendente()) {
            throw new RegraDeNegocioException("ARQUIVO_NAO_PENDENTE",
                    "Este arquivo nao esta mais aguardando envio.");
        }

        arquivo.marcarEnviando();
        ObjectStorage.ObjectMetadata metadata = arquivo.getUploadId() == null
                ? storage.metadados(arquivo.getObjectKey())
                : storage.concluirMultipart(arquivo.getObjectKey(), arquivo.getUploadId(), partes);

        if (metadata.tamanhoBytes() != arquivo.getTamanhoBytes()) {
            descartar(arquivo);
            throw new ArquivoInvalidoException("ARQUIVO_TAMANHO_DIVERGENTE",
                    "O tamanho do arquivo enviado nao corresponde ao informado.");
        }

        TipoArquivo detectado = validador.validarConteudo(
                storage.primeirosBytes(arquivo.getObjectKey(), 16));
        if (!detectado.mimeType().equalsIgnoreCase(arquivo.getMimeType())) {
            descartar(arquivo);
            throw new ArquivoInvalidoException("ARQUIVO_TIPO_DIVERGENTE",
                    "O conteudo do arquivo nao corresponde ao tipo informado.");
        }

        arquivo.marcarDisponivel(detectado.mimeType(), Instant.now());
        registrarRemocaoEmRollback(arquivo.getObjectKey());
        return resumo(arquivo);
    }

    @Override
    @Transactional
    public void abortar(String arquivoId, ArquivoFinalidade finalidade, String referenciaId) {
        Arquivo arquivo = obter(arquivoId, finalidade, referenciaId);
        if (arquivo.estaDisponivel()) {
            return;
        }
        if (arquivo.getUploadId() != null) {
            storage.abortarMultipart(arquivo.getObjectKey(), arquivo.getUploadId());
        }
        storage.remover(arquivo.getObjectKey());
        arquivo.cancelar();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArquivoResumo> buscar(String arquivoId, ArquivoFinalidade finalidade,
                                          String referenciaId) {
        return repository.findByIdAndFinalidadeAndReferenciaId(arquivoId, finalidade, referenciaId)
                .map(ArquivoService::resumo);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] abrirConteudo(String arquivoId, ArquivoFinalidade finalidade,
                                String referenciaId) {
        Arquivo arquivo = obter(arquivoId, finalidade, referenciaId);
        if (!arquivo.estaDisponivel()) {
            throw new RecursoNaoEncontradoException("Arquivo", arquivoId);
        }
        return storage.ler(arquivo.getObjectKey());
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream abrirStream(String arquivoId, ArquivoFinalidade finalidade,
                                   String referenciaId) {
        Arquivo arquivo = obter(arquivoId, finalidade, referenciaId);
        if (!arquivo.estaDisponivel()) {
            throw new RecursoNaoEncontradoException("Arquivo", arquivoId);
        }
        return storage.abrir(arquivo.getObjectKey());
    }

    @Override
    @Transactional
    public ArquivoResumo salvarDisponivel(IniciarArquivo comando, byte[] conteudo) {
        String checksum = sha256(conteudo);
        ValidadorArquivo.ArquivoValidado validado = validador.validarInicio(
                comando.finalidade(), comando.nomeOriginal(), comando.mimeType(),
                conteudo.length, checksum, tamanhoMaximo(comando.finalidade()));

        String arquivoId = java.util.UUID.randomUUID().toString();
        String objectKey = montarChave(comando.prefixo(), arquivoId, validado.extensao());
        storage.salvar(objectKey, conteudo, validado.mimeType());

        Arquivo arquivo = Arquivo.iniciar(
                arquivoId,
                comando.finalidade(),
                comando.referenciaId(),
                properties.getProvider(),
                properties.getBucket(),
                objectKey,
                validado.nomeOriginal(),
                validado.mimeType(),
                conteudo.length,
                checksum,
                null);
        arquivo.marcarDisponivel(validado.mimeType(), Instant.now());
        return resumo(repository.save(arquivo));
    }

    @Override
    @Transactional(readOnly = true)
    public long tamanhoMaximo(ArquivoFinalidade finalidade) {
        return switch (finalidade) {
            case COMPROVANTE_PAGAMENTO -> properties.getMaxComprovanteBytes();
            case FOTO_ORIGINAL, FOTO_EDITADA -> 50L * 1024 * 1024;
            case VIDEO_ORIGINAL -> 500L * 1024 * 1024;
            default -> properties.getMaxComprovanteBytes();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public UrlDownload urlDownload(String arquivoId, ArquivoFinalidade finalidade, String referenciaId) {
        Arquivo arquivo = obter(arquivoId, finalidade, referenciaId);
        if (!arquivo.estaDisponivel()) {
            throw new RecursoNaoEncontradoException("Arquivo", arquivoId);
        }
        Duration validade = Duration.ofSeconds(properties.getPresignSeconds());
        return new UrlDownload(storage.urlGet(arquivo.getObjectKey(), validade),
                Instant.now().plus(validade));
    }

    private Arquivo obter(String arquivoId, ArquivoFinalidade finalidade, String referenciaId) {
        return repository.findByIdAndFinalidadeAndReferenciaId(arquivoId, finalidade, referenciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo", arquivoId));
    }

    private void descartar(Arquivo arquivo) {
        try {
            storage.remover(arquivo.getObjectKey());
        } catch (RuntimeException ex) {
            log.warn("Falha ao remover arquivo invalido {}: {}", arquivo.getObjectKey(), ex.getMessage());
        }
        arquivo.falhar();
    }

    private void registrarRemocaoEmRollback(String objectKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        storage.remover(objectKey);
                    } catch (RuntimeException ex) {
                        log.warn("Falha ao remover arquivo apos rollback {}: {}",
                                objectKey, ex.getMessage());
                    }
                }
            }
        });
    }

    private static String montarChave(String prefixo, String arquivoId, String extensao) {
        String base = prefixo == null ? "arquivos" : prefixo.replaceAll("^/+|/+$", "");
        return "%s/%s/original.%s".formatted(base, arquivoId, extensao);
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }

    private static ArquivoResumo resumo(Arquivo arquivo) {
        return new ArquivoResumo(
                arquivo.getId(),
                arquivo.getFinalidade(),
                arquivo.getReferenciaId(),
                arquivo.getObjectKey(),
                arquivo.getNomeOriginal(),
                arquivo.getMimeType(),
                arquivo.getTamanhoBytes(),
                arquivo.getChecksumSha256(),
                arquivo.getStatus(),
                arquivo.getConcluidoEm());
    }
}
