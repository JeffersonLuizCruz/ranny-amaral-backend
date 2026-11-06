package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * PATTERN: DTO / Form Object.
 *
 * <p>O request aceita snapshot e clausulas ja resolvidas (para edicoes
 * manuais) ou apenas as referencias de cliente/pacote/agendamento. Quando os
 * documentos nao vem, o backend monta o snapshot e resolve o template.
 */
public record ContratoRequest(
        String clienteId,
        String agendamentoId,
        String pacoteId,

        @NotBlank(message = "nao pode ser vazio")
        String modeloId,

        @Size(max = 180, message = "deve ter no maximo 180 caracteres")
        String titulo,

        List<ContratoClausula> clausulas,
        ContratoSnapshot snapshot,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal valorTotal,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal valorEntrada,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal percentualEntrada,

        @Size(max = 40, message = "deve ter no maximo 40 caracteres")
        String formaPagamento,

        @Size(max = 1000, message = "deve ter no maximo 1000 caracteres")
        String condicoesPagamento,

        Instant dataValidade,

        @Size(max = 1000, message = "deve ter no maximo 1000 caracteres")
        String observacoes
) {
}
