package com.ranny.fotografia.shared.seguranca.borda

import com.ranny.fotografia.support.JsonCorpo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.RequestPostProcessor
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * Spec de integracao do filtro de borda.
 *
 * <p>O contexto Spring real e carregado, a cadeia do Security e exercitada e
 * apenas o avaliador de IP e substituido. As regras de User-Agent,
 * Content-Type, tamanho e scanning ficam ligadas via {@code @TestPropertySource}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ConfiguracaoTesteBorda)
@TestPropertySource(properties = [
        "app.seguranca.borda.exigir-user-agent=true",
        "app.seguranca.borda.exigir-content-type-em-mutacoes=true",
        "app.seguranca.borda.ip-check-habilitado=true",
        "app.seguranca.borda.bloquear-padroes-scanning=true",
        "app.seguranca.borda.max-payload-bytes=4096"
])
class FiltroBordaSegurancaSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Unroll
    def "borda responde #statusEsperado quando #cenario"() {
        given:
        def request = requisicao(metodo, caminho, headers, tipoConteudo, corpo, ipRemoto)

        when:
        def resultado = mockMvc.perform(request).andReturn()

        then:
        resultado.response.status == statusEsperado
        if (statusEsperado >= 400) {
            assert resultado.response.contentType != null
            assert resultado.response.contentType.startsWith("application/problem+json")
            def json = JsonCorpo.mapa(resultado)
            assert json.status == statusEsperado
            assert json.codigo == codigoEsperado
            assert json.instance == caminho
            assert json.timestamp
        }

        where:
        cenario                   | metodo | caminho            | headers                       | tipoConteudo       | corpo          | ipRemoto   | statusEsperado | codigoEsperado
        "requisicao valida"       | "GET"  | "/api/v1/clientes" | ["User-Agent": "Mozilla/5.0"] | null               | null           | null       | 200            | null
        "sem user-agent"          | "GET"  | "/api/v1/clientes" | ["User-Agent": ""]            | null               | null           | null       | 400            | "HEADER_OBRIGATORIO"
        "scanner sqlmap"          | "GET"  | "/api/v1/clientes" | ["User-Agent": "sqlmap/1.8"]  | null               | null           | null       | 403            | "PADRAO_MALICIOSO"
        "payload acima do limite" | "POST" | "/api/v1/clientes" | ["User-Agent": "Mozilla/5.0"] | "application/json" | ("x" * 5000)   | null       | 413            | "PAYLOAD_EXCEDIDO"
        "content-type ausente"    | "POST" | "/api/v1/clientes" | ["User-Agent": "Mozilla/5.0"] | null               | "{}"           | null       | 415            | "CONTENT_TYPE_OBRIGATORIO"
        "content-type invalido"   | "POST" | "/api/v1/clientes" | ["User-Agent": "Mozilla/5.0"] | "text/plain"       | "{}"           | null       | 415            | "CONTENT_TYPE_INVALIDO"
        "path de scanning"        | "GET"  | "/.env"            | ["User-Agent": "Mozilla/5.0"] | null               | null           | null       | 403            | "PADRAO_MALICIOSO"
        "ip bloqueado"            | "GET"  | "/api/v1/clientes" | ["User-Agent": "Mozilla/5.0"] | null               | null           | "10.9.9.9" | 403            | "ACESSO_NEGADO"
    }

    private MockHttpServletRequestBuilder requisicao(String metodo,
                                                     String caminho,
                                                     Map<String, String> headers,
                                                     String tipoConteudo,
                                                     String corpo,
                                                     String ipRemoto) {
        MockHttpServletRequestBuilder builder
        if (metodo == "GET") {
            builder = get(caminho)
        } else if (metodo == "POST") {
            builder = post(caminho)
        } else {
            throw new IllegalArgumentException("Metodo nao suportado no teste: " + metodo)
        }

        headers.each { chave, valor -> builder.header(chave, valor) }
        if (tipoConteudo != null) {
            builder.contentType(tipoConteudo)
        }
        if (corpo != null) {
            builder.content(corpo)
        }
        if (ipRemoto != null) {
            builder.with(new RequestPostProcessor() {
                @Override
                MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                    request.setRemoteAddr(ipRemoto)
                    return request
                }
            })
        }
        return builder
    }
}
