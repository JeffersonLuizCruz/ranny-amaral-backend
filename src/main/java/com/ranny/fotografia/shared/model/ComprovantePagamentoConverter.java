package com.ranny.fotografia.shared.model;

import com.ranny.fotografia.shared.json.JsonApoio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * PATTERN: Adapter (JPA AttributeConverter) compondo {@link JsonApoio}.
 *
 * <p>Isola o formato JSON do comprovante do restante do dominio: a entidade ve
 * um {@link ComprovantePagamento}, o banco ve texto. Trocar o formato de
 * persistencia no futuro exige alterar apenas este adaptador.
 */
@Converter
public class ComprovantePagamentoConverter implements AttributeConverter<ComprovantePagamento, String> {

    @Override
    public String convertToDatabaseColumn(ComprovantePagamento atributo) {
        return JsonApoio.escrever(atributo);
    }

    @Override
    public ComprovantePagamento convertToEntityAttribute(String coluna) {
        return JsonApoio.ler(coluna, ComprovantePagamento.class);
    }
}
