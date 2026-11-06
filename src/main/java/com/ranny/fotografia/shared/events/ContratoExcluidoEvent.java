package com.ranny.fotografia.shared.events;

/**
 * Publicado quando um contrato é excluído.
 *
 * <p>O agendamento precisa limpar o vínculo (`contratoId`) para não ficar
 * bloqueado por um contrato que não existe mais. O evento mantém o
 * desacoplamento entre os módulos {@code contrato} e {@code agenda}.
 */
public record ContratoExcluidoEvent(String contratoId, String agendamentoId) {
}
