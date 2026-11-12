package com.ranny.fotografia.galeria.model;

import java.util.Locale;

/** Etapa do expurgo de binarios de uma galeria. */
public enum ExpurgoStatus {
    NENHUM,
    AGENDADO,
    EXECUTANDO,
    EXECUTADO,
    CANCELADO;

    public String valor() {
        return name().toLowerCase(Locale.ROOT);
    }
}
