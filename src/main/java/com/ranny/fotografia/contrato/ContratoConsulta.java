package com.ranny.fotografia.contrato;

import com.ranny.fotografia.shared.model.PagamentoStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * API publica de leitura financeira do modulo {@code contrato}.
 *
 * <p>Outros modulos (financeiro) consultam valores e status sem acessar a
 * entidade {@code Contrato} nem o repositorio.
 */
public interface ContratoConsulta {

    Optional<ResumoFinanceiro> buscarPorAgendamento(String agendamentoId);

    Map<String, ResumoFinanceiro> buscarPorAgendamentos(Collection<String> agendamentoIds);

    /** Contagens acionaveis para o dashboard operacional. */
    ResumoPendentes resumoPendentes();

    record ResumoPendentes(
            long aguardandoAceite,
            long entradaPendente,
            long saldoPendente
    ) {
    }

    record ResumoFinanceiro(
            String contratoId,
            String agendamentoId,
            BigDecimal valorTotal,
            BigDecimal valorEntrada,
            BigDecimal percentualEntrada,
            BigDecimal saldoRestante,
            PagamentoStatus statusEntrada,
            PagamentoStatus statusFinal
    ) {
    }
}
