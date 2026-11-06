package com.ranny.fotografia.contrato.model.converter;

import com.ranny.fotografia.contrato.model.documento.ContratoEvento;
import com.ranny.fotografia.shared.json.JsonApoio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

import java.util.List;

/** PATTERN: Adapter JPA para o historico append-only do contrato. */
@Converter
public class HistoricoContratoConverter implements AttributeConverter<List<ContratoEvento>, String> {

    private static final TypeReference<List<ContratoEvento>> TIPO = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<ContratoEvento> atributo) {
        return JsonApoio.escrever(atributo);
    }

    @Override
    public List<ContratoEvento> convertToEntityAttribute(String coluna) {
        List<ContratoEvento> eventos = JsonApoio.ler(coluna, TIPO);
        return eventos == null ? List.of() : List.copyOf(eventos);
    }
}
