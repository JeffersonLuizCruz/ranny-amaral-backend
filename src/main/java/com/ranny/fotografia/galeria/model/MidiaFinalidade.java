package com.ranny.fotografia.galeria.model;

import java.util.Locale;

/**
 * Papel da midia publicada na galeria.
 *
 * <p>EDICAO: o arquivo enviado e o original que ainda passara pela edicao.
 * PRONTA: o arquivo enviado ja e a versao final, liberada para download apos a
 * selecao do cliente, sem uma segunda versao.
 */
public enum MidiaFinalidade {

    EDICAO,
    PRONTA;

    public static MidiaFinalidade de(String valor) {
        if (valor == null || valor.isBlank()) {
            return EDICAO;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
