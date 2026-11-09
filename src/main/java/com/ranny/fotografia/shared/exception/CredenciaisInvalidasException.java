package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** E-mail ou senha invalidos — responde 401 sem revelar qual campo falhou. */
public class CredenciaisInvalidasException extends ApiException {

    public CredenciaisInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS",
                "E-mail ou senha invalidos.");
    }
}
