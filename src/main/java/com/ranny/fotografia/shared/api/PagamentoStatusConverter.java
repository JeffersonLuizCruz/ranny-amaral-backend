package com.ranny.fotografia.shared.api;

import com.ranny.fotografia.shared.model.PagamentoStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * PATTERN: Strategy (Converter) para aceitar o status de pagamento em minusculo
 * na query string, alinhado ao contrato JSON da API.
 */
@Component
public class PagamentoStatusConverter implements Converter<String, PagamentoStatus> {

    @Override
    public PagamentoStatus convert(String origem) {
        return PagamentoStatus.de(origem);
    }
}
