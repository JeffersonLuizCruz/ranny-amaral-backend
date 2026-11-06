package com.ranny.fotografia.contrato.model.documento;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Estados do aceite do contrato. */
public enum StatusAceite {
    RASCUNHO("rascunho"),
    PENDENTE("pendente"),
    ACEITO("aceito"),
    EXPIRADO("expirado"),
    CANCELADO("cancelado");

    private final String valor;

    StatusAceite(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusAceite de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (StatusAceite status : values()) {
            if (status.valor.equalsIgnoreCase(valor)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de aceite invalido: " + valor);
    }
}
