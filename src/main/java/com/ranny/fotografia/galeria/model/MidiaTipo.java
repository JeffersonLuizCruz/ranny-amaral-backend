package com.ranny.fotografia.galeria.model;

import java.util.Locale;

public enum MidiaTipo {
    FOTO,
    VIDEO;

    public String valor() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MidiaTipo de(String valor) {
        return valor == null || valor.isBlank() ? null
                : MidiaTipo.valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }
}
