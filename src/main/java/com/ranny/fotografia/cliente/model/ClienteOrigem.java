package com.ranny.fotografia.cliente.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Canal pelo qual o cliente chegou ao estudio. */
public enum ClienteOrigem {
    INSTAGRAM("instagram"),
    INDICACAO("indicacao"),
    GOOGLE("google"),
    WHATSAPP("whatsapp"),
    SITE("site"),
    CONTRATO("contrato"),
    OUTRO("outro");

    private final String valor;

    ClienteOrigem(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static ClienteOrigem de(String valor) {
        if (valor == null || valor.isBlank()) {
            return OUTRO;
        }
        for (ClienteOrigem origem : values()) {
            if (origem.valor.equalsIgnoreCase(valor)) {
                return origem;
            }
        }
        throw new IllegalArgumentException("Origem de cliente invalida: " + valor);
    }
}
