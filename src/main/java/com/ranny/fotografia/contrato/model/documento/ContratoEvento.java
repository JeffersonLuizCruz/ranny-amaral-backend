package com.ranny.fotografia.contrato.model.documento;

import java.time.Instant;

/** Evento imutavel do historico do contrato. */
public record ContratoEvento(String id, ContratoEventoTipo tipo, Instant em, String detalhe) {
}
