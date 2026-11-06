package com.ranny.fotografia.shared.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Estado de qualquer pagamento (entrada, saldo final ou extras).
 *
 * <p>O status e um dado consultavel: por isso e persistido como coluna
 * relacional ({@code @Enumerated(STRING)}) e nao dentro de um JSON.
 */
public enum PagamentoStatus {
    PENDENTE("pendente"),
    CONFIRMADO("confirmado"),
    ISENTO("isento"),
    CANCELADO("cancelado");

    private final String valor;

    PagamentoStatus(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static PagamentoStatus de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (PagamentoStatus status : values()) {
            if (status.valor.equalsIgnoreCase(valor)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de pagamento invalido: " + valor);
    }

    public boolean estaQuitado() {
        return this == CONFIRMADO || this == ISENTO;
    }
}
