package com.ranny.fotografia.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excecao base da API.
 *
 * <p>PATTERN: Exception Hierarchy (GoF/idiomatico). A heranca aqui e usada para
 * polimorfismo de tratamento — o {@code GlobalExceptionHandler} captura a base e
 * cada subtipo carrega o status HTTP e o codigo de erro do seu dominio. Isso e
 * diferente de herdar entidades para reusar campos, pratica proibida neste
 * projeto.
 *
 * <p>Beneficios: o controller/service lanca a excecao mais expressiva
 * ({@code RecursoNaoEncontradoException}), e o mapeamento HTTP fica centralizado.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    protected ApiException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }
}
