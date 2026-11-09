package com.ranny.fotografia.agenda;

import com.ranny.fotografia.agenda.model.StatusAgendamento;
import com.ranny.fotografia.shared.model.PagamentoStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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

    List<ResumoAgendamento> listarPorPeriodo(OffsetDateTime inicio, OffsetDateTime fim);

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
            PagamentoStatus entradaStatus,
            PagamentoStatus finalStatus,
            boolean entradaQuitada,
            boolean saldoQuitado,
            boolean quitado,
            BigDecimal valorRecebido,
            BigDecimal valorPendente,
            CondicoesResumo condicoes
    ) {

        public boolean realizado() {
            return status == StatusAgendamento.REALIZADO;
        }

        public boolean cancelado() {
            return status == StatusAgendamento.CANCELADO;
        }

        public String statusValor() {
            return status == null ? null : status.getValor();
        }

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
