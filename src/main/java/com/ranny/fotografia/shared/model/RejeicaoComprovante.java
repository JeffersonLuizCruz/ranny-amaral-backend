package com.ranny.fotografia.shared.model;

import java.time.Instant;

/** Motivo e data da rejeicao de um comprovante de entrada. */
public record RejeicaoComprovante(String motivo, Instant rejeitadoEm) {
}
