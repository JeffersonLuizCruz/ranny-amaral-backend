package com.ranny.fotografia.shared.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Forma de recebimento do saldo final registrado manualmente pelo estudio. */
public enum FormaPagamentoSaldo {
    PIX("pix"),
    DINHEIRO("dinheiro"),
    OUTRO("outro");

    private final String valor;

    FormaPagamentoSaldo(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static FormaPagamentoSaldo de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (FormaPagamentoSaldo forma : values()) {
            if (forma.valor.equalsIgnoreCase(valor)) {
                return forma;
            }
        }
        throw new IllegalArgumentException("Forma de pagamento invalida: " + valor);
    }
}
