package com.ranny.fotografia.support

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.test.web.servlet.MvcResult

/**
 * Utilitario de teste (composicao, sem heranca).
 *
 * <p>Concentra a leitura de JSON das respostas do MockMvc para que os specs
 * expressem apenas o comportamento de negocio.
 */
final class JsonCorpo {

    private JsonCorpo() {
    }

    static Map mapa(MvcResult resultado) {
        return new JsonSlurper().parseText(resultado.response.contentAsString) as Map
    }

    static List lista(MvcResult resultado) {
        return new JsonSlurper().parseText(resultado.response.contentAsString) as List
    }

    static String json(Map valor) {
        return JsonOutput.toJson(valor)
    }
}
