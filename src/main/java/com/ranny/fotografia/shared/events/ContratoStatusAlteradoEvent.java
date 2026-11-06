package com.ranny.fotografia.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Evento publicado quando o estado comercial de um contrato muda.
 *
 * <p>PATTERN: Publish/Subscribe (Observer) via Application Events.
 *
 * <p>O payload é intencionalmente compacto para caber em `varchar(255)` do
 * outbox. As chaves JSON curtas reduzem o tamanho sem perder informação; os
 * nomes Java permanecem expressivos. Dados completos de pagamento continuam no
 * contrato, que é a fonte de verdade.
 */
public record ContratoStatusAlteradoEvent(
        @JsonProperty("c") String contratoId,
        @JsonProperty("a") String agendamentoId,
        @JsonProperty("sa") String statusAceite,
        @JsonProperty("sp") String statusPagamento,
        @JsonProperty("ev") BigDecimal entradaValor,
        @JsonProperty("es") String entradaStatus,
        @JsonProperty("sv") BigDecimal saldoValor,
        @JsonProperty("ss") String saldoStatus
) {
}
