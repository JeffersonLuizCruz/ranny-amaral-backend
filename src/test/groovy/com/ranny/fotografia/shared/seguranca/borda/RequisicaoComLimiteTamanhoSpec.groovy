package com.ranny.fotografia.shared.seguranca.borda

import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

/**
 * Spec unitario do Decorator de limite de payload.
 *
 * <p>Cobre o caminho que o MockMvc nao consegue forcar de forma deterministica:
 * leitura real do stream sem depender do Content-Length declarado.
 */
class RequisicaoComLimiteTamanhoSpec extends Specification {

    def "interrompe a leitura quando o corpo excede o limite"() {
        given:
        def request = new MockHttpServletRequest()
        request.setContent(("x" * 2048).bytes)
        def wrapper = new RequisicaoComLimiteTamanho(request, 1024)

        when:
        wrapper.inputStream.readAllBytes()

        then:
        thrown(LimitePayloadExcedidoException)
    }

    def "permite leitura exatamente no limite"() {
        given:
        def request = new MockHttpServletRequest()
        request.setContent(("x" * 1024).bytes)
        def wrapper = new RequisicaoComLimiteTamanho(request, 1024)

        expect:
        wrapper.inputStream.readAllBytes().length == 1024
    }
}
