package com.ranny.fotografia.agenda.listener;

import com.ranny.fotografia.agenda.model.StatusAgendamento;
import com.ranny.fotografia.agenda.repository.AgendamentoRepository;
import com.ranny.fotografia.agenda.repository.BloqueioAgendaRepository;
import com.ranny.fotografia.shared.events.ContratoStatusAlteradoEvent;
import com.ranny.fotografia.shared.model.PagamentoEntrada;
import com.ranny.fotografia.shared.model.PagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * PATTERN: Observer / Publish-Subscribe com {@code @ApplicationModuleListener}.
 *
 * <p>O listener roda após o commit do contrato (evento durável), em transação
 * própria. Ele traduz o estado comercial do contrato para o agendamento:
 *
 * <ul>
 *   <li>contrato cancelado → agendamento cancelado;</li>
 *   <li>contrato aceito e entrada quitada → agendamento confirmado;</li>
 *   <li>demais estados → agendamento reservado;</li>
 *   <li>ensaio já realizado não tem status reescrito.</li>
 * </ul>
 */
@Component
public class ContratoStatusAlteradoListener {

    private static final Logger log = LoggerFactory.getLogger(ContratoStatusAlteradoListener.class);
    private static final ZoneId FUSO_ESTUDIO = ZoneId.of("America/Sao_Paulo");

    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;

    public ContratoStatusAlteradoListener(AgendamentoRepository agendamentoRepository,
                                          BloqueioAgendaRepository bloqueioAgendaRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.bloqueioAgendaRepository = bloqueioAgendaRepository;
    }

    @ApplicationModuleListener
    public void aoAlterarContrato(ContratoStatusAlteradoEvent evento) {
        if (evento.agendamentoId() == null || evento.agendamentoId().isBlank()) {
            return;
        }

        PagamentoEntrada entrada = PagamentoEntrada.deEvento(
                evento.entradaValor(), PagamentoStatus.de(evento.entradaStatus()));
        PagamentoSaldo saldo = PagamentoSaldo.deEvento(
                evento.saldoValor(), PagamentoStatus.de(evento.saldoStatus()));

        agendamentoRepository.findById(evento.agendamentoId()).ifPresent(agendamento -> {
            if (agendamento.getContratoId() == null) {
                agendamento.vincularContrato(evento.contratoId());
            }
            agendamento.vincularCliente(evento.clienteId());
            StatusAgendamento status = statusParaAgenda(evento);
            if (agendamento.getStatus() == StatusAgendamento.CANCELADO
                    && status != StatusAgendamento.CANCELADO
                    && bloqueioAgendaRepository.existsByInicioLessThanAndFimGreaterThan(
                            agendamento.getDataHoraFim(), agendamento.getDataHoraInicio())) {
                log.warn("Agendamento {} nao reativado: horario bloqueado", agendamento.getId());
                status = StatusAgendamento.CANCELADO;
            }
            agendamento.sincronizarComContrato(status, entrada, saldo);
            agendamento.promoverParaRealizado(LocalDate.now(FUSO_ESTUDIO));
            log.debug("Agendamento {} sincronizado com contrato {} (status={})",
                    agendamento.getId(), evento.contratoId(), agendamento.getStatus());
        });
    }

    static StatusAgendamento statusParaAgenda(ContratoStatusAlteradoEvent evento) {
        if ("cancelado".equalsIgnoreCase(evento.statusAceite())
                || "cancelado".equalsIgnoreCase(evento.statusPagamento())) {
            return StatusAgendamento.CANCELADO;
        }
        boolean aceito = "aceito".equalsIgnoreCase(evento.statusAceite());
        boolean entradaQuitada = "confirmado".equalsIgnoreCase(evento.statusPagamento())
                || "isento".equalsIgnoreCase(evento.statusPagamento());
        if (aceito && entradaQuitada) {
            return StatusAgendamento.CONFIRMADO;
        }
        return StatusAgendamento.RESERVADO;
    }
}
