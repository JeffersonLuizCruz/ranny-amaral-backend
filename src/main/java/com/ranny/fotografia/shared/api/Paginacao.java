package com.ranny.fotografia.shared.api;

import com.ranny.fotografia.shared.exception.RequisicaoInvalidaException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Normaliza e valida os parametros de paginacao.
 *
 * <p>PATTERN: Parameter Object + Guard Clause.
 *
 * <p>Concentra em um unico ponto o default, o limite maximo e a mensagem de
 * erro. Os controllers nao repetem a regra e a API responde sempre com o mesmo
 * codigo de negocio ({@code PAGINACAO_INVALIDA}).
 */
public final class Paginacao {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {
    }

    public static Pageable de(Integer pagina, Integer tamanho, Sort sort) {
        int paginaNormalizada = pagina == null ? 0 : pagina;
        int tamanhoNormalizado = tamanho == null ? TAMANHO_PADRAO : tamanho;

        if (paginaNormalizada < 0) {
            throw new RequisicaoInvalidaException("PAGINACAO_INVALIDA",
                    "O parametro 'pagina' deve ser maior ou igual a zero.");
        }
        if (tamanhoNormalizado < 1 || tamanhoNormalizado > TAMANHO_MAXIMO) {
            throw new RequisicaoInvalidaException("PAGINACAO_INVALIDA",
                    "O parametro 'tamanho' deve estar entre 1 e %d.".formatted(TAMANHO_MAXIMO));
        }

        return PageRequest.of(paginaNormalizada, tamanhoNormalizado, sort);
    }
}
