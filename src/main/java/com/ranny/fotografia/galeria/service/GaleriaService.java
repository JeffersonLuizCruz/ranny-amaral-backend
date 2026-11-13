package com.ranny.fotografia.galeria.service;

import com.ranny.fotografia.agenda.AgendamentoConsulta;
import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.arquivo.ArquivoUpload;
import com.ranny.fotografia.cliente.ClienteConsulta;
import com.ranny.fotografia.contrato.EstudioConsulta;
import com.ranny.fotografia.galeria.GaleriaConsulta;
import com.ranny.fotografia.galeria.event.MidiaProcessamentoEvent;
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
import com.ranny.fotografia.galeria.model.MidiaDerivadoOrigem;
import com.ranny.fotografia.galeria.model.MidiaFinalidade;
import com.ranny.fotografia.galeria.model.MidiaGaleria;
import com.ranny.fotografia.galeria.model.MidiaStatus;
import com.ranny.fotografia.galeria.model.MidiaTipo;
import com.ranny.fotografia.galeria.model.PedidoExtras;
import com.ranny.fotografia.galeria.model.PedidoExtrasStatus;
import com.ranny.fotografia.galeria.model.SelecaoGaleria;
import com.ranny.fotografia.galeria.model.SelecaoItem;
import com.ranny.fotografia.galeria.model.SelecaoStatus;
import com.ranny.fotografia.galeria.model.TokenDownloadSelecao;
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
import com.ranny.fotografia.galeria.repository.TokenDownloadSelecaoRepository;
import com.ranny.fotografia.pacote.PacoteConsulta;
import com.ranny.fotografia.pacote.PacoteResumo;
import com.ranny.fotografia.shared.api.PaginaResponse;
import com.ranny.fotografia.shared.api.Paginacao;
import com.ranny.fotografia.shared.exception.RecursoExpiradoException;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import com.ranny.fotografia.shared.events.LoteLiberadoEvent;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.financeiro.CalculadoraExtras;
import com.ranny.fotografia.shared.financeiro.ResultadoDeltaExtras;
import com.ranny.fotografia.shared.model.FaixaFotoExtraSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Casos de uso de galerias.
 *
 * <p>PATTERN: Facade + State Machine + Domain Service.
 *
 * <p>Orquestra upload de midias, processamento de derivados, selecao publica,
 * extras, edicao e entrega. O binario continua no modulo {@code arquivo}.
 */
@Service
public class GaleriaService implements GaleriaConsulta {

    private static final Logger log = LoggerFactory.getLogger(GaleriaService.class);
    private static final String ESTUDIO_PADRAO = "estudio";
    private static final int DIAS_SELECAO = 30;
    private static final int DIAS_ENTREGA = 90;
    private static final Duration VALIDADE_DOWNLOAD_SELECAO = Duration.ofMinutes(10);

    private final GaleriaRepository galeriaRepository;
    private final MidiaGaleriaRepository midiaRepository;
    private final MidiaDerivadoRepository derivadoRepository;
    private final LinkGaleriaRepository linkRepository;
    private final SelecaoGaleriaRepository selecaoRepository;
    private final SelecaoItemRepository selecaoItemRepository;
    private final PedidoExtrasRepository extrasRepository;
    private final DownloadGaleriaRepository downloadRepository;
    private final EventoGaleriaRepository eventoRepository;
    private final TokenDownloadSelecaoRepository tokenDownloadRepository;
    private final AgendamentoConsulta agendamentoConsulta;
    private final ClienteConsulta clienteConsulta;
    private final EstudioConsulta estudioConsulta;
    private final PacoteConsulta pacoteConsulta;
    private final ArquivoUpload arquivoUpload;
    private final ProcessadorMidia processadorMidia;
    private final ApplicationEventPublisher eventos;
    private final boolean processamentoAsync;
    private final long zipMaxBytes;

