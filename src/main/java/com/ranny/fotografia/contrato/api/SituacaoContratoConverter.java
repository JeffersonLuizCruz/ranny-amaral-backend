package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.model.SituacaoContrato;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Aceita a situacao em minusculo na query string, alinhada ao contrato JSON
 * consumido pelo frontend.
 */
@Component
public class SituacaoContratoConverter implements Converter<String, SituacaoContrato> {

    @Override
    public SituacaoContrato convert(String origem) {
        return SituacaoContrato.de(origem);
    }
}
