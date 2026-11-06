package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Requisicao malformada ou parametro invalido — responde 400. */
public class RequisicaoInvalidaException extends ApiException {

    public RequisicaoInvalidaException(String codigo, String mensagem) {
        super(HttpStatus.BAD_REQUEST, codigo, mensagem);
    }
}
