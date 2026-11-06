package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.model.ModeloContrato;
import com.ranny.fotografia.contrato.model.documento.ContratoClausula;

import java.time.Instant;
import java.util.List;

/** DTO de saida de modelo de contrato. */
public record ModeloContratoResponse(
        String id,
        String nome,
        String descricao,
        List<ContratoClausula> clausulas,
        boolean ativo,
        int versao,
        Instant createdAt,
        Instant updatedAt
) {

    public static ModeloContratoResponse de(ModeloContrato modelo) {
        return new ModeloContratoResponse(
                modelo.getId(),
                modelo.getNome(),
                modelo.getDescricao(),
                modelo.getClausulas(),
                modelo.isAtivo(),
                modelo.getVersao(),
                modelo.getAuditoria().getCriadoEm(),
                modelo.getAuditoria().getAtualizadoEm());
    }
}
