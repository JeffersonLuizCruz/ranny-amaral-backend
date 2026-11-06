package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Arquivo com tipo/conteudo invalido — responde 422. */
public class ArquivoInvalidoException extends ApiException {

    public ArquivoInvalidoException(String codigo, String mensagem) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensagem);
    }
}
