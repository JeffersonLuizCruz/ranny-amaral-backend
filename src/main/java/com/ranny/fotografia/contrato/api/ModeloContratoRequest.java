package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** PATTERN: DTO / Form Object para modelos de contrato. */
public record ModeloContratoRequest(
        @NotBlank(message = "nao pode ser vazio")
        @Size(max = 160, message = "deve ter no maximo 160 caracteres")
        String nome,

        @Size(max = 1000, message = "deve ter no maximo 1000 caracteres")
        String descricao,

        @NotNull(message = "nao pode ser nulo")
        List<ContratoClausula> clausulas,

        Boolean ativo
) {
}
