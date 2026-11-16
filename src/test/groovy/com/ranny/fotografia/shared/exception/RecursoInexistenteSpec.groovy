package com.ranny.fotografia.shared.exception

import com.ranny.fotografia.support.JsonCorpo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecursoInexistenteSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    def "rota inexistente responde 404 no contrato ProblemDetail"() {
        when:
        def resultado = mockMvc.perform(get("/rota-que-nao-existe")
                .header("User-Agent", "Mozilla/5.0"))
                .andReturn()

        then:
        resultado.response.status == 404
        JsonCorpo.mapa(resultado).codigo == "NAO_ENCONTRADO"
    }
}
