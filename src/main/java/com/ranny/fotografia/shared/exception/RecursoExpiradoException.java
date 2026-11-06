package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Recurso expirado ou indisponivel — responde 410. */
public class RecursoExpiradoException extends ApiException {

    public RecursoExpiradoException(String codigo, String mensagem) {
        super(HttpStatus.GONE, codigo, mensagem);
    }
}
