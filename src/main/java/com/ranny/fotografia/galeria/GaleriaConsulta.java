package com.ranny.fotografia.galeria;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * API publica de leitura financeira e operacional das galerias.
 *
 * <p>O modulo financeiro consulta os extras e o modulo de armazenamento consulta
 * tamanhos/expurgo sem acessar as entidades internas de galeria.
 */
public interface GaleriaConsulta {

    Map<String, ResumoExtras> buscarExtrasPorAgendamentos(Collection<String> agendamentoIds);

    /** Contagens por etapa para o dashboard operacional. */
    ResumoOperacional resumoOperacional();

    /**
     * Dados de cada galeria para a tela de armazenamento.
     *
     * <p>A elegibilidade do expurgo e calculada dentro do modulo de galeria, que
     * conhece o ciclo de vida, os extras e os links.
     */
    List<ResumoArmazenamento> galeriasParaArmazenamento();

    enum StatusExtras {
        PENDENTE,
        PAGO,
        ISENTO,
        CANCELADO
    }

    record ResumoExtras(
            String galeriaId,
            String pedidoId,
            int quantidadeFotos,
            BigDecimal valorTotal,
            BigDecimal valorPago,
            BigDecimal valorPendente,
            StatusExtras status
    ) {
    }

    record ResumoOperacional(
            long aguardandoUpload,
            long emSelecao,
            long selecaoEnviada,
            long emEdicao,
            long entregue,
            long extrasPendentes
    ) {
    }

    record ResumoArmazenamento(
            String galeriaId,
            String clienteNome,
            String status,
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
}
