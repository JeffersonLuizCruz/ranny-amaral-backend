package com.ranny.fotografia.contrato.service;

import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.arquivo.ArquivoStatus;
import com.ranny.fotografia.arquivo.ArquivoUpload;
import com.ranny.fotografia.cliente.ClienteConsulta;
import com.ranny.fotografia.cliente.ClienteResumo;
import com.ranny.fotografia.cliente.ContatoCliente;
import com.ranny.fotografia.contrato.api.AceitePublicoDtos;
import com.ranny.fotografia.contrato.api.ContratoResponse;
import com.ranny.fotografia.contrato.api.SolicitacaoAceiteResponse;
import com.ranny.fotografia.contrato.model.Contrato;
import com.ranny.fotografia.contrato.model.SolicitacaoAceite;
import com.ranny.fotografia.contrato.model.documento.DadosAceite;
import com.ranny.fotografia.contrato.model.documento.StatusAceite;
import com.ranny.fotografia.contrato.model.documento.StatusSolicitacaoAceite;
import com.ranny.fotografia.contrato.repository.ContratoRepository;
import com.ranny.fotografia.contrato.repository.SolicitacaoAceiteRepository;
import com.ranny.fotografia.shared.exception.ConflitoException;
import com.ranny.fotografia.shared.exception.RecursoExpiradoException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.model.ComprovantePagamento;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;

/**
 * Casos de uso publicos do aceite.
 *
 * <p>PATTERN: Facade + Token Capability + Specification de disponibilidade.
 *
 * <p>O token e a unica credencial do cliente. Antes de qualquer operacao o
 * service valida existencia, expiracao e estado; links expirados sao marcados e
 * respondem 410, links ja aceitos nao podem ser reutilizados (409). O aceite
 * cria/reaproveita o cliente, gera protocolo e registra a trilha de auditoria
 * (IP, user-agent e hash do conteudo).
 */
