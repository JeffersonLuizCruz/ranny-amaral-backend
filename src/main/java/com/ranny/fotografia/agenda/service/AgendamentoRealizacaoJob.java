package com.ranny.fotografia.agenda.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job diario que promove para REALIZADO os atendimentos cuja data chegou e que
 * ja tenham entrada e saldo final quitados.
 *
 * <p>Cobre o caso de pagamentos registrados antecipadamente: o status permanece
 * CONFIRMADO ate a data do ensaio.
 */
@Component
public class AgendamentoRealizacaoJob {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoRealizacaoJob.class);

    private final AgendaService agendaService;

    public AgendamentoRealizacaoJob(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @Scheduled(cron = "${app.agenda.realizacao-cron:0 5 0 * * *}",
            zone = "America/Sao_Paulo")
    public void executar() {
        int promovidos = agendaService.promoverRealizados();
        if (promovidos > 0) {
            log.info("{} atendimento(s) promovido(s) para REALIZADO.", promovidos);
        }
    }
}
