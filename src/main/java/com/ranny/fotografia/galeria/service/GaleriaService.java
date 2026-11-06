package com.ranny.fotografia.galeria.service;

import com.ranny.fotografia.agenda.AgendamentoConsulta;
import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.arquivo.ArquivoUpload;
import com.ranny.fotografia.cliente.ClienteConsulta;
import com.ranny.fotografia.galeria.api.GaleriaDtos;
import com.ranny.fotografia.galeria.model.DownloadGaleria;
import com.ranny.fotografia.galeria.model.EventoGaleria;
import com.ranny.fotografia.galeria.model.EventoGaleriaTipo;
import com.ranny.fotografia.galeria.model.Galeria;
import com.ranny.fotografia.galeria.model.GaleriaStatus;
import com.ranny.fotografia.galeria.model.LinkGaleria;
import com.ranny.fotografia.galeria.model.LinkGaleriaStatus;
import com.ranny.fotografia.galeria.model.LinkGaleriaTipo;
import com.ranny.fotografia.galeria.model.MidiaDerivado;
import com.ranny.fotografia.galeria.model.MidiaGaleria;
import com.ranny.fotografia.galeria.model.MidiaStatus;
import com.ranny.fotografia.galeria.model.MidiaTipo;
import com.ranny.fotografia.galeria.model.PedidoExtras;
import com.ranny.fotografia.galeria.model.PedidoExtrasStatus;
import com.ranny.fotografia.galeria.model.SelecaoGaleria;
import com.ranny.fotografia.galeria.model.SelecaoItem;
import com.ranny.fotografia.galeria.repository.DownloadGaleriaRepository;
import com.ranny.fotografia.galeria.repository.EventoGaleriaRepository;
import com.ranny.fotografia.galeria.repository.GaleriaRepository;
import com.ranny.fotografia.galeria.repository.GaleriaSpecifications;
import com.ranny.fotografia.galeria.repository.LinkGaleriaRepository;
import com.ranny.fotografia.galeria.repository.MidiaDerivadoRepository;
import com.ranny.fotografia.galeria.repository.MidiaGaleriaRepository;
import com.ranny.fotografia.galeria.repository.PedidoExtrasRepository;
import com.ranny.fotografia.galeria.repository.SelecaoGaleriaRepository;
import com.ranny.fotografia.galeria.repository.SelecaoItemRepository;
import com.ranny.fotografia.shared.api.PaginaResponse;
import com.ranny.fotografia.shared.api.Paginacao;
import com.ranny.fotografia.shared.exception.RecursoExpiradoException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Casos de uso de galerias.
 *
 * <p>PATTERN: Facade + State Machine + Domain Service.
 *
 * <p>Orquestra upload de midias, processamento de derivados, selecao publica,
 * extras, edicao e entrega. O binario continua no modulo {@code arquivo}.
 */
@Service
public class GaleriaService {

    private static final Logger log = LoggerFactory.getLogger(GaleriaService.class);
    private static final String ESTUDIO_PADRAO = "estudio";
    private static final int DIAS_SELECAO = 30;
    private static final int DIAS_ENTREGA = 90;

    private final GaleriaRepository galeriaRepository;
    private final MidiaGaleriaRepository midiaRepository;
    private final MidiaDerivadoRepository derivadoRepository;
    private final LinkGaleriaRepository linkRepository;
    private final SelecaoGaleriaRepository selecaoRepository;
    private final SelecaoItemRepository selecaoItemRepository;
    private final PedidoExtrasRepository extrasRepository;
    private final DownloadGaleriaRepository downloadRepository;
    private final EventoGaleriaRepository eventoRepository;
    private final AgendamentoConsulta agendamentoConsulta;
    private final ClienteConsulta clienteConsulta;
    private final ArquivoUpload arquivoUpload;
    private final ProcessadorMidia processadorMidia;

    public GaleriaService(GaleriaRepository galeriaRepository,
                          MidiaGaleriaRepository midiaRepository,
                          MidiaDerivadoRepository derivadoRepository,
                          LinkGaleriaRepository linkRepository,
                          SelecaoGaleriaRepository selecaoRepository,
                          SelecaoItemRepository selecaoItemRepository,
                          PedidoExtrasRepository extrasRepository,
                          DownloadGaleriaRepository downloadRepository,
                          EventoGaleriaRepository eventoRepository,
                          AgendamentoConsulta agendamentoConsulta,
                          ClienteConsulta clienteConsulta,
                          ArquivoUpload arquivoUpload,
                          ProcessadorMidia processadorMidia) {
        this.galeriaRepository = galeriaRepository;
        this.midiaRepository = midiaRepository;
        this.derivadoRepository = derivadoRepository;
        this.linkRepository = linkRepository;
        this.selecaoRepository = selecaoRepository;
        this.selecaoItemRepository = selecaoItemRepository;
        this.extrasRepository = extrasRepository;
        this.downloadRepository = downloadRepository;
        this.eventoRepository = eventoRepository;
        this.agendamentoConsulta = agendamentoConsulta;
        this.clienteConsulta = clienteConsulta;
        this.arquivoUpload = arquivoUpload;
        this.processadorMidia = processadorMidia;
    }

