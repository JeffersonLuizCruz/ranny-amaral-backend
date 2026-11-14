package com.ranny.fotografia.armazenamento.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/** DTOs da tela de armazenamento. */
public final class ArmazenamentoDtos {

    private ArmazenamentoDtos() {
    }

    public record TotaisResponse(
            long originaisBytes,
            long editadasBytes,
            long derivadosBytes,
            long videosBytes,
            long comprovantesBytes,
            long totalBytes,
            long quantidade
    ) {
    }

    public record ResumoResponse(
            Instant coletadoEm,
            String origemColeta,
            String volumePath,
            Long totalBytes,
            Long livreBytes,
            Long usadoBytes,
            Long crmBytes,
            Long objetosBytes,
            Long postgresBytes,
            Long outrosBytes,
            long reservaBytes,
            Long disponivelParaFotosBytes,
            Integer percentualUsado,
            Integer percentualLimpeza,
            Integer percentualAlvo,
            String alerta,
            Long diasRestantesEstimados,
            long galeriasElegiveis,
            TotaisResponse totais
    ) {
    }

    public record GaleriaArmazenamento(
            String galeriaId,
            String clienteNome,
            String status,
            long totalBytes,
            long quantidadeArquivos,
            Instant entregueEm,
            Instant zipBaixadoEm,
            Instant zipConcluidoEm,
            String expurgoStatus,
            Instant expurgoEfetivadoEm,
            long bytesExpurgados,
            boolean elegivel,
            String motivoBloqueio
    ) {
    }

    public record ReconciliacaoResponse(
            long objetosStorage,
            long objetosCatalogo,
            long somenteStorage,
            long somenteCatalogo,
            long bytesSomenteStorage,
            List<String> amostraSomenteStorage,
            List<String> amostraSomenteCatalogo
    ) {
    }

    public record LimpezaRequest(
            @NotNull(message = "nao pode ser nulo")
            @Min(value = 1, message = "deve ser maior que zero")
            @Max(value = 100, message = "deve ser menor ou igual a 100")
            Integer alvoPercentual
    ) {
    }

    public record LimpezaResponse(
            int galeriasAgendadas,
            long bytesEstimados,
            int percentualAntes,
            int percentualDepoisEstimado,
            boolean atingiuAlvo
    ) {
    }
}
