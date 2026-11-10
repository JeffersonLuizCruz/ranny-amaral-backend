package com.ranny.fotografia.pacote.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PATTERN: DTO / Form Object com validação declarativa.
 *
 * <p>As quantidades e o valor da foto extra são opcionais: quando ausentes, o
 * service infere a partir dos itens inclusos (mesma heurística do frontend).
 * Isso mantém compatibilidade com payloads antigos sem abrir mão de um contrato
 * explícito para os novos.
 */
public record PacoteRequest(
        @NotBlank(message = "nao pode ser vazio")
        @Size(max = 120, message = "deve ter no maximo 120 caracteres")
        String nome,

        @NotNull(message = "nao pode ser nulo")
        @Positive(message = "deve ser maior que zero")
        Integer duracaoMinutos,

        @NotNull(message = "nao pode ser nulo")
        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal valor,

        @Size(max = 1000, message = "deve ter no maximo 1000 caracteres")
        String descricao,

        @NotBlank(message = "nao pode ser vazio")
        @Size(max = 9, message = "deve ter no maximo 9 caracteres")
        String corHex,

        @NotNull(message = "nao pode ser nulo")
        List<@NotBlank(message = "nao pode ser vazio") String> itensInclusos,

        Boolean ativo,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        Integer quantidadeFotosInclusas,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        Integer quantidadeVideosInclusos,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal valorFotoExtra,

        @Valid
        @Size(max = 20, message = "deve ter no maximo 20 faixas")
        List<FaixaFotoExtraRequest> faixasFotoExtra,

        @Valid
        PromocaoRequest promocao
) {

    public record FaixaFotoExtraRequest(
            @NotNull(message = "nao pode ser nulo")
            @Positive(message = "deve ser maior que zero")
            Integer quantidadeMinima,

            @NotNull(message = "nao pode ser nulo")
            @PositiveOrZero(message = "deve ser maior ou igual a zero")
            BigDecimal valorUnitario
    ) {
    }

    public record PromocaoRequest(
            Boolean ativa,
            @PositiveOrZero(message = "deve ser maior ou igual a zero")
            BigDecimal valorPromocional,
            LocalDate validaAte,
            @Size(max = 120, message = "deve ter no maximo 120 caracteres")
            String rotulo
    ) {
    }
}