    @Transactional
    public GaleriaDtos.GaleriaResponse criar(GaleriaDtos.CriarGaleriaRequest request) {
        AgendamentoConsulta.ResumoAgendamento agendamento = agendamentoConsulta
                .buscar(request.agendamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", request.agendamentoId()));

        return galeriaRepository.findByAgendamentoId(request.agendamentoId())
                .map(this::response)
                .orElseGet(() -> {
                    var condicoes = agendamento.condicoes();
                    int fotos = condicoes == null ? 0 : condicoes.quantidadeFotosInclusas();
                    int videos = condicoes == null ? 0 : condicoes.quantidadeVideosInclusos();
                    BigDecimal valorExtra = condicoes == null ? BigDecimal.ZERO : condicoes.valorFotoExtra();
                    String clienteNome = clienteConsulta.buscar(agendamento.clienteId())
                            .map(cliente -> cliente.nome())
                            .orElseGet(() -> agendamento.contato() == null
                                    ? null : agendamento.contato().nome());

                    Galeria galeria = Galeria.criar(
                            ESTUDIO_PADRAO, agendamento.id(), agendamento.contratoId(),
                            agendamento.clienteId(), clienteNome, fotos, videos, valorExtra);
                    galeriaRepository.save(galeria);
                    evento(galeria, EventoGaleriaTipo.CRIADA, "Galeria criada para o ensaio.");
                    return response(galeria);
                });
    }

    @Transactional(readOnly = true)
    public PaginaResponse<GaleriaDtos.GaleriaResponse> listar(String status, String busca,
                                                               Integer pagina, Integer tamanho) {
        Specification<Galeria> spec = (root, query, cb) -> cb.conjunction();
        Specification<Galeria> filtroStatus = GaleriaSpecifications.comStatus(GaleriaStatus.de(status));
        if (filtroStatus != null) {
            spec = spec.and(filtroStatus);
        }
        Specification<Galeria> filtroBusca = GaleriaSpecifications.busca(busca);
        if (filtroBusca != null) {
            spec = spec.and(filtroBusca);
        }
        var pageable = Paginacao.de(pagina, tamanho,
                Sort.by(Sort.Direction.DESC, "auditoria.atualizadoEm"));
        Page<Galeria> galerias = galeriaRepository.findAll(spec, pageable);
        return PaginaResponse.de(galerias, this::response);
    }

    @Transactional(readOnly = true)
    public GaleriaDtos.GaleriaDetalheResponse detalhar(String id) {
        Galeria galeria = obter(id);
        List<GaleriaDtos.LinkResponse> links = linkRepository
                .findByGaleriaIdOrderByExpiraEmDesc(id).stream()
                .map(GaleriaDtos.LinkResponse::de)
                .toList();
        SelecaoGaleria selecao = selecaoRepository
                .findFirstByGaleriaIdOrderByEnviadaEmDesc(id).orElse(null);
        PedidoExtras extras = extrasRepository
                .findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(id).orElse(null);
        List<GaleriaDtos.EventoResponse> historico = eventoRepository
                .findByGaleriaIdOrderByCriadoEmAsc(id).stream()
                .map(GaleriaDtos.EventoResponse::de)
                .toList();
        return new GaleriaDtos.GaleriaDetalheResponse(
                response(galeria),
                links,
                selecao == null ? null : selecaoResponse(selecao),
                extras == null ? null : GaleriaDtos.PedidoExtrasResponse.de(extras),
                historico);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<GaleriaDtos.MidiaResponse> listarMidias(String galeriaId,
                                                                   Integer pagina, Integer tamanho) {
        Galeria galeria = obter(galeriaId);
        var pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.ASC, "ordem"));
        Page<MidiaGaleria> midias = midiaRepository
                .findByGaleriaIdAndStatusNotOrderByOrdemAsc(galeriaId, MidiaStatus.REMOVIDA, pageable);
        return PaginaResponse.de(midias, midia -> midiaResponse(midia, galeria.getId(), true));
    }

