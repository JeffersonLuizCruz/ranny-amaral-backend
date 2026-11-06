package com.ranny.fotografia.shared.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tipo de documento aceito no cadastro de clientes e no aceite de contratos.
 *
 * <p>PATTERN: Value Object serializado por contrato (Value Mapping). As
 * constantes Java ficam em maiusculo (convencao Java), mas o JSON usa os
 * valores em minusculo consumidos pelo frontend. O mapeamento fica no proprio
 * enum, evitando {@code @JsonProperty} repetido em cada DTO e mantendo o
 * contrato da API em um unico lugar.
 */
public enum TipoDocumento {
    RG("rg"),
    CPF("cpf");

    private final String valor;

    TipoDocumento(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static TipoDocumento de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (TipoDocumento tipo : values()) {
            if (tipo.valor.equalsIgnoreCase(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de documento invalido: " + valor);
    }
}
