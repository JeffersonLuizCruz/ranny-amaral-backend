package com.ranny.fotografia.shared.model;

import com.ranny.fotografia.shared.json.JsonApoio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** PATTERN: Adapter JPA para serializar a rejeicao do comprovante. */
@Converter
public class RejeicaoComprovanteConverter implements AttributeConverter<RejeicaoComprovante, String> {

    @Override
    public String convertToDatabaseColumn(RejeicaoComprovante atributo) {
        return JsonApoio.escrever(atributo);
    }

    @Override
    public RejeicaoComprovante convertToEntityAttribute(String coluna) {
        return JsonApoio.ler(coluna, RejeicaoComprovante.class);
    }
}
