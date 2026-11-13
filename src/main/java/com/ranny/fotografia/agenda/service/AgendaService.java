package com.ranny.fotografia.agenda.service;

import com.ranny.fotografia.agenda.AgendamentoConsulta;
import com.ranny.fotografia.agenda.AgendamentoConsulta.ResumoAgendamento;
import com.ranny.fotografia.agenda.api.AgendaRequests;
import com.ranny.fotografia.agenda.api.AgendamentoRequest;
import com.ranny.fotografia.agenda.api.AgendamentoResponse;
import com.ranny.fotografia.agenda.model.Agendamento;
import com.ranny.fotografia.agenda.model.CondicoesPacote;
import com.ranny.fotografia.agenda.model.ContatoReserva;
import com.ranny.fotografia.agenda.model.PromocaoAplicada;
import com.ranny.fotografia.agenda.model.StatusAgendamento;
import com.ranny.fotografia.agenda.repository.AgendamentoRepository;
import com.ranny.fotografia.cliente.ClienteConsulta;
import com.ranny.fotografia.cliente.ClienteResumo;
import com.ranny.fotografia.pacote.PacoteConsulta;
import com.ranny.fotografia.pacote.PacoteResumo;
import com.ranny.fotografia.shared.api.PaginaResponse;
import com.ranny.fotografia.shared.api.Paginacao;
import com.ranny.fotografia.shared.exception.RequisicaoInvalidaException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.financeiro.CalculadoraPagamento;
import com.ranny.fotografia.shared.financeiro.ResumoPagamento;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoEntrada;
import com.ranny.fotografia.shared.model.PagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * PATTERN: Facade + Application Service (DDD).
 *
 * <p>Orquestra repositorio, portas de cliente/pacote e dominio. Nao contem
 * detalhes HTTP e nao expoe entidades: toda saida e DTO. E o unico ponto que
 * conhece a ordem das operacoes de criacao/edicao de agendamento.
 *
 * <p>A comunicacao com contrato NAO acontece aqui: o contrato publica eventos e
 * um listener deste modulo reage. Assim nao ha dependencia ciclica.
 */
@Service
public class AgendaService implements AgendamentoConsulta {

    private static final BigDecimal PERCENTUAL_ENTRADA_PADRAO = BigDecimal.valueOf(30);
    private static final ZoneId FUSO_ESTUDIO = ZoneId.of("America/Sao_Paulo");

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteConsulta clienteConsulta;
    private final PacoteConsulta pacoteConsulta;

