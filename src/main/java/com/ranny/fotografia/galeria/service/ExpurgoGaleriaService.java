package com.ranny.fotografia.galeria.service;

import com.ranny.fotografia.arquivo.ArquivoConsulta;
import com.ranny.fotografia.galeria.GaleriaConsulta;
import com.ranny.fotografia.galeria.api.ExpurgoDtos;
import com.ranny.fotografia.galeria.event.ExpurgoSolicitadoEvent;
import com.ranny.fotografia.galeria.model.EventoGaleria;
import com.ranny.fotografia.galeria.model.EventoGaleriaTipo;
import com.ranny.fotografia.galeria.model.ExpurgoStatus;
import com.ranny.fotografia.galeria.model.Galeria;
import com.ranny.fotografia.galeria.model.LinkGaleria;
import com.ranny.fotografia.galeria.repository.EventoGaleriaRepository;
import com.ranny.fotografia.galeria.repository.GaleriaRepository;
import com.ranny.fotografia.galeria.repository.LinkGaleriaRepository;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expurgo dos binarios de uma galeria.
 *
 * <p>O expurgo e irreversivel e exclusivo do perfil ADMIN (garantido no
 * controller). Metadados, historico, financeiro e comprovantes de pagamento
 * permanecem intactos; apenas os objetos da midia saem do storage.
 */
@Service
public class ExpurgoGaleriaService {

    private static final Logger log = LoggerFactory.getLogger(ExpurgoGaleriaService.class);
    private static final String CONFIRMACAO_PADRAO = "EXPURGAR";

    private final GaleriaRepository galeriaRepository;
    private final LinkGaleriaRepository linkRepository;
    private final EventoGaleriaRepository eventoRepository;
    private final ArquivoConsulta arquivoConsulta;
    private final GaleriaService galeriaService;
    private final ApplicationEventPublisher eventos;

    public ExpurgoGaleriaService(GaleriaRepository galeriaRepository,
                                 LinkGaleriaRepository linkRepository,
                                 EventoGaleriaRepository eventoRepository,
                                 ArquivoConsulta arquivoConsulta,
                                 GaleriaService galeriaService,
                                 ApplicationEventPublisher eventos) {
        this.galeriaRepository = galeriaRepository;
        this.linkRepository = linkRepository;
        this.eventoRepository = eventoRepository;
        this.arquivoConsulta = arquivoConsulta;
        this.galeriaService = galeriaService;
        this.eventos = eventos;
    }

    /**
     * Marca o ZIP como concluido depois que o stream fecha com sucesso.
     *
     * <p>Chamado pelo controller somente quando {@code escreverZip} retorna sem
     * excecao; um download interrompido nao chega aqui.
     */
    @Transactional
    public void registrarZipConcluido(String token) {
        LinkGaleria link = linkRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link de galeria", token));
        if (!link.estaAtivo()) {
            return;
        }
        Galeria galeria = obter(link.getGaleriaId());
        if (galeria.getZipConcluidoEm() != null) {
            return;
        }
        galeria.registrarZipConcluido();
        evento(galeria, EventoGaleriaTipo.DOWNLOAD_REGISTRADO,
                "ZIP da entrega concluido pelo cliente.");
    }

    @Transactional(readOnly = true)
    public ExpurgoDtos.ArmazenamentoGaleria detalhe(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        GaleriaConsulta.ResumoArmazenamento resumo = galeriaService.resumoArmazenamento(galeriaId);
        List<ArquivoConsulta.ArquivoResumo> arquivos = arquivoConsulta.arquivosDaReferencia(galeriaId);

        Map<String, long[]> porFinalidade = new LinkedHashMap<>();
        for (ArquivoConsulta.ArquivoResumo arquivo : arquivos) {
            long[] acumulado = porFinalidade.computeIfAbsent(
                    arquivo.finalidade(), chave -> new long[2]);
            acumulado[0] += arquivo.tamanhoBytes();
            acumulado[1] += 1;
        }
        List<ExpurgoDtos.GrupoFinalidade> grupos = new ArrayList<>();
        porFinalidade.forEach((finalidade, valores) -> grupos.add(
                new ExpurgoDtos.GrupoFinalidade(finalidade, valores[0], valores[1])));
        grupos.sort(Comparator.comparingLong(ExpurgoDtos.GrupoFinalidade::bytes).reversed());

        long total = arquivos.stream().mapToLong(ArquivoConsulta.ArquivoResumo::tamanhoBytes).sum();
        return new ExpurgoDtos.ArmazenamentoGaleria(
                galeria.getId(),
                galeria.getClienteNome(),
                galeria.getStatus().valor(),
                total,
                arquivos.size(),
                grupos,
                resumo.elegivel(),
                resumo.motivoBloqueio(),
                resumo.entregueEm(),
                resumo.zipBaixadoEm(),
                resumo.zipConcluidoEm(),
                resumo.expurgoStatus(),
                galeria.getExpurgoSolicitadoEm(),
                galeria.getExpurgoSolicitadoPor(),
                galeria.getExpurgoMotivo(),
                resumo.expurgoEfetivadoEm(),
                resumo.bytesExpurgados(),
                confirmacaoEsperada(galeria));
    }

