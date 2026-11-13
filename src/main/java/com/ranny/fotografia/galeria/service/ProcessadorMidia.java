package com.ranny.fotografia.galeria.service;

import java.util.List;

/**
 * Porta de processamento de imagem.
 *
 * <p>PATTERN: Strategy / Ports & Adapters.
 *
 * <p>A implementacao inicial usa Thumbnailator/ImageIO; em producao de alto
 * volume pode ser substituida por libvips sem alterar o modulo de galeria.
 */
public interface ProcessadorMidia {

    Resultado processar(byte[] conteudo, String nomeOriginal, String marcaDagua);

    record Derivado(
            String tipo,
            String nomeArquivo,
            String mimeType,
            byte[] conteudo,
            int largura,
            int altura,
            boolean marcada
    ) {
    }

    record Resultado(
            int largura,
            int altura,
            List<Derivado> derivados
    ) {
    }
}
