package com.ranny.fotografia.contrato.model.converter;

import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot;
import com.ranny.fotografia.shared.json.JsonApoio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** PATTERN: Adapter JPA para o snapshot comercial imutavel do contrato. */
@Converter
public class SnapshotContratoConverter implements AttributeConverter<ContratoSnapshot, String> {

    @Override
    public String convertToDatabaseColumn(ContratoSnapshot atributo) {
        return JsonApoio.escrever(atributo);
    }

    @Override
    public ContratoSnapshot convertToEntityAttribute(String coluna) {
        return JsonApoio.ler(coluna, ContratoSnapshot.class);
    }
}
