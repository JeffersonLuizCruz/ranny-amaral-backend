package com.ranny.fotografia.contrato.model.converter;

import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import com.ranny.fotografia.shared.json.JsonApoio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

import java.util.List;

/**
 * PATTERN: Adapter JPA (AttributeConverter) para a lista de clausulas.
 *
 * <p>O converter delega ao {@link JsonApoio} (composicao), sem herdar uma base
 * generica. Cada documento tem o seu adapter explicito, o que facilita evoluir
 * o formato de um tipo sem afetar os demais.
 */
@Converter
public class ContratoClausulasConverter implements AttributeConverter<List<ContratoClausula>, String> {

    private static final TypeReference<List<ContratoClausula>> TIPO = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<ContratoClausula> atributo) {
        return JsonApoio.escrever(atributo);
    }

    @Override
    public List<ContratoClausula> convertToEntityAttribute(String coluna) {
        List<ContratoClausula> clausulas = JsonApoio.ler(coluna, TIPO);
        return clausulas == null ? List.of() : List.copyOf(clausulas);
    }
}