    @Transactional
    public GaleriaDtos.UploadSessaoResponse iniciarUpload(
            String galeriaId, GaleriaDtos.IniciarMidiaUploadRequest request) {
        Galeria galeria = obter(galeriaId);
        if (!galeria.aceitaNovasMidias()) {
            throw new RegraDeNegocioException("GALERIA_NAO_ACEITA_MIDIA",
                    "A galeria nao aceita novas midias no status atual.");
        }
        MidiaTipo tipo = MidiaTipo.de(request.tipo());
        if (tipo == null) {
            throw new RegraDeNegocioException("TIPO_MIDIA_INVALIDO",
                    "Informe o tipo da midia: foto ou video.");
        }
        ArquivoFinalidade finalidade = tipo == MidiaTipo.FOTO
                ? ArquivoFinalidade.FOTO_ORIGINAL
                : ArquivoFinalidade.VIDEO_ORIGINAL;
        ArquivoUpload.IniciarUpload comando = new ArquivoUpload.IniciarUpload(
                finalidade,
                galeriaId,
                "estudios/%s/galerias/%s/midias".formatted(galeria.getEstudioId(), galeriaId),
                request.nomeArquivo(),
                request.mimeType(),
                request.tamanho(),
                request.checksumSha256(),
                arquivoUpload.tamanhoMaximo(finalidade));
        return GaleriaDtos.UploadSessaoResponse.de(arquivoUpload.iniciar(comando));
    }

    @Transactional
    public GaleriaDtos.MidiaResponse concluirUpload(
            String galeriaId, String arquivoId,
            GaleriaDtos.ConcluirMidiaUploadRequest request) {
        Galeria galeria = obter(galeriaId);
        MidiaTipo tipo = MidiaTipo.de(request.tipo());
        if (tipo == null) {
            throw new RegraDeNegocioException("TIPO_MIDIA_INVALIDO",
                    "Informe o tipo da midia: foto ou video.");
        }
        ArquivoFinalidade finalidade = tipo == MidiaTipo.FOTO
                ? ArquivoFinalidade.FOTO_ORIGINAL
                : ArquivoFinalidade.VIDEO_ORIGINAL;
        List<ArquivoUpload.ParteEnviada> partes = request.partes().stream()
                .map(parte -> new ArquivoUpload.ParteEnviada(parte.partNumber(), parte.etag()))
                .toList();
        ArquivoUpload.ArquivoResumo arquivo = arquivoUpload.concluir(
                arquivoId, finalidade, galeriaId, partes);

        long existentes = midiaRepository.countByGaleriaIdAndTipoAndStatusNot(
                galeriaId, tipo, MidiaStatus.REMOVIDA);
        MidiaGaleria midia = MidiaGaleria.criar(
                galeriaId, tipo, arquivo.id(), arquivo.nomeOriginal(), arquivo.mimeType(),
                arquivo.tamanhoBytes(), (int) existentes + 1);
        midiaRepository.save(midia);
        evento(galeria, EventoGaleriaTipo.MIDIA_ENVIADA, arquivo.nomeOriginal() + " enviada.");

        if (tipo == MidiaTipo.FOTO) {
            processarMidia(midia, galeria);
        }
        midiaRepository.saveAndFlush(midia);
        return midiaResponse(midia, galeriaId, true);
    }

    @Transactional
    public void abortarUpload(String galeriaId, String arquivoId, String tipo) {
        obter(galeriaId);
        MidiaTipo tipoMidia = MidiaTipo.de(tipo);
        ArquivoFinalidade finalidade = tipoMidia == MidiaTipo.VIDEO
                ? ArquivoFinalidade.VIDEO_ORIGINAL
                : ArquivoFinalidade.FOTO_ORIGINAL;
        arquivoUpload.abortar(arquivoId, finalidade, galeriaId);
    }

