package com.ranny.fotografia.contrato.model.documento;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Estados da solicitacao (link publico) de aceite. */
public enum StatusSolicitacaoAceite {
    PENDENTE("pendente"),
    VISUALIZADA("visualizada"),
    ACEITA("aceita"),
    EXPIRADA("expirada"),
    CANCELADA("cancelada");

    private final String valor;

    StatusSolicitacaoAceite(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusSolicitacaoAceite de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (StatusSolicitacaoAceite status : values()) {
            if (status.valor.equalsIgnoreCase(valor)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de solicitacao invalido: " + valor);
    }

    public boolean estaAberta() {
        return this == PENDENTE || this == VISUALIZADA;
    }
}
