package com.ranny.fotografia.arquivo.service;

import com.ranny.fotografia.arquivo.ArquivoFinalidade;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

/**
 * Tipos de arquivo aceitos, assinaturas binarias e finalidades permitidas.
 *
 * <p>PATTERN: Strategy / Value Object.
 *
 * <p>A validacao nao confia no Content-Type informado pelo navegador: os
 * primeiros bytes precisam corresponder ao formato declarado.
 */
enum TipoArquivo {
    PDF("application/pdf", "pdf", new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}),
    JPEG("image/jpeg", "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", "png",
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    WEBP("image/webp", "webp", "RIFF".getBytes(StandardCharsets.US_ASCII)),
    MP4("video/mp4", "mp4", "ftyp".getBytes(StandardCharsets.US_ASCII)),
    MOV("video/quicktime", "mov", "ftyp".getBytes(StandardCharsets.US_ASCII));

    private final String mimeType;
    private final String extensao;
    private final byte[] assinatura;

    TipoArquivo(String mimeType, String extensao, byte[] assinatura) {
        this.mimeType = mimeType;
        this.extensao = extensao;
        this.assinatura = assinatura;
    }

    String mimeType() {
        return mimeType;
    }

    String extensao() {
        return extensao;
    }

    static TipoArquivo porMime(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        String normalizado = mimeType.trim().toLowerCase();
        if ("image/jpg".equals(normalizado)) {
            return JPEG;
        }
        for (TipoArquivo tipo : values()) {
            if (tipo.mimeType.equalsIgnoreCase(normalizado)) {
                return tipo;
            }
        }
        return null;
    }

    static Set<TipoArquivo> permitidos(ArquivoFinalidade finalidade) {
        return switch (finalidade) {
            case COMPROVANTE_PAGAMENTO -> Set.of(PDF, JPEG, PNG, WEBP);
            case CONTRATO_PDF -> Set.of(PDF);
            case FOTO_ORIGINAL, FOTO_DERIVADO, FOTO_EDITADA -> Set.of(JPEG, PNG, WEBP);
            case VIDEO_ORIGINAL -> Set.of(MP4, MOV);
        };
    }

    static TipoArquivo detectar(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }
        for (TipoArquivo tipo : values()) {
            if (tipo == WEBP) {
                if (bytes.length >= 12
                        && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                        && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) {
                    return WEBP;
                }
                continue;
            }
            if (tipo == MP4 || tipo == MOV) {
                if (bytes.length >= 12
                        && "ftyp".equals(new String(bytes, 4, 4, StandardCharsets.US_ASCII))) {
                    String marca = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
                    if (tipo == MOV && "qt  ".equals(marca)) {
                        return MOV;
                    }
                    if (tipo == MP4 && !"qt  ".equals(marca)) {
                        return MP4;
                    }
                }
                continue;
            }
            if (bytes.length >= tipo.assinatura.length
                    && Arrays.equals(
                    Arrays.copyOfRange(bytes, 0, tipo.assinatura.length),
                    tipo.assinatura)) {
                return tipo;
            }
        }
        return null;
    }
}
