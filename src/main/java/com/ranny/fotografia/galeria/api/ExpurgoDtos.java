package com.ranny.fotografia.galeria.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** DTOs de armazenamento e expurgo por galeria. */
public final class ExpurgoDtos {

    private ExpurgoDtos() {
    }

    public record GrupoFinalidade(String finalidade, long bytes, long quantidade) {
    }

    public record ArmazenamentoGaleria(
            String galeriaId,
            String clienteNome,
            String status,
            long totalBytes,
            long quantidadeArquivos,
            List<GrupoFinalidade> grupos,
            boolean elegivel,
            String motivoBloqueio,
            Instant entregueEm,
            Instant zipBaixadoEm,
            Instant zipConcluidoEm,
            String expurgoStatus,
            Instant expurgoSolicitadoEm,
            String expurgoSolicitadoPor,
            String expurgoMotivo,
            Instant expurgoEfetivadoEm,
            long bytesExpurgados,
            String confirmacaoEsperada
    ) {
    }

    public record ExpurgoRequest(
            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 500, message = "deve ter no maximo 500 caracteres")
            String motivo,

            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 160, message = "deve ter no maximo 160 caracteres")
            String confirmacao
    ) {
    }

    public record ExpurgoResponse(
            String galeriaId,
            String expurgoStatus,
            long bytesEstimados,
            long quantidadeArquivos
    ) {
    }
}
