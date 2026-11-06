package com.ranny.fotografia.shared.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginacao da API.
 *
 * <p>PATTERN: Data Transfer Object (DTO) + Factory Method.
 *
 * <p>O JSON de {@link Page} do Spring Data nao e um contrato estavel: expoe
 * detalhes internos ({@code pageable}, {@code sort}, {@code empty}) que mudam
 * entre versoes. Este record fixa apenas o que o frontend precisa e mantem a
 * resposta independente do framework.
 */
public record PaginaResponse<T>(
        List<T> itens,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas,
        boolean temAnterior,
        boolean temProxima
) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasPrevious(),
                page.hasNext());
    }

    public static <T, R> PaginaResponse<R> de(Page<T> page, Function<T, R> mapper) {
        return de(page.map(mapper));
    }
}
