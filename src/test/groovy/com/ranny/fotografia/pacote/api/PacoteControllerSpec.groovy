package com.ranny.fotografia.pacote.api

import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.pacote.model.PromocaoPacote
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

/** Spec de integracao HTTP do catalogo de pacotes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PacoteControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    PacoteRepository pacoteRepository

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    AgendamentoRepository agendamentoRepository

    def "cria pacote e infere quantidades a partir dos itens inclusos"() {
        given:
        def corpo = [
                nome       : "Ensaio Corporativo",
                duracaoMinutos: 120,
                valor      : 1490.00,
                descricao  : "Retratos profissionais.",
                corHex     : "#0284C7",
                itensInclusos: ["2 looks", "40 fotos tratadas", "1 vídeo institucional curto"],
                ativo      : true
        ]

        when:
        def resultado = mockMvc.perform(post("/api/v1/pacotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.quantidadeFotosInclusas').value(40))
                .andExpect(jsonPath('$.quantidadeVideosInclusos').value(1))
                .andExpect(jsonPath('$.valorFotoExtra').value(0))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).id
    }

    def "cria pacote com promocao e valores explicitos"() {
        given:
        def corpo = [
                nome                  : "Retrato Promocional",
                duracaoMinutos        : 45,
                valor                 : 300.00,
                descricao             : "Retratos com valor fechado.",
                corHex                : "#E11D48",
                itensInclusos         : ["20 fotos tratadas"],
                quantidadeFotosInclusas: 20,
                quantidadeVideosInclusos: 0,
                valorFotoExtra        : 15.00,
                ativo                 : true,
                promocao              : [ativa: true, valorPromocional: 200.00, validaAte: "2030-12-31", rotulo: "Promo de lançamento"]
        ]

        when:
        def resultado = mockMvc.perform(post("/api/v1/pacotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.promocao.ativa').value(true))
                .andExpect(jsonPath('$.promocao.valorPromocional').value(200.0d))
                .andExpect(jsonPath('$.promocao.validaAte').value("2030-12-31"))
                .andExpect(jsonPath('$.valorFotoExtra').value(15.0d))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).quantidadeVideosInclusos == 0
    }

    def "aceita pacote com descricao vazia"() {
        given:
        def corpo = [
                nome         : "Pacote sem descrição",
                duracaoMinutos: 60,
                valor        : 100.00,
                descricao    : "",
                corHex       : "#7C3AED",
                itensInclusos: []
        ]

        expect:
        mockMvc.perform(post("/api/v1/pacotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.descricao').value(""))
    }

    @Unroll
    def "rejeita pacote invalido quando #descricao"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/pacotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpo)))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).campos*.campo.contains(campo)

        where:
        descricao                 | corpo                                                                                     | campo
        "o nome esta vazio"       | [nome: "", duracaoMinutos: 60, valor: 100, descricao: "d", corHex: "#fff", itensInclusos: []]         | "nome"
        "a duracao e zero"        | [nome: "P", duracaoMinutos: 0, valor: 100, descricao: "d", corHex: "#fff", itensInclusos: []]          | "duracaoMinutos"
        "o valor e negativo"      | [nome: "P", duracaoMinutos: 60, valor: -1, descricao: "d", corHex: "#fff", itensInclusos: []]          | "valor"
        "a lista de itens e nula" | [nome: "P", duracaoMinutos: 60, valor: 100, descricao: "d", corHex: "#fff", itensInclusos: null]       | "itensInclusos"
    }

    def "alterna a situacao ativo do pacote"() {
        given:
        def pacote = Cenario.pacote(pacoteRepository)

        expect:
        mockMvc.perform(patch("/api/v1/pacotes/${pacote.id}/ativo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.ativo').value(false))
    }

    def "filtra pacotes por situacao e busca textual"() {
        given:
        def ativo = Cenario.pacote(pacoteRepository, "Ativo")
        def inativo = Cenario.pacote(pacoteRepository, "Inativo")
        inativo.alternarAtivo()

        when:
        def ativos = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/pacotes").param("ativo", "true")).andReturn())
        def inativos = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/pacotes").param("ativo", "false")).andReturn())
        def busca = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/pacotes").param("q", "inativ")).andReturn())

        then:
        ativos.itens*.id == [ativo.id]
        inativos.itens*.id == [inativo.id]
        busca.itens*.id == [inativo.id]
        busca.totalItens == 1
    }

    def "pagina pacotes com metadados corretos"() {
        given:
        Cenario.pacote(pacoteRepository, "A")
        Cenario.pacote(pacoteRepository, "B")
        Cenario.pacote(pacoteRepository, "C")

        when:
        def primeira = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/pacotes")
                .param("pagina", "0").param("tamanho", "2")).andReturn())
        def ultima = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/pacotes")
                .param("pagina", "1").param("tamanho", "2")).andReturn())

        then:
        primeira.itens*.nome == ["A", "B"]
        primeira.totalItens == 3
        primeira.totalPaginas == 2
        primeira.temProxima
        ultima.itens*.nome == ["C"]
        ultima.temAnterior
    }

    @Unroll
    def "rejeita paginacao de pacotes quando #descricao"() {
        when:
        def resultado = mockMvc.perform(get("/api/v1/pacotes")
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

    def "impede excluir pacote com agendamento vinculado"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        Cenario.agendamento(agendamentoRepository, cliente, pacote)

        when:
        def resultado = mockMvc.perform(delete("/api/v1/pacotes/${pacote.id}")).andReturn()

        then:
        resultado.response.status == 409
        JsonCorpo.mapa(resultado).codigo == "PACOTE_COM_VINCULO"
    }

    def "exclui pacote sem vinculos"() {
        given:
        def pacote = Cenario.pacote(pacoteRepository)

        expect:
        mockMvc.perform(delete("/api/v1/pacotes/${pacote.id}")).andExpect(status().isNoContent())
        pacoteRepository.findById(pacote.id).isEmpty()
    }
}
