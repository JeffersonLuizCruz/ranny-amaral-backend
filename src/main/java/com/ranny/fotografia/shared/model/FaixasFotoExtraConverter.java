package com.ranny.fotografia.shared.model;

import com.ranny.fotografia.shared.json.JsonApoio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

import java.util.List;

/** Persiste as faixas de foto extra como JSON no snapshot comercial. */
@Converter
public class FaixasFotoExtraConverter implements AttributeConverter<List<FaixaFotoExtraSnapshot>, String> {

    private static final TypeReference<List<FaixaFotoExtraSnapshot>> TIPO = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<FaixaFotoExtraSnapshot> atributo) {
        return JsonApoio.escrever(atributo);
    }

    @Override
    public List<FaixaFotoExtraSnapshot> convertToEntityAttribute(String coluna) {
        List<FaixaFotoExtraSnapshot> faixas = JsonApoio.ler(coluna, TIPO);
        return faixas == null ? List.of() : List.copyOf(faixas);
    }
}