@Service
public class AceitePublicoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final SolicitacaoAceiteRepository solicitacaoRepository;
    private final ContratoRepository contratoRepository;
    private final ClienteConsulta clienteConsulta;
    private final ContratoNumerador numerador;
    private final ApplicationEventPublisher eventos;
    private final ArquivoUpload arquivoUpload;

    public AceitePublicoService(SolicitacaoAceiteRepository solicitacaoRepository,
                                ContratoRepository contratoRepository,
                                ClienteConsulta clienteConsulta,
                                ContratoNumerador numerador,
                                ApplicationEventPublisher eventos,
                                ArquivoUpload arquivoUpload) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.contratoRepository = contratoRepository;
        this.clienteConsulta = clienteConsulta;
        this.numerador = numerador;
        this.eventos = eventos;
        this.arquivoUpload = arquivoUpload;
    }

    @Transactional(noRollbackFor = RecursoExpiradoException.class)
    public AceitePublicoDtos.ContratoPublicoResponse consultar(String token) {
        SolicitacaoAceite solicitacao = porToken(token);
        Contrato contrato = contratoDa(solicitacao);
        validarExpiracao(solicitacao, contrato);
        validarDisponibilidade(solicitacao);
        return new AceitePublicoDtos.ContratoPublicoResponse(
                SolicitacaoAceiteResponse.de(solicitacao), ContratoResponse.de(contrato));
    }

    @Transactional(noRollbackFor = RecursoExpiradoException.class)
    public void visualizar(String token) {
        SolicitacaoAceite solicitacao = porToken(token);
        Contrato contrato = contratoDa(solicitacao);
        validarExpiracao(solicitacao, contrato);
        if (solicitacao.getStatus().estaAberta()) {
            solicitacao.visualizar(Instant.now());
            contrato.registrarVisualizacao();
        }
    }

    @Transactional(noRollbackFor = RecursoExpiradoException.class)
    public AceitePublicoDtos.AceiteResponse aceitar(String token, AceitePublicoDtos.AceiteRequest request,
                                                    String ip, String userAgent) {
        SolicitacaoAceite solicitacao = porToken(token);
        Contrato contrato = contratoDa(solicitacao);
        validarExpiracao(solicitacao, contrato);
        validarDisponibilidade(solicitacao);

        if (solicitacao.getStatus() == StatusSolicitacaoAceite.ACEITA) {
            throw new ConflitoException("LINK_JA_ACEITO", "Este contrato ja foi aceito.");
        }
        if (!solicitacao.getStatus().estaAberta()) {
            throw new RecursoExpiradoException("LINK_INDISPONIVEL",
                    "Este link de aceite nao esta mais disponivel.");
        }
        if (contrato.getStatusAceite() == StatusAceite.ACEITO) {
            throw new ConflitoException("CONTRATO_JA_ACEITO", "Este contrato ja foi aceito.");
        }

        ClienteResumo cliente = clienteConsulta.obterOuCriar(new ContatoCliente(
                request.nomeCompleto().trim(),
                request.whatsapp(),
                null,
                request.documento(),
                request.tipoDocumento(),
                request.cidade()));

        Instant aceitoEm = Instant.now();
        String protocolo = numerador.proximoProtocolo(LocalDate.now(FUSO).getYear());
        DadosAceite dados = new DadosAceite(
                request.nomeCompleto().trim(),
                request.tipoDocumento(),
                request.documento(),
                request.cidade(),
                request.whatsapp(),
                aceitoEm,
                protocolo,
                ip,
                userAgent,
                hashConteudo(contrato.getNumero(), request.documento(), aceitoEm));

        contrato.registrarAceite(dados, cliente.id(), solicitacao.getId());
        solicitacao.aceitar(dados);

        publicar(contrato);
        return new AceitePublicoDtos.AceiteResponse(
                protocolo, ContratoResponse.de(contrato), SolicitacaoAceiteResponse.de(solicitacao));
    }

    @Transactional(noRollbackFor = RecursoExpiradoException.class)
    public AceitePublicoDtos.UploadSessaoResponse iniciarUploadComprovante(
            String token, AceitePublicoDtos.IniciarUploadRequest request) {
        ContextoPublico contexto = contextoDisponivel(token);
        Contrato contrato = contexto.contrato();
        ArquivoUpload.IniciarUpload comando = new ArquivoUpload.IniciarUpload(
                ArquivoFinalidade.COMPROVANTE_PAGAMENTO,
                contrato.getId(),
                "estudio/contratos/%s/comprovantes".formatted(contrato.getId()),
                request.nomeArquivo(),
                request.mimeType(),
                request.tamanho(),
                request.checksumSha256(),
                arquivoUpload.tamanhoMaximo(ArquivoFinalidade.COMPROVANTE_PAGAMENTO));
        return AceitePublicoDtos.UploadSessaoResponse.de(arquivoUpload.iniciar(comando));
    }

    @Transactional(noRollbackFor = RecursoExpiradoException.class)
    public AceitePublicoDtos.ComprovanteResponse concluirUploadComprovante(
            String token, String arquivoId, AceitePublicoDtos.ConcluirUploadRequest request) {
        ContextoPublico contexto = contextoDisponivel(token);
        Contrato contrato = contexto.contrato();
        List<ArquivoUpload.ParteEnviada> partes = request.partes().stream()
                .map(parte -> new ArquivoUpload.ParteEnviada(parte.partNumber(), parte.etag()))
                .toList();

        ArquivoUpload.ArquivoResumo arquivo = arquivoUpload.concluir(
                arquivoId, ArquivoFinalidade.COMPROVANTE_PAGAMENTO, contrato.getId(), partes);

        ComprovantePagamento comprovante = new ComprovantePagamento(
                arquivo.objectKey(),
                arquivo.nomeOriginal(),
                arquivo.mimeType(),
                arquivo.tamanhoBytes(),
                arquivo.concluidoEm() == null ? Instant.now() : arquivo.concluidoEm());
        contrato.registrarComprovante(arquivo.id(), comprovante);
        publicar(contrato);
        return new AceitePublicoDtos.ComprovanteResponse(
                ContratoResponse.de(contrato),
                SolicitacaoAceiteResponse.de(contexto.solicitacao()));
    }

    @Transactional(noRollbackFor = RecursoExpiradoException.class)
    public void abortarUploadComprovante(String token, String arquivoId) {
        ContextoPublico contexto = contextoDisponivel(token);
        arquivoUpload.abortar(arquivoId, ArquivoFinalidade.COMPROVANTE_PAGAMENTO,
                contexto.contrato().getId());
    }

    @Transactional(noRollbackFor = RecursoExpiradoException.class)
    public AceitePublicoDtos.UploadStatusResponse statusUploadComprovante(String token, String arquivoId) {
        ContextoPublico contexto = contextoLeitura(token);
        ArquivoUpload.ArquivoResumo arquivo = arquivoUpload.buscar(
                        arquivoId, ArquivoFinalidade.COMPROVANTE_PAGAMENTO,
                        contexto.contrato().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo", arquivoId));
        return new AceitePublicoDtos.UploadStatusResponse(
                arquivo.id(), arquivo.status().name(), arquivo.status() == ArquivoStatus.DISPONIVEL);
    }

    private ContextoPublico contextoDisponivel(String token) {
        ContextoPublico contexto = contextoLeitura(token);
        validarDisponibilidade(contexto.solicitacao());
        if (contexto.contrato().getStatusAceite() == StatusAceite.CANCELADO) {
            throw new RecursoExpiradoException("CONTRATO_CANCELADO", "Este contrato foi cancelado.");
        }
        return contexto;
    }

    private ContextoPublico contextoLeitura(String token) {
        SolicitacaoAceite solicitacao = porToken(token);
        Contrato contrato = contratoDa(solicitacao);
        validarExpiracao(solicitacao, contrato);
        return new ContextoPublico(solicitacao, contrato);
    }

    private record ContextoPublico(SolicitacaoAceite solicitacao, Contrato contrato) {
    }

    private SolicitacaoAceite porToken(String token) {
        return solicitacaoRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link de aceite", token));
    }

    private Contrato contratoDa(SolicitacaoAceite solicitacao) {
        return contratoRepository.findById(solicitacao.getContratoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato", solicitacao.getContratoId()));
    }

    /**
     * SPECIFICATION: link expirado e marcado no contrato e na solicitacao antes
     * de responder 410. Isso deixa o estado consistente para o CRM.
     */
    private void validarExpiracao(SolicitacaoAceite solicitacao, Contrato contrato) {
        if (solicitacao.estaExpirada(Instant.now())) {
            solicitacao.expirar();
            contrato.expirar();
            throw new RecursoExpiradoException("LINK_EXPIRADO", "Este link de aceite expirou.");
        }
    }

    /** Bloqueia links que ja foram expirados ou cancelados anteriormente. */
    private void validarDisponibilidade(SolicitacaoAceite solicitacao) {
        if (solicitacao.getStatus() == StatusSolicitacaoAceite.CANCELADA) {
            throw new RecursoExpiradoException("LINK_CANCELADO", "Este link de aceite foi cancelado.");
        }
        if (solicitacao.getStatus() == StatusSolicitacaoAceite.EXPIRADA) {
            throw new RecursoExpiradoException("LINK_EXPIRADO", "Este link de aceite expirou.");
        }
    }

    private void publicar(Contrato contrato) {
        eventos.publishEvent(ContratoEventoFactory.de(contrato));
    }

    private static String hashConteudo(String numero, String documento, Instant aceitoEm) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String conteudo = numero + "|" + documento + "|" + aceitoEm;
            return HexFormat.of().formatHex(digest.digest(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