    public GaleriaService(GaleriaRepository galeriaRepository,
                          MidiaGaleriaRepository midiaRepository,
                          MidiaDerivadoRepository derivadoRepository,
                          LinkGaleriaRepository linkRepository,
                          SelecaoGaleriaRepository selecaoRepository,
                          SelecaoItemRepository selecaoItemRepository,
                          PedidoExtrasRepository extrasRepository,
                          DownloadGaleriaRepository downloadRepository,
                          EventoGaleriaRepository eventoRepository,
                          TokenDownloadSelecaoRepository tokenDownloadRepository,
                          AgendamentoConsulta agendamentoConsulta,
                          ClienteConsulta clienteConsulta,
                          EstudioConsulta estudioConsulta,
                          PacoteConsulta pacoteConsulta,
                          ArquivoUpload arquivoUpload,
                          ProcessadorMidia processadorMidia,
                          ApplicationEventPublisher eventos,
                          @Value("${app.galeria.processamento-async:true}")
                          boolean processamentoAsync,
                           @Value("${app.galeria.zip-max-bytes:2147483648}")
                           long zipMaxBytes) {
        this.galeriaRepository = galeriaRepository;
        this.midiaRepository = midiaRepository;
        this.derivadoRepository = derivadoRepository;
        this.linkRepository = linkRepository;
        this.selecaoRepository = selecaoRepository;
        this.selecaoItemRepository = selecaoItemRepository;
        this.extrasRepository = extrasRepository;
        this.downloadRepository = downloadRepository;
        this.eventoRepository = eventoRepository;
        this.tokenDownloadRepository = tokenDownloadRepository;
        this.agendamentoConsulta = agendamentoConsulta;
        this.clienteConsulta = clienteConsulta;
        this.estudioConsulta = estudioConsulta;
        this.pacoteConsulta = pacoteConsulta;
        this.arquivoUpload = arquivoUpload;
        this.processadorMidia = processadorMidia;
        this.eventos = eventos;
        this.processamentoAsync = processamentoAsync;
        this.zipMaxBytes = zipMaxBytes;
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
                    List<FaixaFotoExtraSnapshot> faixasExtras =
                            condicoes == null ? List.of() : condicoes.faixasFotoExtra();
                    String clienteNome = clienteConsulta.buscar(agendamento.clienteId())
                            .map(cliente -> cliente.nome())
                            .orElseGet(() -> agendamento.contato() == null
                                    ? null : agendamento.contato().nome());

                    Galeria galeria = Galeria.criar(
                            ESTUDIO_PADRAO, agendamento.id(), agendamento.contratoId(),
                            agendamento.clienteId(), clienteNome, fotos, videos, valorExtra,
                            faixasExtras);
                    galeriaRepository.save(galeria);
                    evento(galeria, EventoGaleriaTipo.CRIADA, "Galeria criada para o ensaio.");
                    return response(galeria);
                });
    }

    @Transactional(readOnly = true)
    public PaginaResponse<GaleriaDtos.GaleriaResponse> listar(String status, String busca,
                                                               Boolean baixada,
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
        Specification<Galeria> filtroZip = GaleriaSpecifications.comZipBaixado(baixada);
        if (filtroZip != null) {
            spec = spec.and(filtroZip);
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
        List<PedidoExtras> pedidosExtras = extrasRepository
                .findByGaleriaIdOrderByAuditoriaCriadoEmAsc(id);
        PedidoExtras extras = pedidosExtras.isEmpty()
                ? null : pedidosExtras.get(pedidosExtras.size() - 1);
        List<GaleriaDtos.EventoResponse> historico = eventoRepository
                .findByGaleriaIdOrderByCriadoEmAsc(id).stream()
                .map(GaleriaDtos.EventoResponse::de)
                .toList();
        List<GaleriaDtos.PedidoExtrasResponse> pedidos = pedidosExtras.stream()
                .map(pedido -> extrasResponse(pedido, true))
                .toList();
        GaleriaDtos.GaleriaResponse galeriaResponse = response(galeria);
        return new GaleriaDtos.GaleriaDetalheResponse(
                galeriaResponse,
                links,
                selecao == null ? null : selecaoResponse(selecao),
                extras == null ? null : extrasResponse(extras, true),
                pedidos,
                resumoExtras(galeria, pedidosExtras),
                historico,
                galeriaResponse.pacoteNome());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<GaleriaDtos.MidiaResponse> listarMidias(String galeriaId,
                                                                   Integer pagina, Integer tamanho) {
        Galeria galeria = obter(galeriaId);
        var pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.ASC, "ordem"));
        Page<MidiaGaleria> midias = midiaRepository
                .findByGaleriaIdAndStatusNotOrderByOrdemAsc(galeriaId, MidiaStatus.REMOVIDA, pageable);
        return PaginaResponse.de(midias, midia -> midiaResponse(midia, galeria.getId(), true, false));
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
        finalidadeUpload(request.finalidade(), tipo);
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
        MidiaFinalidade finalidadeMidia = finalidadeUpload(request.finalidade(), tipo);
        ArquivoFinalidade finalidade = tipo == MidiaTipo.FOTO
                ? ArquivoFinalidade.FOTO_ORIGINAL
                : ArquivoFinalidade.VIDEO_ORIGINAL;
        List<ArquivoUpload.ParteEnviada> partes = request.partes().stream()
                .map(parte -> new ArquivoUpload.ParteEnviada(parte.partNumber(), parte.etag()))
                .toList();
        ArquivoUpload.ArquivoResumo arquivo = arquivoUpload.concluir(
                arquivoId, finalidade, galeriaId, partes);

        // O lock na galeria serializa conclusoes concorrentes e evita que duas
        // midias recebam o mesmo numero; removidas continuam reservando o seu.
        galeriaRepository.findByIdComLock(galeriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Galeria", galeriaId));
        int proximaOrdem = midiaRepository.maiorOrdem(galeriaId, tipo) + 1;
        MidiaGaleria midia = MidiaGaleria.criar(
                galeriaId, tipo, arquivo.id(), arquivo.nomeOriginal(), arquivo.mimeType(),
                arquivo.tamanhoBytes(), proximaOrdem, finalidadeMidia);
        midiaRepository.save(midia);
        evento(galeria, EventoGaleriaTipo.MIDIA_ENVIADA, arquivo.nomeOriginal() + " enviada.");

        if (tipo == MidiaTipo.FOTO) {
            if (processamentoAsync) {
                eventos.publishEvent(new MidiaProcessamentoEvent(
                        galeriaId, midia.getId(), arquivo.id(), false));
            } else {
                processarMidia(midia, galeria);
            }
        }
        midiaRepository.saveAndFlush(midia);
        return midiaResponse(midia, galeriaId, true, false);
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
        MidiaGaleria midia = midiaDaGaleria(galeriaId, midiaId);
        midia.remover();
        evento(galeria, EventoGaleriaTipo.MIDIA_REMOVIDA, midia.getNomeOriginal() + " removida.");
    }

    @Transactional
    public GaleriaDtos.UploadSessaoResponse iniciarUploadEditada(
            String galeriaId, String midiaId,
            GaleriaDtos.IniciarMidiaEditadaUploadRequest request) {
        Galeria galeria = obter(galeriaId);
        MidiaGaleria midia = midiaDaGaleria(galeriaId, midiaId);
        validarVersaoEditada(galeria, midia);
        if (request.liberarAutomaticamente() != null) {
            midia.definirLiberacaoAutomatica(request.liberarAutomaticamente());
        }

        ArquivoFinalidade finalidade = ArquivoFinalidade.FOTO_EDITADA;
        ArquivoUpload.IniciarUpload comando = new ArquivoUpload.IniciarUpload(
                finalidade,
                galeriaId,
                "estudios/%s/galerias/%s/editadas/%s"
                        .formatted(galeria.getEstudioId(), galeriaId, midiaId),
                request.nomeArquivo(),
                request.mimeType(),
                request.tamanho(),
                request.checksumSha256(),
                arquivoUpload.tamanhoMaximo(finalidade));
        return GaleriaDtos.UploadSessaoResponse.de(arquivoUpload.iniciar(comando));
    }

    @Transactional
    public GaleriaDtos.MidiaResponse concluirUploadEditada(
            String galeriaId, String midiaId, String arquivoId,
            GaleriaDtos.ConcluirMidiaEditadaUploadRequest request) {
        Galeria galeria = obter(galeriaId);
        MidiaGaleria midia = midiaDaGaleria(galeriaId, midiaId);
        validarVersaoEditada(galeria, midia);

        List<ArquivoUpload.ParteEnviada> partes = request.partes().stream()
                .map(parte -> new ArquivoUpload.ParteEnviada(parte.partNumber(), parte.etag()))
                .toList();
        ArquivoUpload.ArquivoResumo arquivo = arquivoUpload.concluir(
                arquivoId, ArquivoFinalidade.FOTO_EDITADA, galeriaId, partes);
        if (processamentoAsync) {
            midia.vincularEditada(arquivo.id(), null, null);
            midiaRepository.saveAndFlush(midia);
            eventos.publishEvent(new MidiaProcessamentoEvent(
                    galeriaId, midiaId, arquivo.id(), true));
        } else {
            processarVersaoEditada(midia, galeria, arquivo.id());
            midiaRepository.saveAndFlush(midia);
        }
        evento(galeria, EventoGaleriaTipo.MIDIA_EDITADA,
                "Versao editada de " + midia.getNomeOriginal() + " enviada.");
        return midiaResponse(midia, galeriaId, true, false);
    }

    /**
     * Executa o processamento solicitado pelo listener assincrono.
     *
     * <p>PATTERN: Idempotent Consumer. Eventos antigos sao ignorados quando o
     * arquivo vinculado nao e mais o atual; o metodo recarrega o agregado em uma
     * transacao propria.
     */
    @Transactional
    public void processarMidiaSolicitada(String galeriaId, String midiaId,
                                         String arquivoId, boolean editada) {
        Galeria galeria = obter(galeriaId);
        MidiaGaleria midia = midiaDaGaleria(galeriaId, midiaId);
        // O processamento assincrono pode chegar depois de uma remocao; nesse
        // caso nao pode "ressuscitar" a midia chamando disponivel().
        if (midia.getStatus() == MidiaStatus.REMOVIDA) {
            return;
        }
        if (editada) {
            if (!arquivoId.equals(midia.getArquivoEditadoId())) {
                return;
            }
            processarVersaoEditada(midia, galeria, arquivoId);
        } else {
            if (!arquivoId.equals(midia.getArquivoOriginalId())) {
                return;
            }
            processarMidia(midia, galeria);
        }
        midiaRepository.saveAndFlush(midia);
    }

    @Transactional
    public void abortarUploadEditada(String galeriaId, String midiaId, String arquivoId) {
        obter(galeriaId);
        midiaDaGaleria(galeriaId, midiaId);
        arquivoUpload.abortar(arquivoId, ArquivoFinalidade.FOTO_EDITADA, galeriaId);
    }

    @Transactional
    public void removerVersaoEditada(String galeriaId, String midiaId) {
        Galeria galeria = obter(galeriaId);
        MidiaGaleria midia = midiaDaGaleria(galeriaId, midiaId);
        if (!midia.temVersaoEditada()) {
            return;
        }
        derivadoRepository.deleteByMidiaIdAndOrigem(midia.getId(), MidiaDerivadoOrigem.EDITADA);
        midia.removerEditada();
        if (midia.estaLiberada() && !galeria.isRawLiberado()) {
            midia.revogarLiberacao();
        }
        evento(galeria, EventoGaleriaTipo.MIDIA_EDITADA_REMOVIDA,
                "Versao editada de " + midia.getNomeOriginal() + " removida.");
    }

    @Transactional
    public GaleriaDtos.LinkResponse publicarSelecao(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        if (galeria.getStatus() == GaleriaStatus.SELECAO_ENVIADA
                || galeria.getStatus() == GaleriaStatus.EM_EDICAO
                || galeria.getStatus() == GaleriaStatus.ENTREGUE
                || galeria.getStatus() == GaleriaStatus.CANCELADA
                || galeria.getStatus() == GaleriaStatus.EXPURGADA) {
            throw new RegraDeNegocioException("SELECAO_NAO_PUBLICAVEL",
                    "A selecao so pode ser publicada antes do envio ou para renovar o link em selecao.");
        }
        validarGateSelecao(galeria);
        LinkGaleria link = linkUnicoAtivo(galeriaId)
                .map(existente -> {
                    existente.estender(DIAS_SELECAO);
                    return existente;
                })
                .orElseGet(() -> linkRepository.save(
                        LinkGaleria.criar(galeriaId, LinkGaleriaTipo.GALERIA, DIAS_SELECAO)));
        galeria.publicarSelecao();
        evento(galeria, EventoGaleriaTipo.SELECAO_PUBLICADA,
                "Link do cliente publicado por %d dias.".formatted(DIAS_SELECAO));
        return GaleriaDtos.LinkResponse.de(link);
    }

    @Transactional
    public GaleriaDtos.LinkResponse reabrirSelecao(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        // A reabertura e sempre uma acao do staff. Depois da entrega publicada
        // ela so pode ocorrer apos revogar a entrega, para nao derrubar um link
        // ativo silenciosamente.
        if (galeria.getStatus() != GaleriaStatus.EM_SELECAO
                && galeria.getStatus() != GaleriaStatus.SELECAO_ENVIADA
                && galeria.getStatus() != GaleriaStatus.EM_EDICAO) {
            throw new RegraDeNegocioException("SELECAO_NAO_REABRIVEL",
                    "Nao e possivel reabrir a selecao no status atual.");
        }
        List<PedidoExtras> pedidos = extrasRepository
                .findByGaleriaIdOrderByAuditoriaCriadoEmAsc(galeriaId);
        // Pedidos pagos ou isentos sao preservados: o reenvio cobra apenas o
        // delta. Pedidos pendentes sao recalculados e por isso cancelados.
        pedidos.stream()
                .filter(pedido -> pedido.getStatus() == PedidoExtrasStatus.PENDENTE)
                .forEach(PedidoExtras::cancelar);
        selecaoRepository.findFirstByGaleriaIdOrderByEnviadaEmDesc(galeriaId)
                .ifPresent(SelecaoGaleria::reabrir);
        LinkGaleria link = linkUnico(galeriaId)
                .map(existente -> {
                    existente.estender(DIAS_SELECAO);
                    return existente;
                })
                .orElseGet(() -> linkRepository.save(
                        LinkGaleria.criar(galeriaId, LinkGaleriaTipo.GALERIA, DIAS_SELECAO)));
        galeria.reabrirSelecao();
        evento(galeria, EventoGaleriaTipo.SELECAO_REABERTA,
                "Selecao reaberta para compra de extras adicionais.");
        return GaleriaDtos.LinkResponse.de(link);
    }

    @Transactional
    public synchronized GaleriaDtos.SelecaoResponse submeterSelecao(
            String token, List<String> fotoIds, List<String> videoIds,
            String ip, String userAgent) {
        LinkGaleria link = linkAtivoPublico(token);
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

        validarMidiasEntregues(galeria, fotos, videos);

        List<PedidoExtras> pedidos = extrasRepository
                .findByGaleriaIdOrderByAuditoriaCriadoEmAsc(galeria.getId());
        int quantidadeCoberta = quantidadeCoberta(pedidos);
        int extras = Math.max(0, fotos.size() - galeria.getFotosInclusas());
        if (extras < quantidadeCoberta) {
            throw new RegraDeNegocioException("EXTRAS_ABAIXO_DO_PAGO",
                    "A nova selecao tem menos fotos extras do que as ja cobertas "
                            + "(pagas ou isentas). Fale com o estudio para ajustar a compra.");
        }

        ResultadoDeltaExtras delta = CalculadoraExtras.calcularDelta(
                galeria.getValorFotoExtra(), galeria.getFaixasFotoExtra(),
                extras, quantidadeCoberta);
        BigDecimal valorExtras = CalculadoraExtras.calcular(
                galeria.getValorFotoExtra(), galeria.getFaixasFotoExtra(), extras).valorTotal();
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
        if (delta.quantidade() > 0 && delta.valorTotal().signum() > 0) {
            EstudioConsulta.DadosPagamentoExtras pagamentoExtras =
                    estudioConsulta.dadosPagamentoExtras();
            pedido = extrasRepository.save(PedidoExtras.criar(
                    galeria.getId(), selecao.getId(), delta.quantidade(),
                    galeria.getValorFotoExtra(), delta.valorTotal(),
                    delta.detalhamento(),
                    pagamentoExtras.chavePix(), pagamentoExtras.instrucoesPagamento()));
        }
        galeria.registrarSelecao(selecao.getId(), pedido == null ? null : pedido.getId());
        evento(galeria, EventoGaleriaTipo.SELECAO_ENVIADA,
                pedido == null ? "Selecao enviada pelo cliente."
                        : quantidadeCoberta > 0
                        ? "Selecao enviada com %d foto(s) extra(s) adicional(is)."
                        .formatted(delta.quantidade())
                        : "Selecao enviada com %d foto(s) extra(s).".formatted(extras));
        link.estender(DIAS_ENTREGA);
        if (!extrasRepository.existsByGaleriaIdAndStatus(galeria.getId(), PedidoExtrasStatus.PENDENTE)) {
            iniciarEdicaoComEvento(galeria, "Edicao iniciada automaticamente apos a selecao.");
            liberarMidiasProntas(galeria);
        }
        return selecaoResponse(selecao);
    }

    @Transactional
    public GaleriaDtos.PedidoExtrasResponse confirmarExtras(
            String galeriaId, GaleriaDtos.ExtrasConfirmacaoRequest request) {
        Galeria galeria = obter(galeriaId);
        PedidoExtras pedido = pedidoPendente(galeriaId);
        if (request.formaPagamento() == FormaPagamentoSaldo.PIX
                && pedido.getComprovanteArquivoId() == null) {
            throw new RegraDeNegocioException("COMPROVANTE_OBRIGATORIO",
                    "O cliente precisa enviar o comprovante Pix dos extras.");
        }
        pedido.pagar(request.formaPagamento(),
                request.confirmadoPor() == null ? "Equipe do estudio" : request.confirmadoPor());
        evento(galeria, EventoGaleriaTipo.EXTRAS_CONFIRMADOS, "Extras confirmados.");
        iniciarEdicaoSeSemExtrasPendentes(galeria,
                "Edicao iniciada apos confirmacao dos extras.");
        return extrasResponse(pedido, true);
    }

    @Transactional
    public GaleriaDtos.PedidoExtrasResponse isentarExtras(String galeriaId, String por) {
        Galeria galeria = obter(galeriaId);
        PedidoExtras pedido = pedidoPendente(galeriaId);
        pedido.isentar(por == null ? "Equipe do estudio" : por);
        evento(galeria, EventoGaleriaTipo.EXTRAS_ISENTOS, "Extras isentos.");
        iniciarEdicaoSeSemExtrasPendentes(galeria,
                "Edicao iniciada apos isencao dos extras.");
        return extrasResponse(pedido, true);
    }

    @Transactional
    public GaleriaDtos.PedidoExtrasResponse liberarEdicaoManual(
            String galeriaId, GaleriaDtos.LiberacaoEdicaoRequest request) {
        Galeria galeria = obter(galeriaId);
        PedidoExtras pedido = pedidoPendente(galeriaId);
        pedido.liberarManual(request.motivo(), request.por());
        iniciarEdicaoSeSemExtrasPendentes(galeria,
                "Edicao liberada manualmente: " + request.motivo());
        return extrasResponse(pedido, true);
    }

    private void iniciarEdicaoSeSemExtrasPendentes(Galeria galeria, String detalhe) {
        if (!extrasRepository.existsByGaleriaIdAndStatus(
                galeria.getId(), PedidoExtrasStatus.PENDENTE)) {
            iniciarEdicaoComEvento(galeria, detalhe);
            liberarMidiasProntas(galeria);
        }
    }

    @Transactional
    public GaleriaDtos.UploadSessaoResponse iniciarComprovanteExtras(
            String token, GaleriaDtos.IniciarComprovanteExtrasRequest request) {
        LinkGaleria link = linkAtivoPublico(token);
        Galeria galeria = obter(link.getGaleriaId());
        PedidoExtras pedido = pedidoPendente(galeria.getId());
        ArquivoUpload.IniciarUpload comando = new ArquivoUpload.IniciarUpload(
                ArquivoFinalidade.COMPROVANTE_PAGAMENTO,
                pedido.getId(),
                "estudios/%s/galerias/%s/extras/%s".formatted(
                        galeria.getEstudioId(), galeria.getId(), pedido.getId()),
                request.nomeArquivo(),
                request.mimeType(),
                request.tamanho(),
                request.checksumSha256(),
                arquivoUpload.tamanhoMaximo(ArquivoFinalidade.COMPROVANTE_PAGAMENTO));
        return GaleriaDtos.UploadSessaoResponse.de(arquivoUpload.iniciar(comando));
    }

    @Transactional
    public GaleriaDtos.PedidoExtrasResponse concluirComprovanteExtras(
            String token, String arquivoId,
            GaleriaDtos.ConcluirComprovanteExtrasRequest request) {
        LinkGaleria link = linkAtivoPublico(token);
        Galeria galeria = obter(link.getGaleriaId());
        PedidoExtras pedido = pedidoPendente(galeria.getId());
        List<ArquivoUpload.ParteEnviada> partes = request.partes().stream()
                .map(parte -> new ArquivoUpload.ParteEnviada(parte.partNumber(), parte.etag()))
                .toList();
        ArquivoUpload.ArquivoResumo arquivo = arquivoUpload.concluir(
                arquivoId, ArquivoFinalidade.COMPROVANTE_PAGAMENTO, pedido.getId(), partes);
        pedido.anexarComprovante(arquivo.id(), arquivo.nomeOriginal(), Instant.now());
        evento(galeria, EventoGaleriaTipo.COMPROVANTE_EXTRAS_ENVIADO,
                "Comprovante dos extras enviado: " + arquivo.nomeOriginal());
        return extrasResponse(pedido, false);
    }

    @Transactional
    public void abortarComprovanteExtras(String token, String arquivoId) {
        LinkGaleria link = linkAtivoPublico(token);
        Galeria galeria = obter(link.getGaleriaId());
        PedidoExtras pedido = pedidoPendente(galeria.getId());
        arquivoUpload.abortar(arquivoId, ArquivoFinalidade.COMPROVANTE_PAGAMENTO,
                pedido.getId());
    }

    private PedidoExtras pedidoPendente(String galeriaId) {
        return extrasRepository
                .findFirstByGaleriaIdAndStatusOrderByAuditoriaCriadoEmDesc(
                        galeriaId, PedidoExtrasStatus.PENDENTE)
                .orElseThrow(() -> new RegraDeNegocioException("SEM_EXTRAS_PENDENTES",
                        "Esta galeria nao possui pedido de extras pendente."));
    }

    private GaleriaDtos.PedidoExtrasResponse extrasResponse(PedidoExtras pedido,
                                                             boolean incluirComprovanteUrl) {
        String chavePix = pedido.getChavePix();
        String instrucoes = pedido.getInstrucoesPagamento();
        if (chavePix == null || instrucoes == null) {
            EstudioConsulta.DadosPagamentoExtras dados = estudioConsulta.dadosPagamentoExtras();
            if (chavePix == null) {
                chavePix = dados.chavePix();
            }
            if (instrucoes == null) {
                instrucoes = dados.instrucoesPagamento();
            }
        }
        String comprovanteUrl = null;
        if (incluirComprovanteUrl && pedido.getComprovanteArquivoId() != null) {
            comprovanteUrl = urlOuNull(pedido.getComprovanteArquivoId(),
                    ArquivoFinalidade.COMPROVANTE_PAGAMENTO, pedido.getId());
        }
        return GaleriaDtos.PedidoExtrasResponse.de(
                pedido, chavePix, instrucoes, comprovanteUrl);
    }

    @Transactional
    public void iniciarEdicao(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        if (extrasRepository.existsByGaleriaIdAndStatus(
                galeriaId, PedidoExtrasStatus.PENDENTE)) {
            throw new RegraDeNegocioException("EXTRAS_PENDENTES",
                    "Confirme ou isente os extras antes de iniciar a edicao.");
        }
        iniciarEdicaoComEvento(galeria, "Edicao iniciada.");
        liberarMidiasProntas(galeria);
    }

    private void iniciarEdicaoComEvento(Galeria galeria, String detalhe) {
        galeria.iniciarEdicao();
        evento(galeria, EventoGaleriaTipo.EDICAO_INICIADA, detalhe);
    }

    @Transactional
    public GaleriaDtos.LinkResponse publicarEntrega(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        if (galeria.getStatus() != GaleriaStatus.EM_EDICAO
                && galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            throw new RegraDeNegocioException("GALERIA_NAO_PRONTA",
                    "A galeria precisa estar em edicao para finalizar a entrega.");
        }
        if (extrasRepository.existsByGaleriaIdAndStatus(
                galeriaId, PedidoExtrasStatus.PENDENTE)) {
            throw new RegraDeNegocioException("EXTRAS_PENDENTES",
                    "Confirme ou isente os extras antes de finalizar a entrega.");
        }
        SelecaoGaleria selecao = selecaoRepository
                .findFirstByGaleriaIdOrderByEnviadaEmDesc(galeriaId)
                .orElseThrow(() -> new RegraDeNegocioException("SEM_SELECAO",
                        "A galeria ainda nao possui selecao enviada."));

        liberarMidiasProntas(galeria);
        long semEntrega = fotosSemEntrega(galeria);
        if (semEntrega > 0) {
            throw new RegraDeNegocioException("FOTOS_SEM_ENTREGA",
                    "%d foto(s) selecionada(s) ainda nao possuem versao editada e o RAW nao foi liberado."
                            .formatted(semEntrega));
        }
        List<MidiaGaleria> selecionadas = midiasDaSelecao(galeriaId);
        long naoLiberadas = selecionadas.stream()
                .filter(midia -> !midia.estaLiberada())
                .count();
        if (naoLiberadas > 0) {
            throw new RegraDeNegocioException("MIDIAS_NAO_LIBERADAS",
                    "%d midia(s) selecionada(s) ainda nao foram liberadas. "
                            .formatted(naoLiberadas)
                            + "Libere o lote ou ative a auto-liberacao antes de finalizar.");
        }
        selecionadas.forEach(MidiaGaleria::entregar);
        LinkGaleria link = linkUnico(galeriaId)
                .map(existente -> {
                    existente.estender(DIAS_ENTREGA);
                    return existente;
                })
                .orElseGet(() -> linkRepository.save(
                        LinkGaleria.criar(galeriaId, LinkGaleriaTipo.GALERIA, DIAS_ENTREGA)));
        galeria.registrarSelecaoEntregue(selecao.getId());
        galeria.publicarEntrega();
        evento(galeria, EventoGaleriaTipo.ENTREGA_PUBLICADA,
                "Entrega finalizada. Link do cliente renovado por %d dias."
                        .formatted(DIAS_ENTREGA));
        return GaleriaDtos.LinkResponse.de(link);
    }

    @Transactional
    public void revogarEntrega(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        midiasDaSelecao(galeriaId).forEach(MidiaGaleria::revogarLiberacao);
        galeria.revogarEntrega();
        evento(galeria, EventoGaleriaTipo.ENTREGA_REVOGADA,
                "Entrega revogada. As midias continuam travadas, mas os downloads foram suspensos.");
    }

    /**
     * Libera manualmente um lote de midias selecionadas e prontas.
     *
     * <p>Ignora o flag de auto-liberacao: e uma acao explicita do staff.
     */
    @Transactional
    public GaleriaDtos.LiberarLoteResponse liberarLote(String galeriaId, List<String> midiaIds) {
        Galeria galeria = obter(galeriaId);
        validarLiberacaoProgressiva(galeria);
        List<String> ids = idsUnicos(midiaIds);
        List<MidiaGaleria> selecionadas = midiasDaSelecao(galeriaId);
        Map<String, MidiaGaleria> porId = new LinkedHashMap<>();
        for (MidiaGaleria midia : selecionadas) {
            porId.put(midia.getId(), midia);
        }
        List<MidiaGaleria> lote = new ArrayList<>();
        for (String id : ids) {
            MidiaGaleria midia = porId.get(id);
            if (midia == null) {
                throw new RegraDeNegocioException("MIDIA_NAO_SELECIONADA",
                        "Uma ou mais midias do lote nao pertencem a selecao atual.");
            }
            if (!midiaPronta(galeria, midia)) {
                throw new RegraDeNegocioException("MIDIA_NAO_PRONTA",
                        "Uma ou mais midias do lote ainda nao possuem versao final.");
            }
            lote.add(midia);
        }
        if (lote.isEmpty()) {
            return new GaleriaDtos.LiberarLoteResponse(0, totalLiberado(selecionadas),
                    selecionadas.size());
        }
        lote.forEach(MidiaGaleria::liberar);
        evento(galeria, EventoGaleriaTipo.ENTREGA_LOTE_LIBERADO,
                "%d midia(s) liberadas manualmente.".formatted(lote.size()));
        publicarEventoLiberacao(galeria, lote.size());
        return new GaleriaDtos.LiberarLoteResponse(lote.size(), totalLiberado(selecionadas),
                selecionadas.size());
    }

    /** Libera todas as midias selecionadas que ja estao prontas, ignorando o flag. */
    @Transactional
    public GaleriaDtos.LiberarLoteResponse liberarProntas(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        validarLiberacaoProgressiva(galeria);
        List<MidiaGaleria> selecionadas = midiasDaSelecao(galeriaId);
        List<MidiaGaleria> prontas = selecionadas.stream()
                .filter(midia -> !midia.estaLiberada())
                .filter(midia -> midiaPronta(galeria, midia))
                .toList();
        if (prontas.isEmpty()) {
            return new GaleriaDtos.LiberarLoteResponse(0, totalLiberado(selecionadas),
                    selecionadas.size());
        }
        prontas.forEach(MidiaGaleria::liberar);
        evento(galeria, EventoGaleriaTipo.ENTREGA_LOTE_LIBERADO,
                "%d midia(s) prontas liberadas manualmente.".formatted(prontas.size()));
        publicarEventoLiberacao(galeria, prontas.size());
        return new GaleriaDtos.LiberarLoteResponse(prontas.size(), totalLiberado(selecionadas),
                selecionadas.size());
    }

    @Transactional
    public GaleriaDtos.LiberarLoteResponse revogarLiberacao(String galeriaId,
                                                             List<String> midiaIds) {
        Galeria galeria = obter(galeriaId);
        List<String> ids = idsUnicos(midiaIds);
        List<MidiaGaleria> selecionadas = midiasDaSelecao(galeriaId);
        Map<String, MidiaGaleria> porId = new LinkedHashMap<>();
        for (MidiaGaleria midia : selecionadas) {
            porId.put(midia.getId(), midia);
        }
        List<MidiaGaleria> lote = new ArrayList<>();
        for (String id : ids) {
            MidiaGaleria midia = porId.get(id);
            if (midia == null) {
                throw new RegraDeNegocioException("MIDIA_NAO_SELECIONADA",
                        "Uma ou mais midias do lote nao pertencem a selecao atual.");
            }
            lote.add(midia);
        }
        lote.forEach(MidiaGaleria::revogarLiberacao);
        evento(galeria, EventoGaleriaTipo.ENTREGA_LIBERACAO_REVOGADA,
                "%d midia(s) tiveram a liberacao revogada.".formatted(lote.size()));
        return new GaleriaDtos.LiberarLoteResponse(lote.size(), totalLiberado(selecionadas),
                selecionadas.size());
    }

    @Transactional
    public GaleriaDtos.MidiaResponse definirAutoLiberacao(String galeriaId, String midiaId,
                                                           boolean liberarAutomaticamente) {
        Galeria galeria = obter(galeriaId);
        MidiaGaleria midia = midiaDaGaleria(galeriaId, midiaId);
        midia.definirLiberacaoAutomatica(liberarAutomaticamente);
        evento(galeria, EventoGaleriaTipo.LIBERACAO_AUTOMATICA_ALTERADA,
                "Auto-liberacao de " + midia.getNomeOriginal() + " "
                        + (liberarAutomaticamente ? "ativada" : "desativada") + ".");
        return midiaResponse(midia, galeriaId, true, false);
    }

    @Transactional
    public GaleriaDtos.MidiaResponse alterarFinalidade(String galeriaId, String midiaId,
                                                        String valor) {
        Galeria galeria = obter(galeriaId);
        MidiaGaleria midia = midiaDaGaleria(galeriaId, midiaId);
        if (!galeria.aceitaNovasMidias()) {
            throw new RegraDeNegocioException("FINALIDADE_BLOQUEADA",
                    "Altere a finalidade antes de publicar a selecao. "
                            + "Depois disso, reabra a selecao para corrigir.");
        }
        if (midia.getTipo() != MidiaTipo.FOTO) {
            throw new RegraDeNegocioException("FINALIDADE_APENAS_FOTOS",
                    "Somente fotos podem ter a finalidade alterada.");
        }
        MidiaFinalidade finalidade = MidiaFinalidade.de(valor);
        if (finalidade == null) {
            throw new RegraDeNegocioException("FINALIDADE_INVALIDA",
                    "Informe a finalidade 'edicao' ou 'pronta'.");
        }
        if (finalidade == MidiaFinalidade.PRONTA && midia.temVersaoEditada()) {
            throw new RegraDeNegocioException("MIDIA_JA_EDITADA",
                    "Remova a versao editada antes de marcar a foto como pronta.");
        }
        if (midia.getFinalidade() != finalidade) {
            midia.definirFinalidade(finalidade);
            evento(galeria, EventoGaleriaTipo.MIDIA_FINALIDADE_ALTERADA,
                    midia.getNomeOriginal() + (finalidade == MidiaFinalidade.PRONTA
                            ? " marcada como ja editada." : " marcada para edicao."));
        }
        return midiaResponse(midia, galeriaId, true, false);
    }

    @Transactional
    public GaleriaDtos.LinkResponse renovarLink(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        LinkGaleria link = linkUnico(galeriaId)
                .map(existente -> {
                    existente.estender(DIAS_ENTREGA);
                    return existente;
                })
                .orElseGet(() -> linkRepository.save(
                        LinkGaleria.criar(galeriaId, LinkGaleriaTipo.GALERIA, DIAS_ENTREGA)));
        evento(galeria, EventoGaleriaTipo.SELECAO_PUBLICADA,
                "Link do cliente renovado por %d dias.".formatted(DIAS_ENTREGA));
        return GaleriaDtos.LinkResponse.de(link);
    }

    private void validarLiberacaoProgressiva(Galeria galeria) {
        if (extrasRepository.existsByGaleriaIdAndStatus(
                galeria.getId(), PedidoExtrasStatus.PENDENTE)) {
            throw new RegraDeNegocioException("EXTRAS_PENDENTES",
                    "Confirme ou isente os extras antes de liberar downloads.");
        }
        if (galeria.getStatus() != GaleriaStatus.EM_EDICAO
                && galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            throw new RegraDeNegocioException("GALERIA_NAO_PRONTA",
                    "A galeria precisa estar em edicao para liberar midias.");
        }
    }

    private void liberarMidiasProntas(Galeria galeria) {
        if (galeria.getStatus() != GaleriaStatus.EM_EDICAO
                && galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            return;
        }
        if (extrasRepository.existsByGaleriaIdAndStatus(
                galeria.getId(), PedidoExtrasStatus.PENDENTE)) {
            return;
        }
        List<MidiaGaleria> prontas = midiasDaSelecao(galeria.getId()).stream()
                .filter(MidiaGaleria::isLiberacaoAutomatica)
                .filter(midia -> !midia.estaLiberada())
                .filter(midia -> midiaPronta(galeria, midia))
                .toList();
        if (prontas.isEmpty()) {
            return;
        }
        prontas.forEach(MidiaGaleria::liberar);
        evento(galeria, EventoGaleriaTipo.ENTREGA_LOTE_LIBERADO,
                "%d midia(s) prontas liberadas automaticamente.".formatted(prontas.size()));
        publicarEventoLiberacao(galeria, prontas.size());
    }

    private boolean midiaPronta(Galeria galeria, MidiaGaleria midia) {
        return midia.prontaParaEntrega(galeria.isRawLiberado());
    }

    private int totalLiberado(List<MidiaGaleria> midias) {
        return (int) midias.stream().filter(MidiaGaleria::estaLiberada).count();
    }

    private void publicarEventoLiberacao(Galeria galeria, int quantidade) {
        // A integracao real de WhatsApp e conectada via listener deste evento.
        List<MidiaGaleria> selecionadas = midiasDaSelecao(galeria.getId());
        String token = linkUnico(galeria.getId())
                .map(LinkGaleria::getToken)
                .orElse(null);
        eventos.publishEvent(new LoteLiberadoEvent(
                galeria.getId(),
                galeria.getClienteId(),
                token,
                quantidade,
                totalLiberado(selecionadas),
                selecionadas.size()));
    }

    /**
     * @deprecated RAW não é mais exposto na UI; mantido para compatibilidade,
     * automação e QA.
     */
    @Deprecated(since = "2026-11", forRemoval = false)
    @Transactional
    public GaleriaDtos.GaleriaResponse liberarRaw(String galeriaId, String por) {
        Galeria galeria = obter(galeriaId);
        validarLiberacaoRaw(galeria);
        galeria.liberarRaw(por);
        liberarMidiasProntas(galeria);
        evento(galeria, EventoGaleriaTipo.RAW_LIBERADO,
                "Arquivos originais liberados para download.");
        return response(galeria);
    }

    /**
     * @deprecated RAW não é mais exposto na UI; mantido para compatibilidade,
     * automação e QA.
     */
    @Deprecated(since = "2026-11", forRemoval = false)
    @Transactional
    public GaleriaDtos.GaleriaResponse revogarRaw(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        if (!galeria.isRawLiberado()) {
            return response(galeria);
        }
        if (fotosSemEdicao(galeria) > 0) {
            throw new RegraDeNegocioException("RAW_EM_USO",
                    "Envie as versoes editadas antes de revogar o RAW; ha fotos sem versao final.");
        }
        galeria.revogarRaw();
        evento(galeria, EventoGaleriaTipo.RAW_REVOGADO,
                "Liberacao dos arquivos originais revogada.");
        return response(galeria);
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

        SelecaoGaleria selecao = selecaoRepository
                .findFirstByGaleriaIdOrderByEnviadaEmDesc(galeria.getId()).orElse(null);
        List<PedidoExtras> pedidosExtras = extrasRepository
                .findByGaleriaIdOrderByAuditoriaCriadoEmAsc(galeria.getId());
        PedidoExtras extras = pedidosExtras.isEmpty()
                ? null : pedidosExtras.get(pedidosExtras.size() - 1);
        String fase = fasePublica(galeria, pedidosExtras);
        boolean selecaoEnviada = selecao != null
                && galeria.getStatus() != GaleriaStatus.EM_SELECAO
                && galeria.getStatus() != GaleriaStatus.AGUARDANDO_UPLOAD;
        List<MidiaGaleria> midias = selecaoEnviada
                ? midiasDaSelecao(galeria.getId())
                : midiaRepository.findByGaleriaIdAndStatusNotOrderByOrdemAsc(
                        galeria.getId(), MidiaStatus.REMOVIDA);
        List<MidiaGaleria> selecionadas = selecao == null
                ? List.of() : midiasDaSelecao(galeria.getId());

        int fotosSelecionadas = contarTipo(selecionadas, MidiaTipo.FOTO);
        int fotosLiberadas = contarTipoLiberado(selecionadas, MidiaTipo.FOTO);
        int videosSelecionados = contarTipo(selecionadas, MidiaTipo.VIDEO);
        int videosLiberados = contarTipoLiberado(selecionadas, MidiaTipo.VIDEO);

        // Links antigos de ENTREGA continuam expondo o original para a pagina
        // legada. O link unico nunca revela RAW; o download resolve a versao.
        boolean exporOriginal = link.getTipo() == LinkGaleriaTipo.ENTREGA
                && galeria.isRawLiberado();
        GaleriaDtos.GaleriaResponse galeriaResponse = response(galeria);
        return new GaleriaDtos.GaleriaPublicaResponse(
                GaleriaDtos.LinkResponse.de(link),
                galeriaResponse,
                midias.stream().map(midia -> midiaResponse(midia, galeria.getId(),
                        exporOriginal, true)).toList(),
                selecao == null ? null : selecaoResponse(selecao),
                extras == null ? null : extrasResponse(extras, false),
                resumoExtras(galeria, pedidosExtras),
                midiasEntregues(galeria),
                "em_liberacao".equals(fase) || "entregue".equals(fase),
                fase,
                fotosSelecionadas,
                fotosLiberadas,
                videosSelecionados,
                videosLiberados,
                estudioConsulta.nome(),
                galeriaResponse.pacoteNome());
    }

    private String fasePublica(Galeria galeria, List<PedidoExtras> pedidos) {
        if (galeria.getStatus() == GaleriaStatus.CANCELADA) {
            return "cancelada";
        }
        if (galeria.getStatus() == GaleriaStatus.EM_SELECAO
                || galeria.getStatus() == GaleriaStatus.AGUARDANDO_UPLOAD) {
            return "selecao";
        }
        if (galeria.getStatus() == GaleriaStatus.SELECAO_ENVIADA) {
            boolean pendente = pedidos.stream()
                    .anyMatch(pedido -> pedido.getStatus() == PedidoExtrasStatus.PENDENTE);
            return pendente ? "aguardando_pagamento" : "em_liberacao";
        }
        if (galeria.getStatus() == GaleriaStatus.ENTREGUE) {
            return "entregue";
        }
        return "em_liberacao";
    }

    private int contarTipo(List<MidiaGaleria> midias, MidiaTipo tipo) {
        return (int) midias.stream().filter(midia -> midia.getTipo() == tipo).count();
    }

    private int contarTipoLiberado(List<MidiaGaleria> midias, MidiaTipo tipo) {
        return (int) midias.stream()
                .filter(midia -> midia.getTipo() == tipo)
                .filter(MidiaGaleria::estaLiberada)
                .count();
    }

    @Transactional
    public GaleriaDtos.DownloadResponse registrarDownload(
            String token, String midiaId, String versao, String ip, String userAgent) {
        LinkGaleria link = linkAtivoPublico(token);
        Galeria galeria = obter(link.getGaleriaId());
        if (extrasRepository.existsByGaleriaIdAndStatus(
                galeria.getId(), PedidoExtrasStatus.PENDENTE)) {
            throw new RegraDeNegocioException("EXTRAS_PENDENTES",
                    "Confirme os extras antes de baixar as fotos.");
        }
        if (galeria.getStatus() != GaleriaStatus.EM_EDICAO
                && galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            throw new RegraDeNegocioException("ENTREGA_INDISPONIVEL",
                    "A entrega ainda nao foi liberada.");
        }
        MidiaGaleria midia = midiaRepository.findByIdAndGaleriaId(midiaId, galeria.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Midia", midiaId));
        if (!midia.estaDisponivel() || !midiaSelecionada(galeria.getId(), midiaId)) {
            throw new RecursoNaoEncontradoException("Midia", midiaId);
        }
        if (!midia.estaLiberada()) {
            throw new RegraDeNegocioException("MIDIA_NAO_LIBERADA",
                    "Esta midia ainda nao foi liberada para download.");
        }

        String solicitada = versao == null || versao.isBlank()
                ? "auto" : versao.trim().toLowerCase(Locale.ROOT);
        if (!"auto".equals(solicitada) && !"original".equals(solicitada)
                && !"raw".equals(solicitada) && !"editada".equals(solicitada)) {
            throw new RegraDeNegocioException("VERSAO_INVALIDA",
                    "Informe a versao 'auto', 'editada' ou 'original'.");
        }

        boolean prontaNaOrigem = midia.prontaNaOrigem();
        boolean original;
        if (midia.getTipo() == MidiaTipo.VIDEO || prontaNaOrigem) {
            if (prontaNaOrigem && "editada".equals(solicitada)) {
                throw new RegraDeNegocioException("EDICAO_NAO_DISPONIVEL",
                        "Esta foto ja foi publicada como versao final.");
            }
            original = true;
        } else if ("auto".equals(solicitada)) {
            original = !midia.temVersaoEditada();
        } else if ("original".equals(solicitada) || "raw".equals(solicitada)) {
            original = true;
        } else {
            original = false;
        }

        if (original && midia.getTipo() == MidiaTipo.FOTO
                && !prontaNaOrigem && !galeria.isRawLiberado()) {
            throw new RegraDeNegocioException("RAW_NAO_LIBERADO",
                    "Os arquivos originais ainda nao foram liberados.");
        }
        if (!original && !midia.temVersaoEditada()) {
            throw new RegraDeNegocioException("EDICAO_NAO_DISPONIVEL",
                    "Esta foto ainda nao possui versao editada.");
        }

        String arquivoId = original ? midia.getArquivoOriginalId() : midia.getArquivoEditadoId();
        ArquivoFinalidade finalidade = original
                ? (midia.getTipo() == MidiaTipo.FOTO
                ? ArquivoFinalidade.FOTO_ORIGINAL : ArquivoFinalidade.VIDEO_ORIGINAL)
                : ArquivoFinalidade.FOTO_EDITADA;
        ArquivoUpload.UrlDownload download = arquivoUpload.urlDownload(
                arquivoId, finalidade, galeria.getId());
        downloadRepository.save(DownloadGaleria.criar(
                galeria.getId(), link.getId(), midia.getId(), arquivoId,
                original ? "ORIGINAL" : "EDITADA", ip, userAgent));
        evento(galeria, EventoGaleriaTipo.DOWNLOAD_REGISTRADO,
                midia.getNomeOriginal() + " baixada.");
        return new GaleriaDtos.DownloadResponse(
                download.url(), download.expiraEm(), midia.getNomeOriginal());
    }

    private void validarLiberacaoRaw(Galeria galeria) {
        if (galeria.getStatus() != GaleriaStatus.SELECAO_ENVIADA
                && galeria.getStatus() != GaleriaStatus.EM_EDICAO
                && galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            throw new RegraDeNegocioException("RAW_INDISPONIVEL",
                    "Libere os originais apos a selecao do cliente.");
        }
        if (selecaoRepository.findFirstByGaleriaIdOrderByEnviadaEmDesc(galeria.getId()).isEmpty()) {
            throw new RegraDeNegocioException("SEM_SELECAO",
                    "A galeria ainda nao possui selecao enviada.");
        }
    }

    /**
     * Conta fotos selecionadas que ainda nao podem ser entregues.
     *
     * <p>Uma foto e entregavel quando possui versao editada ou quando o RAW foi
     * liberado explicitamente pelo staff. Sem isso, publicar a entrega
     * disponibilizaria o original por acidente.
     */
    private long fotosSemEntrega(Galeria galeria) {
        if (galeria.isRawLiberado()) {
            return 0;
        }
        return fotosSemEdicao(galeria);
    }

    private long fotosSemEdicao(Galeria galeria) {
        return midiasDaSelecao(galeria.getId()).stream()
                .filter(midia -> midia.getTipo() == MidiaTipo.FOTO)
                .filter(midia -> !midia.prontaNaOrigem())
                .filter(midia -> !midia.temVersaoEditada())
                .count();
    }

    /**
     * Valida o ZIP antes de iniciar o streaming.
     *
     * <p>PATTERN: Guard Clauses. O controller so abre a resposta depois de
     * confirmar link, permissao de RAW e existencia de arquivos; assim os erros
     * ainda podem ser devolvidos como JSON.
     */
    @Transactional(readOnly = true)
    public int validarZip(String token, String versao) {
        return entradasZip(token, versao).size();
    }

    /**
     * Valida e registra o download em lote.
     *
     * <p>O ZIP e streaming; registrar antes de escrever garante que o evento
     * apareca no historico mesmo se o cliente interromper a conexao no meio.
     */
    @Transactional
    public int registrarDownloadsZip(String token, String versao, String ip, String userAgent) {
        LinkGaleria link = linkAtivoPublico(token);
        Galeria galeria = obter(link.getGaleriaId());
        List<EntradaZip> entradas = entradasZip(token, versao);
        for (EntradaZip entrada : entradas) {
            downloadRepository.save(DownloadGaleria.criar(
                    entrada.galeriaId(), link.getId(), entrada.midiaId(),
                    entrada.arquivoId(), entrada.versao(), ip, userAgent));
        }
        galeria.registrarZipBaixado();
        evento(galeria, EventoGaleriaTipo.DOWNLOAD_REGISTRADO,
                "%d arquivo(s) baixados em lote (%s)."
                        .formatted(entradas.size(), versao));
        return entradas.size();
    }

    /**
     * Prepara o download interno das fotos selecionadas para edicao.
     *
     * <p>A URL devolvida e relativa a base da API e usa um token de capability
     * de curta duracao, pois o navegador nao envia o header Authorization em
     * uma navegacao direta de download.
     */
    @Transactional
    public GaleriaDtos.DownloadSelecaoResponse prepararDownloadSelecao(String galeriaId) {
        ContextoDownloadSelecao contexto = contextoDownloadSelecaoDaGaleria(galeriaId);
        validarTamanhoZipSelecao(contexto);

        tokenDownloadRepository.deleteByExpiraEmBefore(Instant.now());
        TokenDownloadSelecao token = tokenDownloadRepository.save(
                TokenDownloadSelecao.criar(galeriaId, contexto.selecao().getId(),
                        VALIDADE_DOWNLOAD_SELECAO));
        return new GaleriaDtos.DownloadSelecaoResponse(
                "/public/galerias/downloads/%s/selecao.zip".formatted(token.getToken()),
                token.getExpiraEm(),
                contexto.fotos().size());
    }

    /**
     * Valida o token e registra o evento antes do streaming.
     *
     * <p>O registro antecipado mantem o historico mesmo se o cliente fechar a
     * conexao no meio do ZIP, seguindo o mesmo criterio do ZIP de entrega.
     */
    @Transactional
    public int registrarDownloadSelecao(String token) {
        ContextoDownloadSelecao contexto = contextoDownloadSelecaoDoToken(token);
        validarTamanhoZipSelecao(contexto);
        evento(contexto.galeria(), EventoGaleriaTipo.DOWNLOAD_REGISTRADO,
                "%d foto(s) selecionada(s) baixadas para edicao (protocolo %s)."
                        .formatted(contexto.fotos().size(),
                                contexto.selecao().getProtocolo()));
        return contexto.fotos().size();
    }

    /** Escreve o ZIP das fotos selecionadas direto na resposta, sem buffer total. */
    @Transactional(readOnly = true)
    public void escreverZipSelecao(String token, OutputStream saida) throws IOException {
        ContextoDownloadSelecao contexto = contextoDownloadSelecaoDoToken(token);
        validarTamanhoZipSelecao(contexto);

        Set<String> nomes = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(saida)) {
            for (MidiaGaleria foto : contexto.fotos()) {
                String nome = "%03d_%s".formatted(foto.getOrdem(), foto.getNomeOriginal());
                zip.putNextEntry(new ZipEntry(nomeUnico(nome, nomes)));
                try (InputStream conteudo = arquivoUpload.abrirStream(
                        foto.getArquivoOriginalId(), ArquivoFinalidade.FOTO_ORIGINAL,
                        contexto.galeria().getId())) {
                    conteudo.transferTo(zip);
                }
                zip.closeEntry();
            }
            zip.finish();
        }
    }

    private ContextoDownloadSelecao contextoDownloadSelecaoDaGaleria(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        SelecaoGaleria selecao = selecaoRepository
                .findFirstByGaleriaIdOrderByEnviadaEmDesc(galeriaId)
                .orElseThrow(() -> new RegraDeNegocioException("SEM_SELECAO",
                        "A galeria ainda nao possui selecao enviada."));
        if (selecao.getStatus() != SelecaoStatus.ENVIADA) {
            throw new RegraDeNegocioException("SELECAO_NAO_ENVIADA",
                    "A selecao precisa estar enviada para baixar as fotos.");
        }
        List<MidiaGaleria> fotos = fotosDaSelecao(galeria, selecao);
        if (fotos.isEmpty()) {
            throw new RegraDeNegocioException("SELECAO_SEM_FOTOS",
                    "A selecao nao possui fotos disponiveis para download.");
        }
        return new ContextoDownloadSelecao(galeria, selecao, fotos);
    }

    private ContextoDownloadSelecao contextoDownloadSelecaoDoToken(String token) {
        TokenDownloadSelecao download = tokenDownloadRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Token de download da selecao invalido ou inexistente."));
        if (!download.estaValido()) {
            throw new RecursoExpiradoException("TOKEN_DOWNLOAD_EXPIRADO",
                    "Este link de download expirou. Solicite um novo na galeria.");
        }
        Galeria galeria = obter(download.getGaleriaId());
        SelecaoGaleria selecao = selecaoRepository.findById(download.getSelecaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Selecao", download.getSelecaoId()));
        List<MidiaGaleria> fotos = fotosDaSelecao(galeria, selecao);
        if (fotos.isEmpty()) {
            throw new RegraDeNegocioException("SELECAO_SEM_FOTOS",
                    "A selecao nao possui fotos disponiveis para download.");
        }
        return new ContextoDownloadSelecao(galeria, selecao, fotos);
    }

    private List<MidiaGaleria> fotosDaSelecao(Galeria galeria, SelecaoGaleria selecao) {
        List<String> ids = selecaoItemRepository.findBySelecaoId(selecao.getId()).stream()
                .filter(item -> MidiaTipo.FOTO.name().equals(item.getTipo()))
                .map(SelecaoItem::getMidiaId)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<MidiaGaleria> fotos = midiaRepository.findByGaleriaIdAndIdIn(galeria.getId(), ids);
        // Nunca omitir silenciosamente uma foto escolhida pelo cliente: se ela
        // foi removida ou falhou, o staff precisa saber antes de editar.
        if (fotos.size() != ids.size() || fotos.stream().anyMatch(foto -> !foto.estaDisponivel())) {
            throw new RegraDeNegocioException("SELECAO_FOTO_INDISPONIVEL",
                    "Uma ou mais fotos selecionadas nao estao disponiveis para download.");
        }
        return fotos.stream()
                .sorted(Comparator.comparingInt(MidiaGaleria::getOrdem))
                .toList();
    }

    private void validarTamanhoZipSelecao(ContextoDownloadSelecao contexto) {
        long totalBytes = 0;
        for (MidiaGaleria foto : contexto.fotos()) {
            totalBytes += arquivoUpload.buscar(foto.getArquivoOriginalId(),
                            ArquivoFinalidade.FOTO_ORIGINAL, contexto.galeria().getId())
                    .map(ArquivoUpload.ArquivoResumo::tamanhoBytes)
                    .orElse(0L);
            if (totalBytes > zipMaxBytes) {
                throw new RegraDeNegocioException("ZIP_MUITO_GRANDE",
                        "A selecao excede o limite de 2 GB para download em lote.");
            }
        }
    }

    private record ContextoDownloadSelecao(Galeria galeria, SelecaoGaleria selecao,
                                           List<MidiaGaleria> fotos) {
    }

    /**
     * Escreve o ZIP diretamente na resposta, sem carregar a galeria em memoria.
     *
     * <p>PATTERN: Streaming / Pipeline. Cada arquivo e lido do storage por stream
     * e copiado para o ZIP; o backend nao materializa o pacote inteiro.
     */
    @Transactional(readOnly = true)
    public void escreverZip(String token, String versao, OutputStream saida)
            throws IOException {
        List<EntradaZip> entradas = entradasZip(token, versao);
        Set<String> nomes = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(saida)) {
            for (EntradaZip entrada : entradas) {
                zip.putNextEntry(new ZipEntry(nomeUnico(entrada.nome(), nomes)));
                try (InputStream conteudo = arquivoUpload.abrirStream(
                        entrada.arquivoId(), entrada.finalidade(), entrada.galeriaId())) {
                    conteudo.transferTo(zip);
                }
                zip.closeEntry();
            }
            zip.finish();
        }
    }

    private List<EntradaZip> entradasZip(String token, String versao) {
        LinkGaleria link = linkAtivoPublico(token);
        Galeria galeria = obter(link.getGaleriaId());
        if (extrasRepository.existsByGaleriaIdAndStatus(
                galeria.getId(), PedidoExtrasStatus.PENDENTE)) {
            throw new RegraDeNegocioException("EXTRAS_PENDENTES",
                    "Confirme os extras antes de baixar as fotos.");
        }
        if (galeria.getStatus() != GaleriaStatus.EM_EDICAO
                && galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            throw new RegraDeNegocioException("ENTREGA_INDISPONIVEL",
                    "A entrega ainda nao foi liberada.");
        }
        String solicitada = versao == null || versao.isBlank()
                ? "auto" : versao.trim().toLowerCase(Locale.ROOT);
        if (!"auto".equals(solicitada) && !"original".equals(solicitada)
                && !"raw".equals(solicitada) && !"editada".equals(solicitada)) {
            throw new RegraDeNegocioException("VERSAO_INVALIDA",
                    "Informe a versao 'auto', 'editada' ou 'original'.");
        }

        List<EntradaZip> entradas = new ArrayList<>();
        long totalBytes = 0;
        for (MidiaGaleria midia : midiasDaSelecao(galeria.getId())) {
            if (!midia.estaLiberada()) {
                continue;
            }
            boolean original;
            if (midia.getTipo() == MidiaTipo.VIDEO) {
                original = true;
            } else if (midia.prontaNaOrigem()) {
                if ("editada".equals(solicitada)) {
                    continue;
                }
                original = true;
            } else if ("auto".equals(solicitada)) {
                original = !midia.temVersaoEditada();
            } else {
                original = "original".equals(solicitada) || "raw".equals(solicitada);
            }
            if (midia.getTipo() == MidiaTipo.FOTO && !original && !midia.temVersaoEditada()) {
                continue;
            }
            String arquivoId;
            ArquivoFinalidade finalidade;
            if (midia.getTipo() == MidiaTipo.VIDEO) {
                arquivoId = midia.getArquivoOriginalId();
                finalidade = ArquivoFinalidade.VIDEO_ORIGINAL;
            } else if (original) {
                if (!midia.prontaNaOrigem() && !galeria.isRawLiberado()) {
                    throw new RegraDeNegocioException("RAW_NAO_LIBERADO",
                            "Os arquivos originais ainda nao foram liberados.");
                }
                arquivoId = midia.getArquivoOriginalId();
                finalidade = ArquivoFinalidade.FOTO_ORIGINAL;
            } else {
                arquivoId = midia.getArquivoEditadoId();
                finalidade = ArquivoFinalidade.FOTO_EDITADA;
            }
            long tamanho = arquivoUpload.buscar(arquivoId, finalidade, galeria.getId())
                    .map(ArquivoUpload.ArquivoResumo::tamanhoBytes)
                    .orElse(0L);
            totalBytes += tamanho;
            if (totalBytes > zipMaxBytes) {
                throw new RegraDeNegocioException("ZIP_MUITO_GRANDE",
                        "A galeria excede o limite de 2 GB para download em lote.");
            }
            String versaoDownload = midia.getTipo() == MidiaTipo.VIDEO || original
                    ? "ORIGINAL" : "EDITADA";
            entradas.add(new EntradaZip(galeria.getId(), midia.getId(), arquivoId,
                    finalidade, midia.getNomeOriginal(), versaoDownload));
        }
        if (entradas.isEmpty()) {
            throw new RegraDeNegocioException("ZIP_SEM_ARQUIVOS",
                    "Nenhum arquivo disponivel para download em lote.");
        }
        return entradas;
    }

    private static String nomeUnico(String nome, Set<String> usados) {
        String base = nome == null || nome.isBlank()
                ? "arquivo" : nome.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (usados.add(base)) {
            return base;
        }
        int ponto = base.lastIndexOf('.');
        String stem = ponto > 0 ? base.substring(0, ponto) : base;
        String extensao = ponto > 0 ? base.substring(ponto) : "";
        int indice = 2;
        String candidato;
        do {
            candidato = stem + " (" + indice++ + ")" + extensao;
        } while (!usados.add(candidato));
        return candidato;
    }

    private record EntradaZip(String galeriaId, String midiaId, String arquivoId,
                              ArquivoFinalidade finalidade, String nome, String versao) {
    }

    private void processarMidia(MidiaGaleria midia, Galeria galeria) {
        try {
            byte[] original = arquivoUpload.abrirConteudo(
                    midia.getArquivoOriginalId(), ArquivoFinalidade.FOTO_ORIGINAL, galeria.getId());
            ProcessadorMidia.Resultado resultado = processadorMidia.processar(
                    original, midia.getNomeOriginal(), estudioConsulta.nome());
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
                        derivado.largura(), derivado.altura(), "jpg",
                        MidiaDerivadoOrigem.ORIGINAL, derivado.marcada()));
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
        if (!agendamento.entradaQuitada()) {
            throw new RegraDeNegocioException("ENTRADA_PENDENTE",
                    "A entrada do ensaio ainda nao foi confirmada.");
        }
        if (!agendamento.saldoQuitado()) {
            throw new RegraDeNegocioException("SALDO_PENDENTE",
                    "Registre o recebimento integral do saldo final antes de publicar a galeria.");
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


    private Optional<LinkGaleria> linkUnico(String galeriaId) {
        return linkRepository.findByGaleriaIdOrderByExpiraEmDesc(galeriaId).stream()
                .filter(link -> link.getTipo() == LinkGaleriaTipo.GALERIA
                        || link.getTipo() == LinkGaleriaTipo.SELECAO)
                .findFirst();
    }

    private Optional<LinkGaleria> linkUnicoAtivo(String galeriaId) {
        return linkUnico(galeriaId).filter(LinkGaleria::estaAtivo);
    }

    private LinkGaleria linkAtivoPublico(String token) {
        LinkGaleria link = linkRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link de galeria", token));
        if (!link.estaAtivo()) {
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

    private PacoteResumo pacoteDaGaleria(Galeria galeria) {
        return agendamentoConsulta.buscar(galeria.getAgendamentoId())
                .map(AgendamentoConsulta.ResumoAgendamento::pacoteId)
                .flatMap(pacoteConsulta::buscar)
                .orElse(null);
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
        List<MidiaGaleria> selecionadas = midiasDaSelecao(galeria.getId());
        long fotosSelecionadas = selecionadas.stream()
                .filter(midia -> midia.getTipo() == MidiaTipo.FOTO)
                .count();
        long fotosProntas = selecionadas.stream()
                .filter(midia -> midia.getTipo() == MidiaTipo.FOTO)
                .filter(midia -> midia.prontaParaEntrega(galeria.isRawLiberado()))
                .count();
        long fotosLiberadas = selecionadas.stream()
                .filter(midia -> midia.getTipo() == MidiaTipo.FOTO)
                .filter(MidiaGaleria::estaLiberada)
                .count();
        PacoteResumo pacote = pacoteDaGaleria(galeria);
        return GaleriaDtos.GaleriaResponse.de(galeria, totalFotos, totalVideos,
                fotosDisponiveis, videosDisponiveis, fotosSelecionadas, fotosProntas,
                fotosLiberadas, null, galeria.getZipBaixadoEm(),
                pacote == null ? null : pacote.nome(),
                pacote == null ? null : pacote.corHex());
    }

    private GaleriaDtos.MidiaResponse midiaResponse(MidiaGaleria midia, String galeriaId,
                                                     boolean exporOriginal, boolean publico) {
        String thumbOriginal = null;
        String webOriginal = null;
        String thumbEditada = null;
        String webEditada = null;
        String thumbOriginalMarcada = null;
        String webOriginalMarcada = null;
        String thumbEditadaMarcada = null;
        String webEditadaMarcada = null;
        boolean temDerivadoEditado = false;
        if (midia.getTipo() == MidiaTipo.FOTO) {
            for (MidiaDerivado derivado : derivadoRepository.findByMidiaId(midia.getId())) {
                String url = urlOuNull(derivado.getArquivoId(),
                        ArquivoFinalidade.FOTO_DERIVADO, galeriaId);
                boolean editada = derivado.getOrigem() == MidiaDerivadoOrigem.EDITADA;
                if (editada) {
                    temDerivadoEditado = true;
                }
                if ("THUMB".equals(derivado.getTipo())) {
                    if (derivado.isMarcada()) {
                        if (editada) {
                            thumbEditadaMarcada = url;
                        } else {
                            thumbOriginalMarcada = url;
                        }
                    } else if (editada) {
                        thumbEditada = url;
                    } else {
                        thumbOriginal = url;
                    }
                } else if ("WEB".equals(derivado.getTipo())) {
                    if (derivado.isMarcada()) {
                        if (editada) {
                            webEditadaMarcada = url;
                        } else {
                            webOriginalMarcada = url;
                        }
                    } else if (editada) {
                        webEditada = url;
                    } else {
                        webOriginal = url;
                    }
                }
            }
        }

        // A previa publica usa sempre o derivado marcado; o download continua limpo.
        String thumbLimpo = primeiroNaoNulo(thumbEditada, thumbOriginal);
        String webLimpo = primeiroNaoNulo(webEditada, webOriginal);
        String thumbMarcado = primeiroNaoNulo(thumbEditadaMarcada, thumbOriginalMarcada);
        String webMarcado = primeiroNaoNulo(webEditadaMarcada, webOriginalMarcada);
        boolean previaMarcada = publico && thumbMarcado != null && webMarcado != null;
        String thumbUrl = previaMarcada ? thumbMarcado : thumbLimpo;
        String webUrl = previaMarcada ? webMarcado : webLimpo;

        String originalUrl = null;
        if (midia.getTipo() == MidiaTipo.VIDEO || exporOriginal) {
            originalUrl = urlOuNull(midia.getArquivoOriginalId(),
                    midia.getTipo() == MidiaTipo.FOTO
                            ? ArquivoFinalidade.FOTO_ORIGINAL : ArquivoFinalidade.VIDEO_ORIGINAL,
                    galeriaId);
        }
        // O link publico nunca recebe a versao final limpa; o download resolve isso.
        String editadaUrl = null;
        if (!publico && midia.getArquivoEditadoId() != null) {
            editadaUrl = urlOuNull(midia.getArquivoEditadoId(),
                    ArquivoFinalidade.FOTO_EDITADA, galeriaId);
        }
        boolean editadaProcessando = midia.temVersaoEditada() && !temDerivadoEditado;
        return GaleriaDtos.MidiaResponse.de(midia, thumbUrl, webUrl, originalUrl, editadaUrl,
                editadaProcessando, previaMarcada);
    }

    private static String primeiroNaoNulo(String primeiro, String segundo) {
        return primeiro != null ? primeiro : segundo;
    }

    private MidiaFinalidade finalidadeUpload(String valor, MidiaTipo tipo) {
        MidiaFinalidade finalidade = MidiaFinalidade.de(valor);
        if (finalidade == null) {
            throw new RegraDeNegocioException("FINALIDADE_INVALIDA",
                    "Informe a finalidade 'edicao' ou 'pronta'.");
        }
        if (tipo == MidiaTipo.VIDEO && finalidade == MidiaFinalidade.PRONTA) {
            throw new RegraDeNegocioException("FINALIDADE_INVALIDA",
                    "Videos ja sao entregues como original e nao usam a finalidade 'pronta'.");
        }
        return finalidade;
    }

    private MidiaGaleria midiaDaGaleria(String galeriaId, String midiaId) {
        return midiaRepository.findByIdAndGaleriaId(midiaId, galeriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Midia", midiaId));
    }

    private void validarVersaoEditada(Galeria galeria, MidiaGaleria midia) {
        if (midia.getTipo() != MidiaTipo.FOTO) {
            throw new RegraDeNegocioException("EDICAO_APENAS_FOTOS",
                    "Somente fotos podem receber versao editada.");
        }
        if (midia.prontaNaOrigem()) {
            throw new RegraDeNegocioException("MIDIA_JA_FINAL",
                    "Esta foto ja foi publicada como versao final. "
                            + "Remova a midia e envie novamente para substituir.");
        }
        boolean antesDaSelecao = galeria.getStatus() == GaleriaStatus.AGUARDANDO_UPLOAD
                || galeria.getStatus() == GaleriaStatus.EM_SELECAO;
        if (!antesDaSelecao
                && galeria.getStatus() != GaleriaStatus.SELECAO_ENVIADA
                && galeria.getStatus() != GaleriaStatus.EM_EDICAO
                && galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            throw new RegraDeNegocioException("EDICAO_INDISPONIVEL",
                    "Envie versoes editadas antes ou depois da selecao do cliente.");
        }
        if (!antesDaSelecao && !midiaSelecionada(galeria.getId(), midia.getId())) {
            throw new RegraDeNegocioException("MIDIA_NAO_SELECIONADA",
                    "Envie versoes editadas apenas das fotos selecionadas.");
        }
        if (!midia.estaDisponivel()) {
            throw new RegraDeNegocioException("MIDIA_INDISPONIVEL",
                    "A foto original ainda nao esta disponivel.");
        }
    }

    /**
     * Processa a versao editada e recria os derivados tecnicos dela.
     *
     * <p>PATTERN: Pipeline / Strategy. O processamento usa a mesma porta
     * {@link ProcessadorMidia} do original, mas grava os derivados com origem
     * EDITADA para a entrega priorizar a versao final.
     */
    private void processarVersaoEditada(MidiaGaleria midia, Galeria galeria, String arquivoId) {
        try {
            derivadoRepository.deleteByMidiaIdAndOrigem(
                    midia.getId(), MidiaDerivadoOrigem.EDITADA);
            byte[] conteudo = arquivoUpload.abrirConteudo(
                    arquivoId, ArquivoFinalidade.FOTO_EDITADA, galeria.getId());
            ProcessadorMidia.Resultado resultado = processadorMidia.processar(
                    conteudo, midia.getNomeOriginal(), estudioConsulta.nome());
            for (ProcessadorMidia.Derivado derivado : resultado.derivados()) {
                ArquivoUpload.ArquivoResumo arquivo = arquivoUpload.salvarDisponivel(
                        new ArquivoUpload.IniciarArquivo(
                                ArquivoFinalidade.FOTO_DERIVADO,
                                galeria.getId(),
                                "estudios/%s/galerias/%s/editadas/%s/derivados"
                                        .formatted(galeria.getEstudioId(), galeria.getId(),
                                                midia.getId()),
                                derivado.nomeArquivo(),
                                derivado.mimeType()),
                        derivado.conteudo());
                derivadoRepository.save(MidiaDerivado.criar(
                        midia.getId(), derivado.tipo(), arquivo.id(),
                        derivado.largura(), derivado.altura(), "jpg",
                        MidiaDerivadoOrigem.EDITADA, derivado.marcada()));
            }
            midia.vincularEditada(arquivoId, resultado.largura(), resultado.altura());
        } catch (RuntimeException ex) {
            log.warn("Falha ao processar versao editada da midia {}: {}",
                    midia.getId(), ex.getMessage());
            throw new RegraDeNegocioException("EDICAO_INVALIDA",
                    "Nao foi possivel processar a versao editada.");
        }
    }

    private String urlOuNull(String arquivoId, ArquivoFinalidade finalidade, String galeriaId) {
        try {
            return arquivoUpload.urlDownload(arquivoId, finalidade, galeriaId).url();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private int quantidadeCoberta(List<PedidoExtras> pedidos) {
        return pedidos.stream()
                .filter(pedido -> pedido.getStatus() == PedidoExtrasStatus.PAGO
                        || pedido.getStatus() == PedidoExtrasStatus.ISENTO)
                .mapToInt(PedidoExtras::getQuantidadeFotos)
                .sum();
    }

    private GaleriaDtos.ResumoExtrasResponse resumoExtras(Galeria galeria,
                                                          List<PedidoExtras> pedidos) {
        int quantidadeCoberta = quantidadeCoberta(pedidos);
        int quantidadePendente = pedidos.stream()
                .filter(pedido -> pedido.getStatus() == PedidoExtrasStatus.PENDENTE)
                .mapToInt(PedidoExtras::getQuantidadeFotos)
                .sum();
        BigDecimal valorPendente = pedidos.stream()
                .filter(pedido -> pedido.getStatus() == PedidoExtrasStatus.PENDENTE)
                .map(PedidoExtras::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorCoberto = CalculadoraExtras.calcular(
                galeria.getValorFotoExtra(), galeria.getFaixasFotoExtra(),
                quantidadeCoberta).valorTotal();
        int extrasSelecionados = selecaoRepository
                .findFirstByGaleriaIdOrderByEnviadaEmDesc(galeria.getId())
                .map(SelecaoGaleria::getFotosExtras)
                .orElse(0);
        int quantidadeTotal = Math.max(extrasSelecionados,
                quantidadeCoberta + quantidadePendente);
        BigDecimal valorTotal = CalculadoraExtras.calcular(
                galeria.getValorFotoExtra(), galeria.getFaixasFotoExtra(),
                quantidadeTotal).valorTotal();
        return new GaleriaDtos.ResumoExtrasResponse(
                quantidadeTotal,
                valorTotal,
                quantidadeCoberta,
                valorCoberto,
                quantidadePendente,
                valorPendente);
    }

    private List<String> midiasEntregues(Galeria galeria) {
        Set<String> ids = new LinkedHashSet<>();
        for (MidiaGaleria midia : midiaRepository
                .findByGaleriaIdAndStatusNotOrderByOrdemAsc(galeria.getId(), MidiaStatus.REMOVIDA)) {
            if (midia.estaEntregue()) {
                ids.add(midia.getId());
            }
        }
        if (galeria.getSelecaoEntregueId() != null) {
            // Compatibilidade com entregas antigas, anteriores ao entregue_em.
            List<String> legado = selecaoItemRepository
                    .findBySelecaoId(galeria.getSelecaoEntregueId()).stream()
                    .map(SelecaoItem::getMidiaId)
                    .toList();
            if (!legado.isEmpty()) {
                midiaRepository.findByGaleriaIdAndIdInAndStatus(
                                galeria.getId(), legado, MidiaStatus.DISPONIVEL)
                        .forEach(midia -> ids.add(midia.getId()));
            }
        }
        return new ArrayList<>(ids);
    }

    /**
     * Garante que midias ja entregues permanecam na nova selecao.
     *
     * <p>Sem essa trava o cliente poderia trocar fotos ja baixadas por outras
     * mantendo a mesma quantidade de extras e pagando zero pelo novo conjunto.
     */
    private void validarMidiasEntregues(Galeria galeria, List<String> fotos,
                                        List<String> videos) {
        Set<String> fotosEntregues = new HashSet<>();
        Set<String> videosEntregues = new HashSet<>();
        for (MidiaGaleria midia : midiaRepository
                .findByGaleriaIdAndStatusNotOrderByOrdemAsc(galeria.getId(), MidiaStatus.REMOVIDA)) {
            if (!midia.estaEntregue()) {
                continue;
            }
            if (midia.getTipo() == MidiaTipo.FOTO) {
                fotosEntregues.add(midia.getId());
            } else {
                videosEntregues.add(midia.getId());
            }
        }
        if (galeria.getSelecaoEntregueId() != null) {
            // Compatibilidade com entregas antigas, anteriores ao entregue_em.
            List<SelecaoItem> itens = selecaoItemRepository
                    .findBySelecaoId(galeria.getSelecaoEntregueId());
            for (SelecaoItem item : itens) {
                if (MidiaTipo.FOTO.name().equals(item.getTipo())) {
                    fotosEntregues.add(item.getMidiaId());
                } else if (MidiaTipo.VIDEO.name().equals(item.getTipo())) {
                    videosEntregues.add(item.getMidiaId());
                }
            }
        }
        if (!fotos.containsAll(fotosEntregues) || !videos.containsAll(videosEntregues)) {
            throw new RegraDeNegocioException("MIDIA_ENTREGUE_NAO_PODE_SER_REMOVIDA",
                    "As fotos e videos ja entregues continuam obrigatorios na selecao. "
                            + "Para substituir uma midia entregue, fale com o estudio.");
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, ResumoExtras> buscarExtrasPorAgendamentos(Collection<String> agendamentoIds) {
        if (agendamentoIds == null || agendamentoIds.isEmpty()) {
            return Map.of();
        }
        List<Galeria> galerias = galeriaRepository.findByAgendamentoIdIn(agendamentoIds);
        Map<String, List<PedidoExtras>> porGaleria = extrasRepository
                .findByGaleriaIdInOrderByAuditoriaCriadoEmAsc(
                        galerias.stream().map(Galeria::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        PedidoExtras::getGaleriaId,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        Map<String, ResumoExtras> resumos = new LinkedHashMap<>();
        for (Galeria galeria : galerias) {
            List<PedidoExtras> pedidos = porGaleria.getOrDefault(galeria.getId(), List.of());
            if (pedidos.isEmpty()) {
                continue;
            }
            int quantidadeCoberta = 0;
            int quantidadePendente = 0;
            BigDecimal valorPago = BigDecimal.ZERO;
            BigDecimal valorPendente = BigDecimal.ZERO;
            boolean temPago = false;
            boolean temIsento = false;
            for (PedidoExtras pedido : pedidos) {
                switch (pedido.getStatus()) {
                    case PAGO -> {
                        quantidadeCoberta += pedido.getQuantidadeFotos();
                        valorPago = valorPago.add(pedido.getValorTotal());
                        temPago = true;
                    }
                    case ISENTO -> {
                        quantidadeCoberta += pedido.getQuantidadeFotos();
                        temIsento = true;
                    }
                    case PENDENTE -> {
                        quantidadePendente += pedido.getQuantidadeFotos();
                        valorPendente = valorPendente.add(pedido.getValorTotal());
                    }
                    default -> {
                        // Pedidos cancelados nao entram em nenhum total.
                    }
                }
            }
            StatusExtras status = quantidadePendente > 0
                    ? StatusExtras.PENDENTE
                    : temPago ? StatusExtras.PAGO
                    : temIsento ? StatusExtras.ISENTO
                    : StatusExtras.CANCELADO;
            PedidoExtras ultimo = pedidos.get(pedidos.size() - 1);
            resumos.put(galeria.getAgendamentoId(), new ResumoExtras(
                    galeria.getId(),
                    ultimo.getId(),
                    quantidadeCoberta + quantidadePendente,
                    valorPago.add(valorPendente),
                    valorPago,
                    valorPendente,
                    status));
        }
        return resumos;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoOperacional resumoOperacional() {
        return new ResumoOperacional(
                galeriaRepository.countByStatus(GaleriaStatus.AGUARDANDO_UPLOAD),
                galeriaRepository.countByStatus(GaleriaStatus.EM_SELECAO),
                galeriaRepository.countByStatus(GaleriaStatus.SELECAO_ENVIADA),
                galeriaRepository.countByStatus(GaleriaStatus.EM_EDICAO),
                galeriaRepository.countByStatus(GaleriaStatus.ENTREGUE),
                extrasRepository.countByStatus(PedidoExtrasStatus.PENDENTE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumoArmazenamento> galeriasParaArmazenamento() {
        List<Galeria> galerias = galeriaRepository.findAll();
        if (galerias.isEmpty()) {
            return List.of();
        }
        List<String> ids = galerias.stream().map(Galeria::getId).toList();
        Set<String> comExtrasPendentes = new HashSet<>();
        for (PedidoExtras pedido : extrasRepository.findByGaleriaIdInOrderByAuditoriaCriadoEmAsc(ids)) {
            if (pedido.getStatus() == PedidoExtrasStatus.PENDENTE) {
                comExtrasPendentes.add(pedido.getGaleriaId());
            }
        }
        Map<String, Instant> entregues = new LinkedHashMap<>();
        for (EventoGaleria evento : eventoRepository
                .findByGaleriaIdInAndTipoOrderByCriadoEmAsc(ids, EventoGaleriaTipo.ENTREGA_PUBLICADA)) {
            entregues.putIfAbsent(evento.getGaleriaId(), evento.getCriadoEm());
        }

        List<ResumoArmazenamento> resumos = new ArrayList<>(galerias.size());
        for (Galeria galeria : galerias) {
            String bloqueio = motivoBloqueioExpurgo(galeria, comExtrasPendentes.contains(galeria.getId()));
            resumos.add(new ResumoArmazenamento(
                    galeria.getId(),
                    galeria.getClienteNome(),
                    galeria.getStatus().valor(),
                    entregues.get(galeria.getId()),
                    galeria.getZipBaixadoEm(),
                    galeria.getZipConcluidoEm(),
                    galeria.getExpurgoStatus().valor(),
                    galeria.getExpurgoEfetivadoEm(),
                    galeria.getBytesExpurgados(),
                    bloqueio == null,
                    bloqueio));
        }
        return resumos;
    }

    /** Resumo de armazenamento de uma galeria, com a elegibilidade do expurgo. */
    @Transactional(readOnly = true)
    public ResumoArmazenamento resumoArmazenamento(String galeriaId) {
        Galeria galeria = obter(galeriaId);
        boolean extrasPendentes = extrasRepository.existsByGaleriaIdAndStatus(
                galeriaId, PedidoExtrasStatus.PENDENTE);
        Instant entregueEm = eventoRepository
                .findByGaleriaIdInAndTipoOrderByCriadoEmAsc(
                        List.of(galeriaId), EventoGaleriaTipo.ENTREGA_PUBLICADA)
                .stream()
                .findFirst()
                .map(EventoGaleria::getCriadoEm)
                .orElse(null);
        String bloqueio = motivoBloqueioExpurgo(galeria, extrasPendentes);
        return new ResumoArmazenamento(
                galeria.getId(),
                galeria.getClienteNome(),
                galeria.getStatus().valor(),
                entregueEm,
                galeria.getZipBaixadoEm(),
                galeria.getZipConcluidoEm(),
                galeria.getExpurgoStatus().valor(),
                galeria.getExpurgoEfetivadoEm(),
                galeria.getBytesExpurgados(),
                bloqueio == null,
                bloqueio);
    }

    /**
     * Regras de elegibilidade do expurgo.
     *
     * <p>O cliente precisa ter concluido o ZIP: o registro de "baixado" acontece
     * antes do streaming e um download interrompido nao pode liberar a remocao.
     */
    private String motivoBloqueioExpurgo(Galeria galeria, boolean extrasPendentes) {        if (galeria.getStatus() == GaleriaStatus.EXPURGADA) {
            return "Galeria ja expurgada.";
        }
        if (galeria.getStatus() != GaleriaStatus.ENTREGUE) {
            return "A entrega ainda nao foi finalizada.";
        }
        if (galeria.expurgoEmAndamento()) {
            return "Ja existe um expurgo em andamento.";
        }
        if (extrasPendentes) {
            return "Existe pedido de extras pendente.";
        }
        if (galeria.getZipConcluidoEm() == null) {
            return "O cliente ainda nao concluiu o download do ZIP.";
        }
        return null;
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
