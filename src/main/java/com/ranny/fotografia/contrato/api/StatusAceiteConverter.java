package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.model.documento.StatusAceite;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * PATTERN: Strategy (Converter) para aceitar o status em minusculo na query
 * string, alinhado ao contrato JSON da API.
 */
@Component
public class StatusAceiteConverter implements Converter<String, StatusAceite> {

    @Override
    public StatusAceite convert(String origem) {
        return StatusAceite.de(origem);
    }
}
