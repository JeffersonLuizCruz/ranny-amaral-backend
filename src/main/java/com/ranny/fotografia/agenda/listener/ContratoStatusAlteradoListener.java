package com.ranny.fotografia.agenda.listener;

import com.ranny.fotografia.agenda.model.StatusAgendamento;
import com.ranny.fotografia.agenda.repository.AgendamentoRepository;
import com.ranny.fotografia.shared.events.ContratoStatusAlteradoEvent;
import com.ranny.fotografia.shared.model.PagamentoEntrada;
import com.ranny.fotografia.shared.model.PagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

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

    private final AgendamentoRepository agendamentoRepository;

    public ContratoStatusAlteradoListener(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
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
            StatusAgendamento status = statusParaAgenda(evento);
            agendamento.sincronizarComContrato(status, entrada, saldo);
            log.debug("Agendamento {} sincronizado com contrato {} (status={})",
                    agendamento.getId(), evento.contratoId(), status);
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
