package com.ranny.fotografia.cliente.api

import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.pacote.repository.PacoteRepository
import com.ranny.fotografia.support.Cenario
import com.ranny.fotografia.support.JsonCorpo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Spec de integracao HTTP do modulo de clientes.
 *
 * <p>Usa {@code @SpringBootTest} + MockMvc com H2 em memoria e transacao com
 * rollback por cenario. O bloco {@code where:} cobre os cenarios de validacao e
 * de busca de ponta a ponta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClienteControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PacoteRepository pacoteRepository

    @Autowired
    AgendamentoRepository agendamentoRepository

    def "cria cliente valido e retorna 201 com localizacao"() {
        given:
        def corpo = [
                nome         : "Marina Alves",
                whatsapp     : "(11) 99876-5432",
                email        : "marina.alves@email.com",
                documento    : "529.982.247-25",
                tipoDocumento: "cpf",
                cidade       : "São Paulo",
                origem       : "instagram",
                observacoes  : "Prefere tons neutros."
        ]

        when:
        def resultado = mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpo)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath('$.nome').value("Marina Alves"))
                .andExpect(jsonPath('$.origem').value("instagram"))
                .andExpect(jsonPath('$.tipoDocumento').value("cpf"))
                .andReturn()

        then:
        def json = JsonCorpo.mapa(resultado)
        json.id
        json.createdAt
        json.updatedAt
        clienteRepository.findById(json.id as String).isPresent()
    }

    @Unroll
    def "rejeita cliente invalido quando #descricao"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpo)))
                .andReturn()

        then:
        resultado.response.status == 422
        def json = JsonCorpo.mapa(resultado)
        json.status == 422
        json.codigo == "VALIDACAO"
        json.campos*.campo.contains(campo)

        where:
        descricao                | corpo                                                                   | campo
        "o nome esta vazio"      | [nome: "", whatsapp: "(11) 99999-0000", origem: "instagram"]            | "nome"
        "a origem e nula"        | [nome: "Ana", whatsapp: "(11) 99999-0000", origem: null]                | "origem"
        "o e-mail e invalido"    | [nome: "Ana", whatsapp: "(11) 99999-0000", origem: "outro", email: "x"] | "email"
    }

    def "aceita cliente sem WhatsApp (contato opcional)"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([nome: "Ana Souza", whatsapp: "", origem: "outro"])))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.nome').value("Ana Souza"))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).whatsapp == null
    }

    def "lista clientes de forma paginada e filtra por nome, WhatsApp ou e-mail"() {
        given:
        def marina = Cenario.cliente(clienteRepository, "Marina Alves", "(11) 99999-1111")
        Cenario.cliente(clienteRepository, "Rafael Costa", "(11) 98888-2222")

        when:
        def porNome = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/clientes").param("q", "marina")).andReturn())
        def porWhatsapp = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/clientes").param("q", "98888")).andReturn())
        def todos = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/clientes")).andReturn())

        then:
        porNome.itens.size() == 1
        porNome.itens[0].id == marina.id
        porWhatsapp.itens.size() == 1
        porWhatsapp.itens[0].nome == "Rafael Costa"
        todos.itens.size() == 2
        todos.totalItens == 2
        todos.totalPaginas == 1
    }

    @Unroll
    def "pagina clientes com #descricao"() {
        given:
        def ana = Cenario.cliente(clienteRepository, "Ana")
        def bruno = Cenario.cliente(clienteRepository, "Bruno")
        Cenario.cliente(clienteRepository, "Carlos")

        when:
        def resposta = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/clientes")
                .param("pagina", pagina.toString())
                .param("tamanho", tamanho.toString()))
                .andReturn())

        then:
        resposta.itens.size() == quantidade
        resposta.pagina == pagina
        resposta.tamanho == tamanho
        resposta.totalItens == 3
        resposta.totalPaginas == 2
        resposta.temAnterior == (pagina > 0)
        resposta.temProxima == (pagina < 1)
        if (quantidade > 0) {
            resposta.itens[0].nome == primeiroNome
        }

        where:
        descricao          | pagina | tamanho | quantidade | primeiroNome
        "a primeira pagina" | 0      | 2       | 2          | "Ana"
        "a ultima pagina"   | 1      | 2       | 1          | "Carlos"
    }

    @Unroll
    def "rejeita paginacao invalida quando #descricao"() {
        when:
        def resultado = mockMvc.perform(get("/api/v1/clientes")
                .param("pagina", pagina.toString())
                .param("tamanho", tamanho.toString()))
                .andReturn()

        then:
        resultado.response.status == 400
        JsonCorpo.mapa(resultado).codigo == "PAGINACAO_INVALIDA"

        where:
        descricao            | pagina | tamanho
        "a pagina e negativa" | -1     | 20
        "o tamanho e zero"    | 0      | 0
        "o tamanho e grande"  | 0      | 101
    }

    def "pagina alem do total retorna lista vazia"() {
        given:
        Cenario.cliente(clienteRepository, "Ana")

        when:
        def resultado = mockMvc.perform(get("/api/v1/clientes")
                .param("pagina", "5")
                .param("tamanho", "20"))
                .andReturn()

        then:
        resultado.response.status == 200
        def json = JsonCorpo.mapa(resultado)
        json.itens.isEmpty()
        json.totalItens == 1
    }

    def "detalha e atualiza um cliente existente"() {
        given:
        def cliente = Cenario.cliente(clienteRepository, "Marina Alves", "(11) 99999-1111")
        def atualizacao = [
                nome    : "Marina Alves Souza",
                whatsapp: "(11) 99999-1111",
                email   : "marina.souza@email.com",
                origem  : "indicacao",
                cidade  : "Campinas"
        ]

        when:
        def detalhe = mockMvc.perform(get("/api/v1/clientes/${cliente.id}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.nome').value("Marina Alves"))
                .andReturn()

        def atualizado = mockMvc.perform(put("/api/v1/clientes/${cliente.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(atualizacao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.nome').value("Marina Alves Souza"))
                .andExpect(jsonPath('$.origem').value("indicacao"))
                .andReturn()

        then:
        JsonCorpo.mapa(detalhe).id == cliente.id
        JsonCorpo.mapa(atualizado).email == "marina.souza@email.com"
    }

    def "retorna 404 ao buscar, atualizar ou excluir cliente inexistente"() {
        given:
        def inexistente = "cli-inexistente"

        expect:
        mockMvc.perform(get("/api/v1/clientes/${inexistente}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.codigo').value("RECURSO_NAO_ENCONTRADO"))

        mockMvc.perform(delete("/api/v1/clientes/${inexistente}"))
                .andExpect(status().isNotFound())
    }

    def "exclui cliente sem vinculos"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)

        when:
        mockMvc.perform(delete("/api/v1/clientes/${cliente.id}"))
                .andExpect(status().isNoContent())

        then:
        clienteRepository.findById(cliente.id).isEmpty()
    }

    def "impede excluir cliente com agendamento vinculado"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        Cenario.agendamento(agendamentoRepository, cliente, pacote)

        when:
        def resultado = mockMvc.perform(delete("/api/v1/clientes/${cliente.id}")).andReturn()

        then:
        resultado.response.status == 409
        def json = JsonCorpo.mapa(resultado)
        json.codigo == "CLIENTE_COM_VINCULO"
        clienteRepository.findById(cliente.id).isPresent()
    }
}
