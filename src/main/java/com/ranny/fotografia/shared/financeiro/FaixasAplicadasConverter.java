package com.ranny.fotografia.shared.financeiro;

import com.ranny.fotografia.shared.json.JsonApoio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

import java.util.List;

/** Persiste o detalhamento progressivo do pedido de extras como JSON. */
@Converter
public class FaixasAplicadasConverter implements AttributeConverter<List<FaixaAplicada>, String> {

    private static final TypeReference<List<FaixaAplicada>> TIPO = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<FaixaAplicada> atributo) {
        return JsonApoio.escrever(atributo);
    }

    @Override
    public List<FaixaAplicada> convertToEntityAttribute(String coluna) {
        List<FaixaAplicada> faixas = JsonApoio.ler(coluna, TIPO);
        return faixas == null ? List.of() : List.copyOf(faixas);
    }
}
