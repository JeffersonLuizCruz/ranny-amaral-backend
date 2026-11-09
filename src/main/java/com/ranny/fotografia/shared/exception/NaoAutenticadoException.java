package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Credenciais ausentes ou invalidas — responde 401. */
public class NaoAutenticadoException extends ApiException {

    public NaoAutenticadoException(String codigo, String mensagem) {
        super(HttpStatus.UNAUTHORIZED, codigo, mensagem);
    }
}
