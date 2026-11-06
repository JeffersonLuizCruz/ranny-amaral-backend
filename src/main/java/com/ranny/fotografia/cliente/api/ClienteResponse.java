package com.ranny.fotografia.cliente.api;

import com.ranny.fotografia.cliente.model.Cliente;
import com.ranny.fotografia.cliente.model.ClienteOrigem;
import com.ranny.fotografia.shared.model.TipoDocumento;

import java.time.Instant;

/**
 * DTO de saida. Os nomes seguem exatamente o contrato consumido pelo frontend
 * ({@code createdAt}/{@code updatedAt}, enums em minusculo).
 *
 * <p>PATTERN: Factory Method ({@code de}) mantendo o mapeamento perto do
 * contrato e fora do controller.
 */
public record ClienteResponse(
        String id,
        String nome,
        String whatsapp,
        String email,
        String documento,
        TipoDocumento tipoDocumento,
        String cidade,
        ClienteOrigem origem,
        String observacoes,
        Instant createdAt,
        Instant updatedAt
) {

    public static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getWhatsapp(),
                cliente.getEmail(),
                cliente.getDocumento(),
                cliente.getTipoDocumento(),
                cliente.getCidade(),
                cliente.getOrigem(),
                cliente.getObservacoes(),
                cliente.getAuditoria().getCriadoEm(),
                cliente.getAuditoria().getAtualizadoEm()
        );
    }
}
