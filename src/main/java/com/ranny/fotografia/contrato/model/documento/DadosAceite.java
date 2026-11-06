package com.ranny.fotografia.contrato.model.documento;

import com.ranny.fotografia.shared.model.TipoDocumento;

import java.time.Instant;

/**
 * Dados preenchidos pelo cliente no aceite publico.
 *
 * <p>O protocolo, o IP, o user-agent e o hash do conteudo formam a trilha de
 * auditoria do aceite eletronico.
 */
public record DadosAceite(
        String nomeCompleto,
        TipoDocumento tipoDocumento,
        String documento,
        String cidade,
        String whatsapp,
        Instant aceitoEm,
        String protocolo,
        String ip,
        String userAgent,
        String hashConteudo
) {
}
