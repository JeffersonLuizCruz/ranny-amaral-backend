package com.ranny.fotografia.galeria.listener;

import com.ranny.fotografia.galeria.event.MidiaProcessamentoEvent;
import com.ranny.fotografia.galeria.service.GaleriaService;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
        RuntimeException falha = tentar(evento);
        if (falha != null && causaDeIntegridade(falha)) {
            // Duas midias com o mesmo original podem gerar derivados identicos
            // ao mesmo tempo. O salvamento reaproveita o arquivo por checksum,
            // entao a segunda tentativa (transacao nova) apenas vincula os
            // derivados ja gravados.
            falha = tentar(evento);
        }
        if (falha == null) {
            return;
        }
        log.warn("Falha ao processar midia {} da galeria {}: {}",
                evento.midiaId(), evento.galeriaId(), falha.getMessage());
        try {
            galeriaService.registrarFalhaProcessamento(
                    evento.galeriaId(), evento.midiaId(), falha.getMessage());
        } catch (RuntimeException ex) {
            log.error("Falha ao registrar status FALHA da midia {}: {}",
                    evento.midiaId(), ex.getMessage());
        }
    }

    private RuntimeException tentar(MidiaProcessamentoEvent evento) {
        try {
            galeriaService.processarMidiaSolicitada(
                    evento.galeriaId(), evento.midiaId(), evento.arquivoId(), evento.editada());
            return null;
        } catch (RuntimeException ex) {
            return ex;
        }
    }

    private static boolean causaDeIntegridade(Throwable erro) {
        Throwable atual = erro;
        while (atual != null) {
            if (atual instanceof DataIntegrityViolationException
                    || atual instanceof ConstraintViolationException) {
                return true;
            }
            atual = atual.getCause();
        }
        return false;
    }
}
