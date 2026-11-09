package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Tentativas de login excedidas — responde 429. */
public class ContaBloqueadaException extends ApiException {

    public ContaBloqueadaException(String mensagem) {
        super(HttpStatus.TOO_MANY_REQUESTS, "CONTA_BLOQUEADA", mensagem);
    }
}
