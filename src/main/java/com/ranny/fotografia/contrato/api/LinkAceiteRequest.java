package com.ranny.fotografia.contrato.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Quantidade de dias de validade do link publico. */
public record LinkAceiteRequest(
        @Min(value = 1, message = "deve ser no minimo 1 dia")
        @Max(value = 60, message = "deve ser no maximo 60 dias")
        Integer expiresInDays
) {
}
