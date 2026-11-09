package com.ranny.fotografia.auth.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Perfis de acesso do CRM. */
public enum Role {
    ADMIN("admin"),
    GESTOR("gestor");

    private final String valor;

    Role(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static Role de(String valor) {
        if (valor == null || valor.isBlank()) {
            return GESTOR;
        }
        for (Role role : values()) {
            if (role.valor.equalsIgnoreCase(valor) || role.name().equalsIgnoreCase(valor)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Perfil invalido: " + valor);
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
