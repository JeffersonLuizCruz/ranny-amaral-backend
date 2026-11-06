package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.model.SolicitacaoAceite;
import com.ranny.fotografia.contrato.model.documento.DadosAceite;
import com.ranny.fotografia.contrato.model.documento.StatusSolicitacaoAceite;

import java.time.Instant;

/** DTO de saida do link publico de aceite. */
public record SolicitacaoAceiteResponse(
        String id,
        String contratoId,
        String token,
        StatusSolicitacaoAceite status,
        Instant criadaEm,
        Instant expiraEm,
        Instant visualizadaEm,
        String protocolo,
        DadosAceite dadosAceite
) {

    public static SolicitacaoAceiteResponse de(SolicitacaoAceite solicitacao) {
        return new SolicitacaoAceiteResponse(
                solicitacao.getId(),
                solicitacao.getContratoId(),
                solicitacao.getToken(),
                solicitacao.getStatus(),
                solicitacao.getCriadaEm(),
                solicitacao.getExpiraEm(),
                solicitacao.getVisualizadaEm(),
                solicitacao.getProtocolo(),
                solicitacao.getDadosAceite());
    }
}
