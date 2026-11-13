package com.ranny.fotografia.contrato.service;

import com.ranny.fotografia.agenda.AgendamentoConsulta;
import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.arquivo.ArquivoUpload;
import com.ranny.fotografia.contrato.ContratoConsulta;
import com.ranny.fotografia.contrato.api.ContratoRequest;
import com.ranny.fotografia.contrato.api.ContratoResponse;
import com.ranny.fotografia.contrato.api.ComprovanteDownloadResponse;
import com.ranny.fotografia.contrato.api.LinkAceiteRequest;
import com.ranny.fotografia.contrato.api.PagamentoContratoRequests;
import com.ranny.fotografia.contrato.api.SolicitacaoAceiteResponse;
import com.ranny.fotografia.contrato.model.Contrato;
import com.ranny.fotografia.contrato.model.DadosContrato;
import com.ranny.fotografia.contrato.model.ModeloContrato;
import com.ranny.fotografia.contrato.model.SolicitacaoAceite;
import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot;
import com.ranny.fotografia.contrato.model.documento.StatusAceite;
import com.ranny.fotografia.contrato.model.documento.StatusSolicitacaoAceite;
import com.ranny.fotografia.contrato.repository.ContratoRepository;
import com.ranny.fotografia.contrato.repository.ContratoSpecifications;
import com.ranny.fotografia.contrato.repository.SolicitacaoAceiteRepository;
import com.ranny.fotografia.pacote.PacoteConsulta;
import com.ranny.fotografia.shared.api.PaginaResponse;
import com.ranny.fotografia.shared.api.Paginacao;
import com.ranny.fotografia.shared.events.ContratoExcluidoEvent;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.financeiro.CalculadoraPagamento;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PATTERN: Facade + State Machine (delegada ao agregado) + Observer (eventos).
 *
 * <p>O service orquestra os casos de uso do contrato: cria o snapshot, resolve
 * clausulas, numera, aplica transicoes e publica eventos. Nenhuma regra de
 * transicao vive aqui: o agregado {@link Contrato} valida seu proprio estado.
 *
 * <p>O metodo de criacao e {@code synchronized} para serializar a numeracao
 * dentro de uma instancia (o UNIQUE no banco e a segunda barreira). Em cluster,
 * a evolucao prevista e usar uma sequence/contador transacional.
 */
@Service
public class ContratoService implements ContratoConsulta {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final String FORMA_PAGAMENTO_PADRAO = "Pix";
    private static final String CONDICOES_PAGAMENTO_PADRAO =
            "Entrada na reserva; saldo no dia do ensaio, via Pix ou dinheiro, registrado pelo estudio.";

    private final ContratoRepository contratoRepository;
    private final SolicitacaoAceiteRepository solicitacaoRepository;
    private final ModeloContratoService modeloService;
    private final EstudioService estudioService;
    private final SnapshotFactory snapshotFactory;
    private final ResolvedorClausulas resolvedorClausulas;
    private final ContratoNumerador numerador;
    private final PacoteConsulta pacoteConsulta;
    private final AgendamentoConsulta agendamentoConsulta;
    private final ApplicationEventPublisher eventos;
    private final ArquivoUpload arquivoUpload;

