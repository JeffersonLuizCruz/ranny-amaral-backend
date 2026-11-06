package com.ranny.fotografia.agenda;

import com.ranny.fotografia.agenda.model.StatusAgendamento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * PATTERN: Public API / Port do modulo {@code agenda}.
 *
 * <p>O modulo {@code contrato} monta o snapshot comercial a partir dos dados
 * reais do ensaio. Consultar por esta interface evita importar a entidade e
 * mantem a fronteira do Modulith.
 */
public interface AgendamentoConsulta {

    Optional<ResumoAgendamento> buscar(String agendamentoId);

    /** Resumo imutavel consumido por outros modulos. */
    record ResumoAgendamento(
            String id,
            String clienteId,
            String pacoteId,
            String contratoId,
            ContatoResumo contato,
            OffsetDateTime dataHoraInicio,
            OffsetDateTime dataHoraFim,
            String local,
            StatusAgendamento status,
            String observacoes,
            BigDecimal valorAcordado,
            BigDecimal valorEntrada,
            BigDecimal percentualEntrada,
            BigDecimal saldoRestante,
            CondicoesResumo condicoes
    ) {

        public record ContatoResumo(String nome, String whatsapp) {
        }

        public record CondicoesResumo(
                int quantidadeFotosInclusas,
                int quantidadeVideosInclusos,
                BigDecimal valorFotoExtra,
                BigDecimal valorTabela,
                BigDecimal promocaoValor,
                String promocaoRotulo
        ) {
        }
    }
}
