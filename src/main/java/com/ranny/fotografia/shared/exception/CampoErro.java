package com.ranny.fotografia.shared.exception;

/** Erro de validacao de um campo do payload. */
public record CampoErro(String campo, String mensagem) {
}
