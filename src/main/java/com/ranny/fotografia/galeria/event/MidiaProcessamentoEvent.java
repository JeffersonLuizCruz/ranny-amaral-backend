package com.ranny.fotografia.galeria.event;

/**
 * Solicita o processamento de derivados de uma midia fora da request de upload.
 *
 * <p>PATTERN: Domain Event. O evento carrega apenas identificadores; o listener
 * recarrega o agregado e executa o pipeline com sua propria transacao.
 */
public record MidiaProcessamentoEvent(
        String galeriaId,
        String midiaId,
        String arquivoId,
        boolean editada
) {
}
