package com.ranny.fotografia.galeria.listener;

import com.ranny.fotografia.galeria.event.MidiaProcessamentoEvent;
import com.ranny.fotografia.galeria.service.GaleriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Processa derivados de midia depois do commit do upload.
 *
 * <p>PATTERN: Observer / Publish-Subscribe com {@code @TransactionalEventListener}
 * e {@code @Async}. A request de upload responde assim que o arquivo e aceito;
 * thumb/web sao gerados em background e a UI acompanha por polling.
 *
 * <p>Diferente dos eventos de contrato, este evento e leve e nao usa o outbox:
 * uma falha de processo deixa a midia em {@code PROCESSANDO} e pode ser
 * reprocessada manualmente (DT-033).
 */
@Component
public class MidiaProcessamentoListener {

    private static final Logger log = LoggerFactory.getLogger(MidiaProcessamentoListener.class);

    private final GaleriaService galeriaService;

    public MidiaProcessamentoListener(GaleriaService galeriaService) {
        this.galeriaService = galeriaService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processar(MidiaProcessamentoEvent evento) {
        try {
            galeriaService.processarMidiaSolicitada(
                    evento.galeriaId(), evento.midiaId(), evento.arquivoId(), evento.editada());
        } catch (RuntimeException ex) {
            log.warn("Falha ao processar midia {} da galeria {}: {}",
                    evento.midiaId(), evento.galeriaId(), ex.getMessage());
        }
    }
}
