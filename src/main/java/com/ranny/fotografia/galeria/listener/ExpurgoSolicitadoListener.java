package com.ranny.fotografia.galeria.listener;

import com.ranny.fotografia.galeria.event.ExpurgoSolicitadoEvent;
import com.ranny.fotografia.galeria.service.ExpurgoGaleriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Executa o expurgo fora da request.
 *
 * <p>Apagar milhares de objetos pode levar segundos; a request apenas agenda e
 * responde. A falha fica registrada no log e a galeria continua {@code AGENDADO}
 * para nova tentativa.
 */
@Component
public class ExpurgoSolicitadoListener {

    private static final Logger log = LoggerFactory.getLogger(ExpurgoSolicitadoListener.class);

    private final ExpurgoGaleriaService expurgoService;

    public ExpurgoSolicitadoListener(ExpurgoGaleriaService expurgoService) {
        this.expurgoService = expurgoService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void executar(ExpurgoSolicitadoEvent evento) {
        try {
            expurgoService.executar(evento.galeriaId());
        } catch (RuntimeException ex) {
            log.warn("Falha ao expurgar a galeria {}: {}", evento.galeriaId(), ex.getMessage());
        }
    }
}
