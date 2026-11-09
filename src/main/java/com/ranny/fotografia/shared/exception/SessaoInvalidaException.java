package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Sessao expirada, revogada ou pertencente a outro usuario. */
public class SessaoInvalidaException extends ApiException {

    public SessaoInvalidaException() {
        super(HttpStatus.UNAUTHORIZED, "SESSAO_INVALIDA",
                "A sessao expirou ou foi revogada. Entre novamente.");
    }
}
