package com.ranny.fotografia.galeria.model;

import java.util.Locale;

/** Ciclo de vida da galeria. */
public enum GaleriaStatus {
    AGUARDANDO_UPLOAD,
    EM_SELECAO,
    SELECAO_ENVIADA,
    EM_EDICAO,
    ENTREGUE,
    CANCELADA;

    public String valor() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static GaleriaStatus de(String valor) {
        return valor == null || valor.isBlank() ? null
                : GaleriaStatus.valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }
}