    public AgendaService(AgendamentoRepository agendamentoRepository,
                         ClienteConsulta clienteConsulta,
                         PacoteConsulta pacoteConsulta) {
        this.agendamentoRepository = agendamentoRepository;
        this.clienteConsulta = clienteConsulta;
        this.pacoteConsulta = pacoteConsulta;
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(OffsetDateTime inicio, OffsetDateTime fim,
                                            StatusAgendamento status) {
        if ((inicio == null) != (fim == null)) {
            throw new RequisicaoInvalidaException("PERIODO_INCOMPLETO",
                    "Informe inicio e fim juntos para filtrar por periodo.");
        }
        if (inicio == null) {
            throw new RequisicaoInvalidaException("PERIODO_OBRIGATORIO",
                    "O calendario exige inicio e fim do periodo.");
        }
        List<Agendamento> agendamentos;
        if (status != null) {
            agendamentos = agendamentoRepository
                    .findByDataHoraInicioBetweenAndStatusOrderByDataHoraInicioAsc(inicio, fim, status);
        } else {
            agendamentos = agendamentoRepository.findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(inicio, fim);
        }
        return agendamentos.stream().map(AgendamentoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AgendamentoResponse> listarPaginado(
            OffsetDateTime inicio, OffsetDateTime fim, StatusAgendamento status,
            Integer pagina, Integer tamanho) {
        if ((inicio == null) != (fim == null)) {
            throw new RequisicaoInvalidaException("PERIODO_INCOMPLETO",
                    "Informe inicio e fim juntos para filtrar por periodo.");
        }
        var pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.ASC, "dataHoraInicio"));
        Page<Agendamento> agendamentos;
        if (inicio != null && status != null) {
            agendamentos = agendamentoRepository
                    .findByDataHoraInicioBetweenAndStatusOrderByDataHoraInicioAsc(inicio, fim, status, pageable);
        } else if (inicio != null) {
            agendamentos = agendamentoRepository
                    .findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(inicio, fim, pageable);
        } else if (status != null) {
            agendamentos = agendamentoRepository.findByStatusOrderByDataHoraInicioAsc(status, pageable);
        } else {
            agendamentos = agendamentoRepository.findAll(pageable);
        }
        return PaginaResponse.de(agendamentos, AgendamentoResponse::de);
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscarPorId(String id) {
        return AgendamentoResponse.de(obter(id));
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        PacoteResumo pacote = obterPacote(request.pacoteId());
        Instant agora = Instant.now();

        ClienteResumo cliente = resolverCliente(request.clienteId());
        ContatoReserva contato = resolverContato(cliente, request.contato());

        BigDecimal valorAcordado = request.valorAcordado() != null
                ? CalculadoraPagamento.normalizar(request.valorAcordado())
                : pacote.valorEfetivo(agora);
        BigDecimal percentual = request.percentualEntrada() != null
                ? CalculadoraPagamento.normalizar(request.percentualEntrada())
                : PERCENTUAL_ENTRADA_PADRAO;
        BigDecimal entrada = request.valorEntrada() != null
                ? CalculadoraPagamento.normalizar(request.valorEntrada())
                : CalculadoraPagamento.valorEntrada(valorAcordado, percentual);
        if (request.valorEntrada() != null && request.percentualEntrada() == null) {
            percentual = CalculadoraPagamento.percentualEntrada(valorAcordado, entrada);
        }

        OffsetDateTime inicio = request.dataHoraInicio();
        OffsetDateTime fim = request.dataHoraFim() != null
                ? request.dataHoraFim()
                : inicio.plusMinutes(pacote.duracaoMinutos());

        Agendamento agendamento = Agendamento.criar(
                cliente == null ? null : cliente.id(),
                contato,
                pacote.id(),
                inicio,
                fim,
                request.local().trim(),
                request.status() == null ? StatusAgendamento.RESERVADO : request.status(),
                normalizar(request.observacoes()),
                valorAcordado,
                entrada,
                percentual,
                montarCondicoes(pacote, agora, request.aplicarPromocao()));

        return AgendamentoResponse.de(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoResponse atualizar(String id, AgendamentoRequest request) {
        Agendamento agendamento = obter(id);
        PacoteResumo pacote = obterPacote(request.pacoteId());
        Instant agora = Instant.now();

        ClienteResumo cliente = resolverCliente(request.clienteId());
        ContatoReserva contato = resolverContato(cliente, request.contato());

        BigDecimal valorAcordado = request.valorAcordado() != null
                ? CalculadoraPagamento.normalizar(request.valorAcordado())
                : agendamento.getValorAcordado();

        BigDecimal percentual;
        BigDecimal entrada;
        if (request.valorEntrada() != null) {
            entrada = CalculadoraPagamento.normalizar(request.valorEntrada());
            percentual = request.percentualEntrada() != null
                    ? CalculadoraPagamento.normalizar(request.percentualEntrada())
                    : CalculadoraPagamento.percentualEntrada(valorAcordado, entrada);
        } else if (request.percentualEntrada() != null) {
            percentual = CalculadoraPagamento.normalizar(request.percentualEntrada());
            entrada = CalculadoraPagamento.valorEntrada(valorAcordado, percentual);
        } else {
            percentual = agendamento.getPercentualEntrada();
            entrada = agendamento.getValorEntrada();
        }

        OffsetDateTime inicio = request.dataHoraInicio();
        OffsetDateTime fim = request.dataHoraFim() != null
                ? request.dataHoraFim()
                : inicio.plusMinutes(pacote.duracaoMinutos());

        StatusAgendamento status = agendamento.getStatus();
        if (request.status() != null) {
            if (!status.podeTransicionarPara(request.status())) {
                throw new RegraDeNegocioException("TRANSICAO_INVALIDA",
                        "Nao e permitido alterar o agendamento de '%s' para '%s'."
                                .formatted(status.getValor(), request.status().getValor()));
            }
            status = request.status();
        }

        agendamento.atualizar(
                cliente == null ? agendamento.getClienteId() : cliente.id(),
                contato,
                pacote.id(),
                inicio,
                fim,
                request.local().trim(),
                status,
                normalizar(request.observacoes()),
                valorAcordado,
                entrada,
                percentual,
                montarCondicoes(pacote, agora, request.aplicarPromocao()));

        return AgendamentoResponse.de(agendamento);
    }

    @Transactional
    public AgendamentoResponse alterarStatus(String id, StatusAgendamento status) {
        Agendamento agendamento = obter(id);
        agendamento.alterarStatus(status);
        return AgendamentoResponse.de(agendamento);
    }

    @Transactional
    public AgendamentoResponse atualizarPagamentoEntrada(String id,
                                                         AgendaRequests.PagamentoEntradaRequest request) {
        Agendamento agendamento = obter(id);
        PagamentoEntrada pagamento = agendamento.getPagamentoEntrada();
        if (pagamento == null) {
            pagamento = PagamentoEntrada.pendente(agendamento.getValorEntrada(), agendamento.getPercentualEntrada());
        }

        BigDecimal valor = request.valor() != null
                ? CalculadoraPagamento.normalizar(request.valor())
                : pagamento.getValor();
        BigDecimal percentual = request.percentual() != null
                ? CalculadoraPagamento.normalizar(request.percentual())
                : CalculadoraPagamento.percentualEntrada(agendamento.getValorAcordado(), valor);

        pagamento.redefinir(valor, percentual);
        if (request.observacoes() != null) {
            pagamento.observar(request.observacoes());
        }
        aplicarStatusEntrada(pagamento, request.status());

        agendamento.atualizarPagamentoEntrada(valor, percentual, pagamento);
        if (agendamento.entradaQuitada()
                && agendamento.getStatus() == StatusAgendamento.RESERVADO) {
            agendamento.alterarStatus(StatusAgendamento.CONFIRMADO);
        }
        agendamento.promoverParaRealizado(LocalDate.now(FUSO_ESTUDIO));
        return AgendamentoResponse.de(agendamento);
    }

    @Transactional
    public AgendamentoResponse atualizarPagamentoFinal(String id,
                                                       AgendaRequests.PagamentoFinalRequest request) {
        Agendamento agendamento = obter(id);
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraDeNegocioException("ATENDIMENTO_CANCELADO",
                    "Atendimento cancelado nao aceita registro de saldo final.");
        }
        if ((request.status() == PagamentoStatus.CONFIRMADO
                || request.status() == PagamentoStatus.ISENTO)
                && !agendamento.entradaQuitada()) {
            throw new RegraDeNegocioException("ENTRADA_PENDENTE",
                    "Confirme a entrada antes de registrar o saldo final.");
        }
        PagamentoSaldo pagamento = agendamento.getPagamentoFinal();
        if (pagamento == null) {
            pagamento = PagamentoSaldo.pendente(agendamento.getSaldoRestante());
        }

        BigDecimal valor = request.valor() != null
                ? CalculadoraPagamento.normalizar(request.valor())
                : agendamento.getSaldoRestante();

        PagamentoStatus status = request.status();
        if (status == PagamentoStatus.CONFIRMADO
                && valor.compareTo(agendamento.getSaldoRestante()) != 0) {
            throw new RegraDeNegocioException("SALDO_FINAL_INTEGRAL_OBRIGATORIO",
                    "O saldo final deve ser recebido integralmente (previsto: R$ "
                            + agendamento.getSaldoRestante().toPlainString() + ").");
        }

        pagamento.redefinirValor(valor);

        if (status == PagamentoStatus.CONFIRMADO) {
            pagamento.receber(
                    valor,
                    request.formaPagamento() == null ? FormaPagamentoSaldo.OUTRO : request.formaPagamento(),
                    request.recebidoEm() == null ? Instant.now() : request.recebidoEm(),
                    request.recebidoPor() == null ? "Equipe do estudio" : request.recebidoPor(),
                    request.observacoes());
        } else if (status == PagamentoStatus.ISENTO) {
            pagamento.isentar();
        } else if (status == PagamentoStatus.PENDENTE) {
            pagamento.reabrir();
        } else if (status == PagamentoStatus.CANCELADO) {
            pagamento.cancelar();
        } else if (request.observacoes() != null) {
            pagamento.observar(request.observacoes());
        }

        agendamento.atualizarPagamentoFinal(pagamento);
        agendamento.promoverParaRealizado(LocalDate.now(FUSO_ESTUDIO));
        return AgendamentoResponse.de(agendamento);
    }

    /**
     * Promove para REALIZADO os atendimentos confirmados cuja data ja chegou
     * e que tenham entrada e saldo final quitados.
     *
     * <p>Chamado pelo job diario para cobrir pagamentos feitos antecipadamente.
     */
    @Transactional
    public int promoverRealizados() {
        LocalDate hoje = LocalDate.now(FUSO_ESTUDIO);
        int promovidos = 0;
        for (Agendamento agendamento : agendamentoRepository.findByStatus(
                StatusAgendamento.CONFIRMADO)) {
            if (agendamento.promoverParaRealizado(hoje)) {
                promovidos += 1;
            }
        }
        return promovidos;
    }

    @Transactional
    public void excluir(String id) {
        Agendamento agendamento = obter(id);
        agendamento.validarExclusao();
        agendamentoRepository.delete(agendamento);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResumoAgendamento> buscar(String agendamentoId) {
        if (agendamentoId == null || agendamentoId.isBlank()) {
            return Optional.empty();
        }
        return agendamentoRepository.findById(agendamentoId).map(AgendaService::paraResumo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumoAgendamento> listarPorPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {
        return agendamentoRepository
                .findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(inicio, fim)
                .stream()
                .map(AgendaService::paraResumo)
                .toList();
    }

    private Agendamento obter(String id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
    }

    private PacoteResumo obterPacote(String pacoteId) {
        return pacoteConsulta.buscar(pacoteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pacote", pacoteId));
    }

    private ClienteResumo resolverCliente(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            return null;
        }
        return clienteConsulta.buscar(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
    }

    private static ContatoReserva resolverContato(ClienteResumo cliente,
                                                  AgendamentoRequest.ContatoRequest contato) {
        if (cliente != null) {
            return ContatoReserva.de(cliente.nome(), cliente.whatsapp());
        }
        ContatoReserva reserva = contato == null ? null : ContatoReserva.de(contato.nome(), contato.whatsapp());
        if (reserva == null) {
            throw new RegraDeNegocioException("CONTATO_OBRIGATORIO",
                    "Informe um cliente cadastrado ou ao menos nome/WhatsApp de contato.");
        }
        return reserva;
    }

    private static CondicoesPacote montarCondicoes(PacoteResumo pacote, Instant referencia,
                                                   Boolean aplicarPromocao) {
        boolean aplicar = aplicarPromocao == null || aplicarPromocao;
        PromocaoAplicada promocao = aplicar
                ? pacote.promocaoAplicada(referencia)
                        .map(p -> PromocaoAplicada.de(p.valorPromocional(), p.rotulo()))
                        .orElse(null)
                : null;
        return CondicoesPacote.de(
                pacote.quantidadeFotosInclusas(),
                pacote.quantidadeVideosInclusos(),
                pacote.valorFotoExtra(),
                pacote.valor(),
                pacote.faixasFotoExtra(),
                promocao);
    }

    private static void aplicarStatusEntrada(PagamentoEntrada pagamento, PagamentoStatus status) {
        if (status == null) {
            return;
        }
        switch (status) {
            case CONFIRMADO -> pagamento.confirmar(Instant.now(), "Equipe do estudio");
            case ISENTO -> pagamento.isentar();
            case PENDENTE -> pagamento.reabrirAnalise();
            case CANCELADO -> pagamento.cancelar();
        }
    }

    private static ResumoAgendamento paraResumo(Agendamento agendamento) {
        ContatoReserva contato = agendamento.getContato();
        CondicoesPacote condicoes = agendamento.getCondicoesPacote();
        var resumoPagamento = ResumoPagamento.de(
                agendamento.getPagamentoEntrada(),
                agendamento.getPagamentoFinal(),
                agendamento.getValorEntrada(),
                agendamento.getSaldoRestante());
        return new ResumoAgendamento(
                agendamento.getId(),
                agendamento.getClienteId(),
                agendamento.getPacoteId(),
                agendamento.getContratoId(),
                contato == null ? null : new ResumoAgendamento.ContatoResumo(contato.getNome(), contato.getWhatsapp()),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getLocal(),
                agendamento.getStatus(),
                agendamento.getObservacoes(),
                agendamento.getValorAcordado(),
                agendamento.getValorEntrada(),
                agendamento.getPercentualEntrada(),
                agendamento.getSaldoRestante(),
                agendamento.getPagamentoEntrada() == null ? null : agendamento.getPagamentoEntrada().getStatus(),
                agendamento.getPagamentoFinal() == null ? null : agendamento.getPagamentoFinal().getStatus(),
                resumoPagamento.entradaQuitada(),
                resumoPagamento.saldoQuitado(),
                resumoPagamento.quitado(),
                resumoPagamento.valorRecebido(),
                resumoPagamento.valorPendente(),
                condicoes == null ? null : new ResumoAgendamento.CondicoesResumo(
                        condicoes.getQuantidadeFotosInclusas(),
                        condicoes.getQuantidadeVideosInclusos(),
                        condicoes.getValorFotoExtra(),
                        condicoes.getValorTabela(),
                        condicoes.getFaixasFotoExtra(),
                        condicoes.getPromocaoAplicada() == null ? null : condicoes.getPromocaoAplicada().getValorPromocional(),
                        condicoes.getPromocaoAplicada() == null ? null : condicoes.getPromocaoAplicada().getRotulo()));
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
