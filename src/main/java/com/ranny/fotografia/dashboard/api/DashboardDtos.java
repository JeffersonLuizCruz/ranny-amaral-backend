package com.ranny.fotografia.dashboard.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** DTOs do dashboard operacional. */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardResponse(
            String data,
            ResumoResponse resumo,
            List<ProximoEnsaioResponse> proximosEnsaios,
            List<PendenciaResponse> pendencias,
            FinanceiroResumoResponse financeiroMes
    ) {
    }

    public record ResumoResponse(
            int ensaiosHoje,
            int ensaiosSemana,
            long contratosAguardandoAceite,
            long contratosEntradaPendente,
            long contratosSaldoPendente,
            long galeriasEmSelecao,
            long galeriasAguardandoEdicao,
            long galeriasEmEdicao,
            long galeriasProntasEntrega,
            long extrasPendentes
    ) {
    }

    public record ProximoEnsaioResponse(
            String id,
            String clienteId,
            String clienteNome,
            String pacoteId,
            OffsetDateTime inicio,
            OffsetDateTime fim,
            String status
    ) {
    }

    public record PendenciaResponse(
            String tipo,
            String titulo,
            String descricao,
            String prioridade,
            String link
    ) {
    }

    public record FinanceiroResumoResponse(
            String mes,
            BigDecimal totalPrevisto,
            BigDecimal totalRecebido,
            BigDecimal totalAReceber,
            int quantidadePendentes,
            int quantidadeExtrasPendentes
    ) {
    }
}