    @Transactional
    public ExpurgoDtos.ExpurgoResponse solicitar(String galeriaId, String motivo,
                                                 String confirmacao, String por) {
        Galeria galeria = obter(galeriaId);
        GaleriaConsulta.ResumoArmazenamento resumo = galeriaService.resumoArmazenamento(galeriaId);
        if (!resumo.elegivel()) {
            throw new RegraDeNegocioException("EXPURGO_NAO_ELEGIVEL",
                    resumo.motivoBloqueio() == null
                            ? "A galeria nao pode ser expurgada agora."
                            : resumo.motivoBloqueio());
        }
        String esperada = confirmacaoEsperada(galeria);
        if (confirmacao == null
                || !esperada.equalsIgnoreCase(confirmacao.trim())) {
            throw new RegraDeNegocioException("CONFIRMACAO_INVALIDA",
                    "Digite exatamente \"" + esperada + "\" para confirmar o expurgo.");
        }

        long bytes = arquivoConsulta.arquivosDaReferencia(galeriaId).stream()
                .mapToLong(ArquivoConsulta.ArquivoResumo::tamanhoBytes)
                .sum();
        galeria.solicitarExpurgo(por, motivo);
        evento(galeria, EventoGaleriaTipo.EXPURGO_SOLICITADO,
                "Expurgo solicitado por %s: %s".formatted(
                        galeria.getExpurgoSolicitadoPor(), motivo));
        eventos.publishEvent(new ExpurgoSolicitadoEvent(galeriaId));
        return new ExpurgoDtos.ExpurgoResponse(galeriaId,
                galeria.getExpurgoStatus().valor(), bytes,
                arquivoConsulta.arquivosDaReferencia(galeriaId).size());
    }

    @Transactional
    public void cancelar(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        if (galeria.getExpurgoStatus() != ExpurgoStatus.AGENDADO) {
            throw new RegraDeNegocioException("EXPURGO_NAO_CANCELAVEL",
                    "So e possivel cancelar um expurgo agendado.");
        }
        galeria.cancelarExpurgo();
        evento(galeria, EventoGaleriaTipo.EXPURGO_CANCELADO, "Expurgo cancelado.");
    }

    /**
     * Executa o expurgo de forma idempotente.
     *
     * <p>Pode ser reexecutado apos queda do processo: objetos ja removidos nao
     * impedem a conclusao, e linhas ja marcadas como {@code EXPURGADO} sao
     * ignoradas.
     */
    @Transactional
    public void executar(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        if (galeria.estaExpurgada()
                || galeria.getExpurgoStatus() == ExpurgoStatus.EXECUTADO) {
            return;
        }
        if (galeria.getExpurgoStatus() != ExpurgoStatus.AGENDADO
                && galeria.getExpurgoStatus() != ExpurgoStatus.EXECUTANDO) {
            return;
        }
        galeria.iniciarExpurgo();

        String prefixo = "estudios/%s/galerias/%s/".formatted(
                galeria.getEstudioId(), galeria.getId());
        List<ArquivoConsulta.ArquivoResumo> removiveis = arquivoConsulta
                .arquivosDaReferencia(galeriaId).stream()
                .filter(arquivo -> arquivo.objectKey() != null)
                .filter(arquivo -> arquivo.objectKey().startsWith(prefixo))
                // Comprovante de extras e prova de pagamento: nunca e expurgado.
                .filter(arquivo -> !arquivo.objectKey().contains("/extras/"))
                .filter(arquivo -> arquivoConsulta.contarReferencias(arquivo.objectKey()) <= 1)
                .toList();

        List<String> chaves = removiveis.stream()
                .map(ArquivoConsulta.ArquivoResumo::objectKey)
                .distinct()
                .toList();
        if (!chaves.isEmpty()) {
            arquivoConsulta.removerObjetos(chaves);
        }
        long bytes = removiveis.stream()
                .mapToLong(ArquivoConsulta.ArquivoResumo::tamanhoBytes)
                .sum();
        arquivoConsulta.marcarExpurgados(removiveis.stream()
                .map(ArquivoConsulta.ArquivoResumo::id)
                .toList());

        for (LinkGaleria link : linkRepository.findByGaleriaIdOrderByExpiraEmDesc(galeriaId)) {
            if (link.estaAtivo()) {
                link.revogar();
            }
        }
        galeria.concluirExpurgo(bytes);
        evento(galeria, EventoGaleriaTipo.EXPURGO_EXECUTADO,
                "%d arquivo(s) removidos, %s liberados."
                        .formatted(removiveis.size(), formatarBytes(bytes)));
        log.info("Expurgo concluido para a galeria {}: {} arquivo(s), {} bytes",
                galeriaId, removiveis.size(), bytes);
    }

    private String confirmacaoEsperada(Galeria galeria) {
        String nome = galeria.getClienteNome();
        return nome == null || nome.isBlank() ? CONFIRMACAO_PADRAO : nome.trim();
    }

    private Galeria obter(String id) {
        return galeriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Galeria", id));
    }

    private void evento(Galeria galeria, EventoGaleriaTipo tipo, String detalhe) {
        eventoRepository.save(EventoGaleria.criar(galeria.getId(), tipo, detalhe));
    }

    private static String formatarBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        BigDecimal valor = BigDecimal.valueOf(bytes);
        String[] unidades = {"KB", "MB", "GB", "TB"};
        for (String unidade : unidades) {
            valor = valor.divide(BigDecimal.valueOf(1024), 1, RoundingMode.HALF_UP);
            if (valor.compareTo(BigDecimal.valueOf(1024)) < 0) {
                return valor.toPlainString().replace('.', ',') + " " + unidade;
            }
        }
        return valor.toPlainString() + " TB";
    }
}
