package com.ranny.fotografia.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Contrato unico de erro da API.
 *
 * <p>PATTERN: Data Transfer Object (record) imutavel. Todos os erros — 400,
 * 404, 409, 422 e 500 — usam o mesmo formato, o que simplifica o tratamento no
 * frontend e a documentacao OpenAPI.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String codigo,
        String mensagem,
        String caminho,
        List<CampoErro> campos
) {

    public static ErrorResponse de(HttpStatus status, String codigo, String mensagem,
                                   String caminho, List<CampoErro> campos) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                codigo,
                mensagem,
                caminho,
                campos == null || campos.isEmpty() ? null : List.copyOf(campos)
        );
    }
}
