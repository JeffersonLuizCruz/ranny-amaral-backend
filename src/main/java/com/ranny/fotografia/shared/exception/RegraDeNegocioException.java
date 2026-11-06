package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Violacao de regra de negocio — responde 422. */
public class RegraDeNegocioException extends ApiException {

    public RegraDeNegocioException(String codigo, String mensagem) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensagem);
    }
}
