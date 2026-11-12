package com.ranny.fotografia.galeria.event;

/**
 * Solicita a execucao assincrona do expurgo depois do commit.
 *
 * <p>Mesmo padrao do processamento de midia: evento leve, sem outbox. Se o
 * processo cair, a galeria permanece {@code AGENDADO} e pode ser reexecutada.
 */
public record ExpurgoSolicitadoEvent(String galeriaId) {
}
