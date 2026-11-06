package com.ranny.fotografia.contrato.api;

import java.time.Instant;

/** URL temporaria para o staff visualizar o comprovante direto do storage. */
public record ComprovanteDownloadResponse(String url, Instant expiraEm) {
}
