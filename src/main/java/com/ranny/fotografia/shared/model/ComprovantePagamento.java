package com.ranny.fotografia.shared.model;

import java.time.Instant;

/**
 * Metadados do comprovante enviado pelo cliente.
 *
 * <p>O arquivo em si nao e armazenado nesta primeira fase (upload fica para o
 * modulo de galerias). O contrato guarda apenas a referencia logica
 * ({@code arquivoKey}) e os metadados, o que permite trocar disco local por
 * object storage sem alterar o modelo.
 */
public record ComprovantePagamento(
        String arquivoKey,
        String nomeArquivo,
        String mimeType,
        long tamanho,
        Instant enviadoEm
) {
}