    @Transactional
    public void removerMidia(String galeriaId, String midiaId) {
        Galeria galeria = obter(galeriaId);
        MidiaGaleria midia = midiaRepository.findByIdAndGaleriaId(midiaId, galeriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Midia", midiaId));
        midia.remover();
        evento(galeria, EventoGaleriaTipo.MIDIA_REMOVIDA, midia.getNomeOriginal() + " removida.");
    }

    @Transactional
    public GaleriaDtos.LinkResponse publicarSelecao(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        validarGateSelecao(galeria);
        LinkGaleria link = linkRepository
                .findFirstByGaleriaIdAndTipoAndStatusOrderByExpiraEmDesc(
                        galeriaId, LinkGaleriaTipo.SELECAO, LinkGaleriaStatus.ATIVO)
                .map(existente -> {
                    existente.estender(DIAS_SELECAO);
                    return existente;
                })
                .orElseGet(() -> linkRepository.save(
                        LinkGaleria.criar(galeriaId, LinkGaleriaTipo.SELECAO, DIAS_SELECAO)));
        galeria.publicarSelecao();
        evento(galeria, EventoGaleriaTipo.SELECAO_PUBLICADA,
                "Link de selecao publicado por %d dias.".formatted(DIAS_SELECAO));
        return GaleriaDtos.LinkResponse.de(link);
    }

    @Transactional
    public GaleriaDtos.LinkResponse reabrirSelecao(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        PedidoExtras extras = extrasRepository
                .findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(galeriaId).orElse(null);
        if (extras != null && extras.getStatus() == PedidoExtrasStatus.PAGO) {
            throw new RegraDeNegocioException("EXTRAS_JA_PAGOS",
                    "Nao e possivel reabrir a selecao com extras pagos.");
        }
        if (extras != null && extras.getStatus() == PedidoExtrasStatus.PENDENTE) {
            extras.cancelar();
        }
        selecaoRepository.findFirstByGaleriaIdOrderByEnviadaEmDesc(galeriaId)
                .ifPresent(SelecaoGaleria::reabrir);
        LinkGaleria link = linkRepository
                .findFirstByGaleriaIdAndTipoOrderByExpiraEmDesc(galeriaId, LinkGaleriaTipo.SELECAO)
                .map(existente -> {
                    existente.estender(DIAS_SELECAO);
                    return existente;
                })
                .orElseGet(() -> linkRepository.save(
                        LinkGaleria.criar(galeriaId, LinkGaleriaTipo.SELECAO, DIAS_SELECAO)));
        galeria.reabrirSelecao();
        evento(galeria, EventoGaleriaTipo.SELECAO_REABERTA, "Selecao reaberta pelo estudio.");
        return GaleriaDtos.LinkResponse.de(link);
    }

    @Transactional
    public synchronized GaleriaDtos.SelecaoResponse submeterSelecao(
            String token, List<String> fotoIds, List<String> videoIds,
            String ip, String userAgent) {
        LinkGaleria link = linkAtivo(token, LinkGaleriaTipo.SELECAO);
        Galeria galeria = obter(link.getGaleriaId());
        if (galeria.getStatus() != GaleriaStatus.EM_SELECAO) {
            throw new RegraDeNegocioException("SELECAO_INDISPONIVEL",
                    "Esta galeria nao esta em selecao.");
        }

        List<String> fotos = idsUnicos(fotoIds);
        List<String> videos = idsUnicos(videoIds);
        List<MidiaGaleria> selecionadas = new ArrayList<>();
        selecionadas.addAll(midiaRepository.findByGaleriaIdAndIdInAndStatus(
                galeria.getId(), fotos, MidiaStatus.DISPONIVEL));
        selecionadas.addAll(midiaRepository.findByGaleriaIdAndIdInAndStatus(
                galeria.getId(), videos, MidiaStatus.DISPONIVEL));

        long fotosEncontradas = selecionadas.stream()
                .filter(midia -> midia.getTipo() == MidiaTipo.FOTO).count();
        long videosEncontrados = selecionadas.stream()
                .filter(midia -> midia.getTipo() == MidiaTipo.VIDEO).count();
        if (fotosEncontradas != fotos.size() || videosEncontrados != videos.size()) {
            throw new RegraDeNegocioException("MIDIA_INVALIDA",
                    "Uma ou mais midias selecionadas nao estao disponiveis nesta galeria.");
        }
        if (fotos.size() < galeria.getFotosInclusas()) {
            throw new RegraDeNegocioException("FOTOS_INSUFICIENTES",
                    "Selecione todas as %d fotos inclusas no pacote.".formatted(galeria.getFotosInclusas()));
        }
        if (videos.size() != galeria.getVideosInclusos()) {
            throw new RegraDeNegocioException("VIDEOS_INVALIDOS",
                    "Selecione exatamente %d video(s) incluso(s).".formatted(galeria.getVideosInclusos()));
        }

        int extras = Math.max(0, fotos.size() - galeria.getFotosInclusas());
        BigDecimal valorExtras = galeria.getValorFotoExtra().multiply(BigDecimal.valueOf(extras));
        String protocolo = "SEL-%d-%04d".formatted(LocalDate.now().getYear(),
                selecaoRepository.count() + 1);
        SelecaoGaleria selecao = selecaoRepository.save(SelecaoGaleria.criar(
                galeria.getId(), protocolo, fotos.size(), videos.size(), extras,
                valorExtras, ip, userAgent));
        for (MidiaGaleria midia : selecionadas) {
            selecaoItemRepository.save(SelecaoItem.criar(
                    selecao.getId(), midia.getId(), midia.getTipo()));
        }

        PedidoExtras pedido = null;
        if (extras > 0) {
            pedido = extrasRepository.save(PedidoExtras.criar(
                    galeria.getId(), selecao.getId(), extras,
                    galeria.getValorFotoExtra(), valorExtras));
        }
        galeria.registrarSelecao(selecao.getId(), pedido == null ? null : pedido.getId());
        evento(galeria, EventoGaleriaTipo.SELECAO_ENVIADA,
                pedido == null ? "Selecao enviada pelo cliente."
                        : "Selecao enviada com %d foto(s) extra(s).".formatted(extras));
        return selecaoResponse(selecao);
    }

    @Transactional
    public GaleriaDtos.PedidoExtrasResponse confirmarExtras(
            String galeriaId, GaleriaDtos.ExtrasConfirmacaoRequest request) {
        Galeria galeria = obter(galeriaId);
        PedidoExtras pedido = extrasRepository
                .findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(galeriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido de extras", galeriaId));
        if (pedido.getStatus() != PedidoExtrasStatus.PENDENTE) {
            throw new RegraDeNegocioException("EXTRAS_NAO_PENDENTES",
                    "Este pedido de extras nao esta pendente.");
        }
        pedido.pagar(request.formaPagamento(),
                request.confirmadoPor() == null ? "Equipe do estudio" : request.confirmadoPor());
        galeria.iniciarEdicao();
        evento(galeria, EventoGaleriaTipo.EXTRAS_CONFIRMADOS, "Extras confirmados.");
        return GaleriaDtos.PedidoExtrasResponse.de(pedido);
    }

    @Transactional
    public GaleriaDtos.PedidoExtrasResponse isentarExtras(String galeriaId, String por) {
        Galeria galeria = obter(galeriaId);
        PedidoExtras pedido = extrasRepository
                .findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(galeriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido de extras", galeriaId));
        pedido.isentar(por == null ? "Equipe do estudio" : por);
        galeria.iniciarEdicao();
        evento(galeria, EventoGaleriaTipo.EXTRAS_ISENTOS, "Extras isentos.");
        return GaleriaDtos.PedidoExtrasResponse.de(pedido);
    }

    @Transactional
    public GaleriaDtos.PedidoExtrasResponse liberarEdicaoManual(
            String galeriaId, GaleriaDtos.LiberacaoEdicaoRequest request) {
        Galeria galeria = obter(galeriaId);
        PedidoExtras pedido = extrasRepository
                .findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(galeriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido de extras", galeriaId));
        pedido.liberarManual(request.motivo(), request.por());
        galeria.iniciarEdicao();
        evento(galeria, EventoGaleriaTipo.EDICAO_INICIADA,
                "Edicao liberada manualmente: " + request.motivo());
        return GaleriaDtos.PedidoExtrasResponse.de(pedido);
    }

    @Transactional
    public void iniciarEdicao(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        PedidoExtras pedido = extrasRepository
                .findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(galeriaId).orElse(null);
        if (pedido != null && !pedido.estaQuitado()) {
            throw new RegraDeNegocioException("EXTRAS_PENDENTES",
                    "Confirme ou isente os extras antes de iniciar a edicao.");
        }
        galeria.iniciarEdicao();
        evento(galeria, EventoGaleriaTipo.EDICAO_INICIADA, "Edicao iniciada.");
    }

    @Transactional
    public void marcarProntaEntrega(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        if (selecaoRepository.findFirstByGaleriaIdOrderByEnviadaEmDesc(galeriaId).isEmpty()) {
            throw new RegraDeNegocioException("SEM_SELECAO",
                    "A galeria ainda nao possui selecao enviada.");
        }
        galeria.marcarProntaEntrega();
        evento(galeria, EventoGaleriaTipo.PRONTA_ENTREGA, "Galeria pronta para entrega.");
    }

    @Transactional
    public GaleriaDtos.LinkResponse publicarEntrega(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        if (galeria.getStatus() != GaleriaStatus.PRONTA_ENTREGA
                && galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            throw new RegraDeNegocioException("GALERIA_NAO_PRONTA",
                    "Marque a galeria como pronta para entrega antes de publicar.");
        }
        PedidoExtras pedido = extrasRepository
                .findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(galeriaId).orElse(null);
        if (pedido != null && !pedido.estaQuitado()) {
            throw new RegraDeNegocioException("EXTRAS_PENDENTES",
                    "Confirme ou isente os extras antes de publicar a entrega.");
        }
        LinkGaleria link = linkRepository
                .findFirstByGaleriaIdAndTipoAndStatusOrderByExpiraEmDesc(
                        galeriaId, LinkGaleriaTipo.ENTREGA, LinkGaleriaStatus.ATIVO)
                .map(existente -> {
                    existente.estender(DIAS_ENTREGA);
                    return existente;
                })
                .orElseGet(() -> linkRepository.save(
                        LinkGaleria.criar(galeriaId, LinkGaleriaTipo.ENTREGA, DIAS_ENTREGA)));
        galeria.publicarEntrega();
        evento(galeria, EventoGaleriaTipo.ENTREGA_PUBLICADA,
                "Link de entrega publicado por %d dias.".formatted(DIAS_ENTREGA));
        return GaleriaDtos.LinkResponse.de(link);
    }

    @Transactional
    public void revogarEntrega(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        linkRepository.findFirstByGaleriaIdAndTipoAndStatusOrderByExpiraEmDesc(
                        galeriaId, LinkGaleriaTipo.ENTREGA, LinkGaleriaStatus.ATIVO)
                .ifPresent(LinkGaleria::revogar);
        galeria.revogarEntrega();
        evento(galeria, EventoGaleriaTipo.ENTREGA_REVOGADA, "Entrega revogada.");
    }

    @Transactional
    public GaleriaDtos.GaleriaPublicaResponse consultarPublico(String token) {
        LinkGaleria link = linkRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link de galeria", token));
        if (!link.estaAtivo()) {
            throw new RecursoExpiradoException("LINK_EXPIRADO",
                    "Este link de galeria expirou ou foi revogado.");
        }
        link.visualizar();
        Galeria galeria = obter(link.getGaleriaId());
        List<MidiaGaleria> midias = link.getTipo() == LinkGaleriaTipo.ENTREGA
                ? midiasDaSelecao(galeria.getId())
                : midiaRepository.findByGaleriaIdAndStatusNotOrderByOrdemAsc(
                        galeria.getId(), MidiaStatus.REMOVIDA);

        SelecaoGaleria selecao = selecaoRepository
                .findFirstByGaleriaIdOrderByEnviadaEmDesc(galeria.getId()).orElse(null);
        PedidoExtras extras = extrasRepository
                .findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(galeria.getId()).orElse(null);
        return new GaleriaDtos.GaleriaPublicaResponse(
                GaleriaDtos.LinkResponse.de(link),
                response(galeria),
                midias.stream().map(midia -> midiaResponse(midia, galeria.getId(),
                        link.getTipo() == LinkGaleriaTipo.ENTREGA)).toList(),
                selecao == null ? null : selecaoResponse(selecao),
                extras == null ? null : GaleriaDtos.PedidoExtrasResponse.de(extras),
                link.getTipo() == LinkGaleriaTipo.ENTREGA
                        && galeria.getStatus() == GaleriaStatus.ENTREGUE);
    }

    @Transactional
    public GaleriaDtos.DownloadResponse registrarDownload(
            String token, String midiaId, String ip, String userAgent) {
        LinkGaleria link = linkAtivo(token, LinkGaleriaTipo.ENTREGA);
        Galeria galeria = obter(link.getGaleriaId());
        if (galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            throw new RegraDeNegocioException("ENTREGA_INDISPONIVEL",
                    "A entrega ainda nao foi publicada.");
        }
        MidiaGaleria midia = midiaRepository.findByIdAndGaleriaId(midiaId, galeria.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Midia", midiaId));
        if (!midia.estaDisponivel() || !midiaSelecionada(galeria.getId(), midiaId)) {
            throw new RecursoNaoEncontradoException("Midia", midiaId);
        }

        String arquivoId = midia.getArquivoEditadoId() == null
                ? midia.getArquivoOriginalId()
                : midia.getArquivoEditadoId();
        ArquivoFinalidade finalidade = midia.getArquivoEditadoId() == null
                ? (midia.getTipo() == MidiaTipo.FOTO
                ? ArquivoFinalidade.FOTO_ORIGINAL : ArquivoFinalidade.VIDEO_ORIGINAL)
                : ArquivoFinalidade.FOTO_DERIVADO;
        ArquivoUpload.UrlDownload download = arquivoUpload.urlDownload(
                arquivoId, finalidade, galeria.getId());
        downloadRepository.save(DownloadGaleria.criar(
                galeria.getId(), link.getId(), midia.getId(), arquivoId, ip, userAgent));
        evento(galeria, EventoGaleriaTipo.DOWNLOAD_REGISTRADO,
                midia.getNomeOriginal() + " baixada.");
        return new GaleriaDtos.DownloadResponse(
                download.url(), download.expiraEm(), midia.getNomeOriginal());
    }

    private void processarMidia(MidiaGaleria midia, Galeria galeria) {
        try {
            byte[] original = arquivoUpload.abrirConteudo(
                    midia.getArquivoOriginalId(), ArquivoFinalidade.FOTO_ORIGINAL, galeria.getId());
            ProcessadorMidia.Resultado resultado = processadorMidia.processar(
                    original, midia.getNomeOriginal());
            for (ProcessadorMidia.Derivado derivado : resultado.derivados()) {
                ArquivoUpload.ArquivoResumo arquivo = arquivoUpload.salvarDisponivel(
                        new ArquivoUpload.IniciarArquivo(
                                ArquivoFinalidade.FOTO_DERIVADO,
                                galeria.getId(),
                                "estudios/%s/galerias/%s/fotos/%s/derivados"
                                        .formatted(galeria.getEstudioId(), galeria.getId(), midia.getId()),
                                derivado.nomeArquivo(),
                                derivado.mimeType()),
                        derivado.conteudo());
                derivadoRepository.save(MidiaDerivado.criar(
                        midia.getId(), derivado.tipo(), arquivo.id(),
                        derivado.largura(), derivado.altura(), "jpg"));
            }
            midia.disponivel(resultado.largura(), resultado.altura(), null);
        } catch (RuntimeException ex) {
            log.warn("Falha ao processar midia {}: {}", midia.getId(), ex.getMessage());
            midia.falhar(ex.getMessage());
        }
    }

    private void validarGateSelecao(Galeria galeria) {
        AgendamentoConsulta.ResumoAgendamento agendamento = agendamentoConsulta
                .buscar(galeria.getAgendamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Agendamento", galeria.getAgendamentoId()));
        if (!agendamento.realizado()) {
            throw new RegraDeNegocioException("ENSAIO_NAO_REALIZADO",
                    "Marque o ensaio como realizado antes de publicar a galeria.");
        }
        if (!liquidado(agendamento.entradaStatus())) {
            throw new RegraDeNegocioException("ENTRADA_PENDENTE",
                    "A entrada do ensaio ainda nao foi confirmada.");
        }
        if (!liquidado(agendamento.finalStatus())) {
            throw new RegraDeNegocioException("SALDO_PENDENTE",
                    "Registre o recebimento do saldo final antes de publicar a galeria.");
        }
        long fotos = midiaRepository.countByGaleriaIdAndTipoAndStatus(
                galeria.getId(), MidiaTipo.FOTO, MidiaStatus.DISPONIVEL);
        long videos = midiaRepository.countByGaleriaIdAndTipoAndStatus(
                galeria.getId(), MidiaTipo.VIDEO, MidiaStatus.DISPONIVEL);
        if (fotos < galeria.getFotosInclusas()) {
            throw new RegraDeNegocioException("FOTOS_INSUFICIENTES",
                    "Envie pelo menos %d fotos (%,d disponiveis)."
                            .formatted(galeria.getFotosInclusas(), fotos));
        }
        if (videos < galeria.getVideosInclusos()) {
            throw new RegraDeNegocioException("VIDEOS_INSUFICIENTES",
                    "Envie pelo menos %d videos (%,d disponiveis)."
                            .formatted(galeria.getVideosInclusos(), videos));
        }
    }

    private static boolean liquidado(PagamentoStatus status) {
        return status == PagamentoStatus.CONFIRMADO || status == PagamentoStatus.ISENTO;
    }

    private LinkGaleria linkAtivo(String token, LinkGaleriaTipo tipo) {
        LinkGaleria link = linkRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link de galeria", token));
        if (link.getTipo() != tipo || !link.estaAtivo()) {
            throw new RecursoExpiradoException("LINK_EXPIRADO",
                    "Este link de galeria expirou ou nao esta disponivel.");
        }
        return link;
    }

    private List<MidiaGaleria> midiasDaSelecao(String galeriaId) {
        SelecaoGaleria selecao = selecaoRepository
                .findFirstByGaleriaIdOrderByEnviadaEmDesc(galeriaId).orElse(null);
        if (selecao == null) {
            return List.of();
        }
        List<String> ids = selecaoItemRepository.findBySelecaoId(selecao.getId()).stream()
                .map(SelecaoItem::getMidiaId)
                .toList();
        return midiaRepository.findByGaleriaIdAndIdInAndStatus(
                galeriaId, ids, MidiaStatus.DISPONIVEL);
    }

    private boolean midiaSelecionada(String galeriaId, String midiaId) {
        return selecaoRepository.findFirstByGaleriaIdOrderByEnviadaEmDesc(galeriaId)
                .map(selecao -> selecaoItemRepository.findBySelecaoId(selecao.getId()).stream()
                        .anyMatch(item -> item.getMidiaId().equals(midiaId)))
                .orElse(false);
    }

    private GaleriaDtos.GaleriaResponse response(Galeria galeria) {
        long totalFotos = midiaRepository.countByGaleriaIdAndTipoAndStatusNot(
                galeria.getId(), MidiaTipo.FOTO, MidiaStatus.REMOVIDA);
        long totalVideos = midiaRepository.countByGaleriaIdAndTipoAndStatusNot(
                galeria.getId(), MidiaTipo.VIDEO, MidiaStatus.REMOVIDA);
        long fotosDisponiveis = midiaRepository.countByGaleriaIdAndTipoAndStatus(
                galeria.getId(), MidiaTipo.FOTO, MidiaStatus.DISPONIVEL);
        long videosDisponiveis = midiaRepository.countByGaleriaIdAndTipoAndStatus(
                galeria.getId(), MidiaTipo.VIDEO, MidiaStatus.DISPONIVEL);
        return GaleriaDtos.GaleriaResponse.de(galeria, totalFotos, totalVideos,
                fotosDisponiveis, videosDisponiveis, null);
    }

    private GaleriaDtos.MidiaResponse midiaResponse(MidiaGaleria midia, String galeriaId,
                                                     boolean exporOriginal) {
        String thumbUrl = null;
        String webUrl = null;
        String editadaUrl = null;
        if (midia.getTipo() == MidiaTipo.FOTO) {
            for (MidiaDerivado derivado : derivadoRepository.findByMidiaId(midia.getId())) {
                String url = urlOuNull(derivado.getArquivoId(), ArquivoFinalidade.FOTO_DERIVADO, galeriaId);
                if ("THUMB".equals(derivado.getTipo())) {
                    thumbUrl = url;
                } else if ("WEB".equals(derivado.getTipo())) {
                    webUrl = url;
                }
            }
        }
        String originalUrl = null;
        if (exporOriginal || midia.getTipo() == MidiaTipo.VIDEO) {
            originalUrl = urlOuNull(midia.getArquivoOriginalId(),
                    midia.getTipo() == MidiaTipo.FOTO
                            ? ArquivoFinalidade.FOTO_ORIGINAL : ArquivoFinalidade.VIDEO_ORIGINAL,
                    galeriaId);
        }
        if (midia.getArquivoEditadoId() != null) {
            editadaUrl = urlOuNull(midia.getArquivoEditadoId(),
                    ArquivoFinalidade.FOTO_DERIVADO, galeriaId);
        }
        return GaleriaDtos.MidiaResponse.de(midia, thumbUrl, webUrl, originalUrl, editadaUrl);
    }

    private String urlOuNull(String arquivoId, ArquivoFinalidade finalidade, String galeriaId) {
        try {
            return arquivoUpload.urlDownload(arquivoId, finalidade, galeriaId).url();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private GaleriaDtos.SelecaoResponse selecaoResponse(SelecaoGaleria selecao) {
        List<SelecaoItem> itens = selecaoItemRepository.findBySelecaoId(selecao.getId());
        List<String> fotoIds = itens.stream()
                .filter(item -> "FOTO".equals(item.getTipo()))
                .map(SelecaoItem::getMidiaId)
                .toList();
        List<String> videoIds = itens.stream()
                .filter(item -> "VIDEO".equals(item.getTipo()))
                .map(SelecaoItem::getMidiaId)
                .toList();
        return GaleriaDtos.SelecaoResponse.de(selecao, fotoIds, videoIds);
    }

    private Galeria obter(String id) {
        return galeriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Galeria", id));
    }

    private void evento(Galeria galeria, EventoGaleriaTipo tipo, String detalhe) {
        eventoRepository.save(EventoGaleria.criar(galeria.getId(), tipo, detalhe));
    }

    private static List<String> idsUnicos(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<String> unicos = new LinkedHashSet<>(ids);
        return new ArrayList<>(unicos);
    }
}
