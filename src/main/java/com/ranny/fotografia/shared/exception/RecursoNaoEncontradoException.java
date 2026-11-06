package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Recurso inexistente — responde 404. */
public class RecursoNaoEncontradoException extends ApiException {

    public RecursoNaoEncontradoException(String recurso, String id) {
        super(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                "%s nao encontrado(a) para o identificador '%s'.".formatted(recurso, id));
    }

    public RecursoNaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", mensagem);
    }
}
