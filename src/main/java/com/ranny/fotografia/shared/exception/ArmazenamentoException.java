package com.ranny.fotografia.shared.exception;

/** Falha de infraestrutura do object storage — responde 500. */
public class ArmazenamentoException extends RuntimeException {

    public ArmazenamentoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public ArmazenamentoException(String mensagem) {
        super(mensagem);
    }
}
