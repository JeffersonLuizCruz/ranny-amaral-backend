package com.ranny.fotografia.cliente.api;

import com.ranny.fotografia.cliente.model.ClienteOrigem;
import com.ranny.fotografia.shared.model.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PATTERN: DTO / Form Object (record imutavel) na fronteira HTTP.
 *
 * <p>O payload nao acessa a entidade diretamente. As anotacoes do Jakarta Bean
 * Validation expressam o contrato de entrada e o {@code GlobalExceptionHandler}
 * converte as violacoes em 422 com a lista de campos.
 */
public record ClienteRequest(
        @NotBlank(message = "nao pode ser vazio")
        @Size(max = 120, message = "deve ter no maximo 120 caracteres")
        String nome,

        @Size(max = 30, message = "deve ter no maximo 30 caracteres")
        String whatsapp,

        @Email(message = "deve ser um e-mail valido")
        @Size(max = 180, message = "deve ter no maximo 180 caracteres")
        String email,

        @Size(max = 30, message = "deve ter no maximo 30 caracteres")
        String documento,

        TipoDocumento tipoDocumento,

        @Size(max = 120, message = "deve ter no maximo 120 caracteres")
        String cidade,

        @NotNull(message = "nao pode ser nulo")
        ClienteOrigem origem,

        @Size(max = 1000, message = "deve ter no maximo 1000 caracteres")
        String observacoes
) {
}
