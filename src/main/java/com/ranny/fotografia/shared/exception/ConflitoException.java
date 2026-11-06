package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Conflito com o estado atual do recurso — responde 409. */
public class ConflitoException extends ApiException {

    public ConflitoException(String codigo, String mensagem) {
        super(HttpStatus.CONFLICT, codigo, mensagem);
    }
}
