package com.ranny.fotografia.galeria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * API publica de leitura financeira das galerias.
 *
 * <p>O modulo financeiro consulta os extras sem acessar as entidades internas
 * de galeria.
 */
public interface GaleriaConsulta {

    Map<String, ResumoExtras> buscarExtrasPorAgendamentos(Collection<String> agendamentoIds);

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
            StatusExtras status
    ) {
    }
}
