package com.ranny.fotografia.contrato.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** PATTERN: DTO / Form Object da configuracao do estudio. */
public record EstudioRequest(
        @NotBlank(message = "nao pode ser vazio")
        @Size(max = 160, message = "deve ter no maximo 160 caracteres")
        String nome,

        @Size(max = 160, message = "deve ter no maximo 160 caracteres")
        String razaoSocial,

        @Size(max = 20, message = "deve ter no maximo 20 caracteres")
        String cnpj,

        @Size(max = 200, message = "deve ter no maximo 200 caracteres")
        String endereco,

        @Size(max = 120, message = "deve ter no maximo 120 caracteres")
        String cidade,

        @Size(max = 2, message = "deve ter no maximo 2 caracteres")
        String estado,

        @Email(message = "deve ser um e-mail valido")
        @Size(max = 180, message = "deve ter no maximo 180 caracteres")
        String email,

        @Size(max = 30, message = "deve ter no maximo 30 caracteres")
        String telefone,

        @Size(max = 120, message = "deve ter no maximo 120 caracteres")
        String responsavel,

        @Size(max = 180, message = "deve ter no maximo 180 caracteres")
        String site,

        @Size(max = 120, message = "deve ter no maximo 120 caracteres")
        String foro,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        BigDecimal percentualEntradaPadrao,

        @Size(max = 180, message = "deve ter no maximo 180 caracteres")
        String chavePix,

        @Size(max = 1500, message = "deve ter no maximo 1500 caracteres")
        String instrucoesPagamento,

        @PositiveOrZero(message = "deve ser maior ou igual a zero")
        Integer prazoPagamentoDias
) {
}