    public ContratoService(ContratoRepository contratoRepository,
                           SolicitacaoAceiteRepository solicitacaoRepository,
                           ModeloContratoService modeloService,
                           EstudioService estudioService,
                           SnapshotFactory snapshotFactory,
                           ResolvedorClausulas resolvedorClausulas,
                           ContratoNumerador numerador,
                           PacoteConsulta pacoteConsulta,
                           AgendamentoConsulta agendamentoConsulta,
                           ApplicationEventPublisher eventos,
                           ArquivoUpload arquivoUpload) {
        this.contratoRepository = contratoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.modeloService = modeloService;
        this.estudioService = estudioService;
        this.snapshotFactory = snapshotFactory;
        this.resolvedorClausulas = resolvedorClausulas;
        this.numerador = numerador;
        this.pacoteConsulta = pacoteConsulta;
        this.agendamentoConsulta = agendamentoConsulta;
        this.eventos = eventos;
        this.arquivoUpload = arquivoUpload;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ContratoResponse> listar(StatusAceite status, PagamentoStatus statusEntrada,
                                                   PagamentoStatus statusSaldoFinal,
                                                   String clienteId, String agendamentoId, String busca,
                                                   Integer pagina, Integer tamanho) {
        Specification<Contrato> spec = (root, query, cb) -> cb.conjunction();
        Specification<Contrato> filtroStatus = ContratoSpecifications.comStatus(status);
        if (filtroStatus != null) {
            spec = spec.and(filtroStatus);
        }
        Specification<Contrato> filtroEntrada = ContratoSpecifications.comStatusEntrada(statusEntrada);
        if (filtroEntrada != null) {
            spec = spec.and(filtroEntrada);
        }
        Specification<Contrato> filtroSaldoFinal =
                ContratoSpecifications.comStatusSaldoFinal(statusSaldoFinal);
        if (filtroSaldoFinal != null) {
            spec = spec.and(filtroSaldoFinal);
        }
        Specification<Contrato> filtroCliente = ContratoSpecifications.doCliente(clienteId);
        if (filtroCliente != null) {
            spec = spec.and(filtroCliente);
        }
        Specification<Contrato> filtroAgendamento = ContratoSpecifications.doAgendamento(agendamentoId);
        if (filtroAgendamento != null) {
            spec = spec.and(filtroAgendamento);
        }
        Specification<Contrato> filtroBusca = ContratoSpecifications.busca(busca);
        if (filtroBusca != null) {
            spec = spec.and(filtroBusca);
        }
        var pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.DESC, "dataEmissao"));
        return PaginaResponse.de(contratoRepository.findAll(spec, pageable), ContratoResponse::de);
    }

    @Transactional(readOnly = true)
    public ContratoResponse buscarPorId(String id) {
        return ContratoResponse.de(obter(id));
    }

    @Transactional
    public synchronized ContratoResponse criar(ContratoRequest request) {
        String numero = numerador.proximoNumero(anoAtual());
        DadosContrato dados = montarDados(request, numero, null);
        Contrato contrato = Contrato.criar(dados);
        contratoRepository.save(contrato);
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional
    public ContratoResponse atualizar(String id, ContratoRequest request) {
        Contrato contrato = obter(id);
        if (contrato.getStatusAceite() != StatusAceite.RASCUNHO) {
            throw new RegraDeNegocioException("CONTRATO_NAO_EDITAVEL",
                    "Somente contratos em rascunho podem ser alterados.");
        }
        DadosContrato dados = montarDados(request, contrato.getNumero(), contrato.getDataEmissao());
        contrato.atualizar(dados);
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional
    public ContratoResponse duplicar(String id) {
        Contrato original = obter(id);
        Contrato duplicado = original.duplicar(numerador.proximoNumero(anoAtual()));
        contratoRepository.save(duplicado);
        publicar(duplicado);
        return ContratoResponse.de(duplicado);
    }

    @Transactional
    public void excluir(String id) {
        Contrato contrato = obter(id);
        contrato.validarExclusao();
        String agendamentoId = contrato.getAgendamentoId();
        contratoRepository.delete(contrato);
        eventos.publishEvent(new ContratoExcluidoEvent(contrato.getId(), agendamentoId));
    }

    // ------------------------------------------------------------------
    // Aceite
    // ------------------------------------------------------------------

    @Transactional
    public SolicitacaoAceiteResponse gerarLink(String contratoId, LinkAceiteRequest request) {
        Contrato contrato = obter(contratoId);
        int dias = request == null || request.expiresInDays() == null
                ? SolicitacaoAceite.VALIDADE_PADRAO_DIAS
                : request.expiresInDays();

        cancelarLinksAbertos(contrato.getId());

        SolicitacaoAceite solicitacao = SolicitacaoAceite.criar(contrato.getId(), dias);
        solicitacaoRepository.save(solicitacao);
        contrato.registrarLink(solicitacao);
        publicar(contrato);
        return SolicitacaoAceiteResponse.de(solicitacao);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoAceiteResponse> listarLinks(String contratoId) {
        obter(contratoId);
        return solicitacaoRepository.findByContratoIdOrderByCriadaEmDesc(contratoId)
                .stream()
                .map(SolicitacaoAceiteResponse::de)
                .toList();
    }

    @Transactional
    public void cancelarLink(String contratoId, String linkId) {
        SolicitacaoAceite solicitacao = solicitacaoRepository.findById(linkId)
                .filter(item -> item.getContratoId().equals(contratoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link de aceite", linkId));
        solicitacao.cancelar();

        Contrato contrato = obter(contratoId);
        if (linkId.equals(contrato.getSolicitacaoAtualId())) {
            contrato.cancelarLink();
            publicar(contrato);
        }
    }

    @Transactional
    public ContratoResponse reabrir(String id) {
        Contrato contrato = obter(id);
        contrato.reabrir();
        cancelarLinksAbertos(contrato.getId());
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional
    public ContratoResponse cancelar(String id) {
        Contrato contrato = obter(id);
        contrato.cancelar();
        cancelarLinksAbertos(contrato.getId());
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    // ------------------------------------------------------------------
    // Pagamentos
    // ------------------------------------------------------------------

    @Transactional
    public ContratoResponse confirmarPagamento(String id, PagamentoContratoRequests.ConfirmacaoRequest request) {
        Contrato contrato = obter(id);
        String por = request == null || request.confirmadoPor() == null || request.confirmadoPor().isBlank()
                ? "Equipe do estudio"
                : request.confirmadoPor();
        contrato.confirmarPagamento(Instant.now(), por);
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional
    public ContratoResponse rejeitarComprovante(String id, PagamentoContratoRequests.RejeicaoRequest request) {
        Contrato contrato = obter(id);
        contrato.rejeitarComprovante(request.motivo(), Instant.now());
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional
    public ContratoResponse isentarEntrada(String id) {
        Contrato contrato = obter(id);
        contrato.isentarEntrada();
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional
    public ContratoResponse registrarPagamentoFinal(String id,
                                                    PagamentoContratoRequests.PagamentoFinalRequest request) {
        Contrato contrato = obter(id);
        validarSaldoFinal(contrato);
        BigDecimal valor = request.valor() == null ? contrato.getSaldoRestante() : request.valor();
        contrato.registrarPagamentoFinal(
                valor,
                request.formaPagamento() == null ? FormaPagamentoSaldo.OUTRO : request.formaPagamento(),
                request.recebidoEm() == null ? Instant.now() : request.recebidoEm(),
                request.recebidoPor() == null ? "Equipe do estudio" : request.recebidoPor(),
                request.observacoes());
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional
    public ContratoResponse isentarPagamentoFinal(String id) {
        Contrato contrato = obter(id);
        validarSaldoFinal(contrato);
        contrato.isentarPagamentoFinal();
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional
    public ContratoResponse reabrirPagamentoFinal(String id) {
        Contrato contrato = obter(id);
        contrato.reabrirPagamentoFinal();
        publicar(contrato);
        return ContratoResponse.de(contrato);
    }

    @Transactional(readOnly = true)
    public ComprovanteDownloadResponse urlComprovante(String id) {
        Contrato contrato = obter(id);
        String arquivoId = contrato.getPagamento() == null
                ? null
                : contrato.getPagamento().getArquivoId();
        if (arquivoId == null || arquivoId.isBlank()) {
            throw new RecursoNaoEncontradoException("Comprovante", id);
        }
        ArquivoUpload.UrlDownload download = arquivoUpload.urlDownload(
                arquivoId, ArquivoFinalidade.COMPROVANTE_PAGAMENTO, contrato.getId());
        return new ComprovanteDownloadResponse(download.url(), download.expiraEm());
    }

    // ------------------------------------------------------------------
    // Montagem
    // ------------------------------------------------------------------

    private DadosContrato montarDados(ContratoRequest request, String numero, Instant dataEmissao) {
        ModeloContrato modelo = modeloService.obter(request.modeloId());
        Instant agora = Instant.now();

        String clienteId = request.clienteId() != null && !request.clienteId().isBlank()
                ? request.clienteId()
                : agendamentoConsulta.buscar(request.agendamentoId())
                        .map(agendamento -> agendamento.clienteId())
                        .orElse(null);

        String pacoteId = request.pacoteId() != null && !request.pacoteId().isBlank()
                ? request.pacoteId()
                : agendamentoConsulta.buscar(request.agendamentoId())
                        .map(agendamento -> agendamento.pacoteId())
                        .orElse(null);

        ContratoSnapshot snapshot = snapshotFactory.montar(
                request.snapshot(), clienteId, request.agendamentoId(), pacoteId, modelo.getVersao());

        BigDecimal valorTotal = request.valorTotal() != null
                ? CalculadoraPagamento.normalizar(request.valorTotal())
                : resolverValorTotal(request.agendamentoId(), pacoteId, agora);

        BigDecimal percentual;
        BigDecimal entrada;
        if (request.valorEntrada() != null) {
            entrada = CalculadoraPagamento.normalizar(request.valorEntrada());
            percentual = request.percentualEntrada() != null
                    ? CalculadoraPagamento.normalizar(request.percentualEntrada())
                    : CalculadoraPagamento.percentualEntrada(valorTotal, entrada);
        } else {
            percentual = request.percentualEntrada() != null
                    ? CalculadoraPagamento.normalizar(request.percentualEntrada())
                    : estudioService.obter().getPercentualEntradaPadrao();
            entrada = CalculadoraPagamento.valorEntrada(valorTotal, percentual);
        }

        List<ContratoClausula> clausulasModelo = modelo.getClausulas();
        List<ContratoClausula> fonte = request.clausulas() == null || request.clausulas().isEmpty()
                ? clausulasModelo
                : request.clausulas();

        ResolvedorClausulas.DadosResolucao dadosResolucao = new ResolvedorClausulas.DadosResolucao(
                numero,
                dataEmissao == null ? agora : dataEmissao,
                valorTotal,
                entrada,
                percentual,
                CalculadoraPagamento.saldoRestante(valorTotal, entrada),
                request.formaPagamento() == null ? FORMA_PAGAMENTO_PADRAO : request.formaPagamento());
        List<ContratoClausula> clausulas = resolvedorClausulas.resolver(fonte, snapshot, dadosResolucao);

        return new DadosContrato(
                null,
                numero,
                clienteId,
                request.agendamentoId(),
                pacoteId,
                modelo.getId(),
                request.titulo() == null || request.titulo().isBlank() ? modelo.getNome() : request.titulo(),
                clausulas,
                clausulasModelo,
                snapshot,
                valorTotal,
                entrada,
                percentual,
                request.formaPagamento() == null ? FORMA_PAGAMENTO_PADRAO : request.formaPagamento(),
                request.condicoesPagamento() == null ? CONDICOES_PAGAMENTO_PADRAO : request.condicoesPagamento(),
                dataEmissao == null ? agora : dataEmissao,
                request.dataValidade(),
                request.observacoes());
    }

    private BigDecimal resolverValorTotal(String agendamentoId, String pacoteId, Instant agora) {
        return agendamentoConsulta.buscar(agendamentoId)
                .map(agendamento -> agendamento.valorAcordado())
                .or(() -> pacoteConsulta.buscar(pacoteId).map(pacote -> pacote.valorEfetivo(agora)))
                .orElseThrow(() -> new RegraDeNegocioException("VALOR_TOTAL_OBRIGATORIO",
                        "Informe o valor total ou vincule um agendamento/pacote."));
    }

    private void cancelarLinksAbertos(String contratoId) {
        solicitacaoRepository.findByContratoIdAndStatusIn(contratoId,
                        List.of(StatusSolicitacaoAceite.PENDENTE, StatusSolicitacaoAceite.VISUALIZADA))
                .forEach(SolicitacaoAceite::cancelar);
    }

    private void publicar(Contrato contrato) {
        eventos.publishEvent(ContratoEventoFactory.de(contrato));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResumoFinanceiro> buscarPorAgendamento(String agendamentoId) {
        if (agendamentoId == null || agendamentoId.isBlank()) {
            return Optional.empty();
        }
        return contratoRepository.findFirstByAgendamentoIdOrderByDataEmissaoDesc(agendamentoId)
                .map(ContratoService::paraResumoFinanceiro);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, ResumoFinanceiro> buscarPorAgendamentos(Collection<String> agendamentoIds) {
        if (agendamentoIds == null || agendamentoIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ResumoFinanceiro> resumos = new LinkedHashMap<>();
        contratoRepository.findByAgendamentoIdInOrderByDataEmissaoDesc(agendamentoIds)
                .forEach(contrato -> {
                    if (contrato.getAgendamentoId() != null) {
                        resumos.putIfAbsent(contrato.getAgendamentoId(),
                                paraResumoFinanceiro(contrato));
                    }
                });
        return resumos;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoPendentes resumoPendentes() {
        return new ResumoPendentes(
                contratoRepository.countByStatusAceite(StatusAceite.PENDENTE),
                contratoRepository.countByStatusAceiteNotAndStatusPagamento(
                        StatusAceite.CANCELADO, PagamentoStatus.PENDENTE),
                contratoRepository.countByStatusAceiteNotAndStatusPagamentoFinal(
                        StatusAceite.CANCELADO, PagamentoStatus.PENDENTE));
    }

    private static ResumoFinanceiro paraResumoFinanceiro(Contrato contrato) {
        return new ResumoFinanceiro(
                contrato.getId(),
                contrato.getAgendamentoId(),
                contrato.getValorTotal(),
                contrato.getValorEntrada(),
                contrato.getPercentualEntrada(),
                contrato.getSaldoRestante(),
                contrato.getStatusPagamento(),
                contrato.getStatusPagamentoFinal());
    }

    private void validarSaldoFinal(Contrato contrato) {
        if (contrato.getStatusAceite() == StatusAceite.CANCELADO
                || contrato.getStatusPagamento() == PagamentoStatus.CANCELADO) {
            throw new RegraDeNegocioException("ATENDIMENTO_CANCELADO",
                    "Contrato cancelado nao aceita registro de saldo final.");
        }
        if (contrato.getPagamento() == null
                || !contrato.getPagamento().getStatus().estaQuitado()) {
            throw new RegraDeNegocioException("ENTRADA_PENDENTE",
                    "Confirme a entrada antes de registrar o saldo final.");
        }
    }

    private Contrato obter(String id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato", id));
    }

    private static int anoAtual() {
        return LocalDate.now(FUSO).getYear();
    }
}
