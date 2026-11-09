package com.ranny.fotografia.shared.exception

import com.ranny.fotografia.support.JsonCorpo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Garante que o contrato global de erro segue RFC 7807/9457.
 *
 * <p>Cobre um erro de dominio (404) e um erro de validacao (422), incluindo as
 * extensoes de compatibilidade usadas pelo frontend.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProblemaHttpSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    def "erro de recurso nao encontrado segue Problem Details"() {
        when:
        def resultado = mockMvc.perform(get("/api/v1/clientes/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn()

        then:
        def json = JsonCorpo.mapa(resultado)
        json.type == "urn:ranny:fotografia:problema:recurso-nao-encontrado"
        json.title == "Not Found"
        json.status == 404
        json.detail
        json.instance == "/api/v1/clientes/inexistente"
        json.codigo == "RECURSO_NAO_ENCONTRADO"
        json.timestamp
    }

    def "erro de validacao segue Problem Details e preserva campos"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn()

        then:
        def json = JsonCorpo.mapa(resultado)
        json.type == "urn:ranny:fotografia:problema:validacao"
        json.title == "Unprocessable Entity"
        json.status == 422
        json.codigo == "VALIDACAO"
        json.campos instanceof List
        !json.campos.isEmpty()
    }
}
