package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Usuario autenticado sem permissao para a operacao — responde 403. */
public class AcessoNegadoException extends ApiException {

    public AcessoNegadoException(String codigo, String mensagem) {
        super(HttpStatus.FORBIDDEN, codigo, mensagem);
    }
}
