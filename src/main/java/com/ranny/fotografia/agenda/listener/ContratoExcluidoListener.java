package com.ranny.fotografia.agenda.listener;

import com.ranny.fotografia.agenda.repository.AgendamentoRepository;
import com.ranny.fotografia.shared.events.ContratoExcluidoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * PATTERN: Observer.
 *
 * <p>Limpa o vínculo do agendamento quando o contrato correspondente é
 * excluído, permitindo que o ensaio volte a ser editável/excluível.
 */
@Component
public class ContratoExcluidoListener {

    private static final Logger log = LoggerFactory.getLogger(ContratoExcluidoListener.class);

    private final AgendamentoRepository agendamentoRepository;

    public ContratoExcluidoListener(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @ApplicationModuleListener
    public void aoExcluirContrato(ContratoExcluidoEvent evento) {
        if (evento.agendamentoId() == null || evento.agendamentoId().isBlank()) {
            return;
        }
        agendamentoRepository.findById(evento.agendamentoId()).ifPresent(agendamento -> {
            if (evento.contratoId().equals(agendamento.getContratoId())) {
                agendamento.desvincularContrato();
                log.debug("Agendamento {} desvinculado do contrato excluído {}",
                        agendamento.getId(), evento.contratoId());
            }
        });
    }
}
