package com.ranny.fotografia.contrato.model.converter;

import com.ranny.fotografia.contrato.model.documento.DadosAceite;
import com.ranny.fotografia.shared.json.JsonApoio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** PATTERN: Adapter JPA para os dados do aceite eletronico. */
@Converter
public class DadosAceiteConverter implements AttributeConverter<DadosAceite, String> {

    @Override
    public String convertToDatabaseColumn(DadosAceite atributo) {
        return JsonApoio.escrever(atributo);
    }

    @Override
    public DadosAceite convertToEntityAttribute(String coluna) {
        return JsonApoio.ler(coluna, DadosAceite.class);
    }
}
