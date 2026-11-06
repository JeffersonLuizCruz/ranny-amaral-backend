package com.ranny.fotografia.shared.json;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * PATTERN: Utility / Adapter para serializacao de documentos JSON.
 *
 * <p>Alguns dados de contrato sao documentos imutaveis e grandes (snapshot,
 * clausulas, historico, aceite). Modela-los em dezenas de tabelas produziria
 * joins sem valor e um schema instavel. O projeto os guarda como JSON/CLOB,
 * mantendo em colunas relacionais apenas o que precisa ser consultado.
 *
 * <p>Os {@code AttributeConverter}s do JPA sao instanciados pelo Hibernate, nao
 * pelo Spring; por isso este adaptador usa uma instancia estatica propria. A
 * composicao (converter delega ao adaptador) evita heranca e concentra a
 * configuracao de serializacao em um unico ponto.
 */
public final class JsonApoio {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private JsonApoio() {
    }

    public static String escrever(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(valor);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Falha ao serializar documento JSON", e);
        }
    }

    public static <T> T ler(String json, Class<T> tipo) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, tipo);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Falha ao desserializar documento JSON", e);
        }
    }

    public static <T> T ler(String json, TypeReference<T> tipo) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, tipo);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Falha ao desserializar documento JSON", e);
        }
    }
}
