package com.ranny.fotografia.agenda.api

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.pacote.model.PromocaoPacote
import com.ranny.fotografia.pacote.repository.PacoteRepository
import com.ranny.fotografia.shared.model.PagamentoStatus
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

import java.time.OffsetDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/** Spec de integracao HTTP da agenda. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgendaControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    AgendamentoRepository agendamentoRepository

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PacoteRepository pacoteRepository

    def "cria agendamento congelando condicoes do pacote e calculando entrada"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository, "Gestante", new BigDecimal("1000.00"), 20, 0,
                new BigDecimal("50.00"),
                PromocaoPacote.de(true, new BigDecimal("800.00"), null, "Promo"))
        def inicio = OffsetDateTime.parse("2026-12-10T10:00:00-03:00")
        def corpo = [
                clienteId    : cliente.id,
                pacoteId     : pacote.id,
                dataHoraInicio: inicio.toString(),
                local        : "Estúdio Teste"
        ]

        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.valorAcordado').value(800.0d))
                .andExpect(jsonPath('$.valorEntrada').value(240.0d))
                .andExpect(jsonPath('$.saldoRestante').value(560.0d))
                .andExpect(jsonPath('$.status').value("reservado"))
                .andExpect(jsonPath('$.pagamentoEntrada.status').value("pendente"))
                .andExpect(jsonPath('$.condicoesPacote.quantidadeFotosInclusas').value(20))
                .andExpect(jsonPath('$.condicoesPacote.valorTabela').value(1000.0d))
                .andExpect(jsonPath('$.condicoesPacote.promocaoAplicada.valorPromocional').value(800.0d))
                .andExpect(jsonPath('$.dataHoraFim').value("2026-12-10T14:30:00Z"))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).id
    }

    def "respeita a decisao de nao aplicar a promocao no agendamento"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository, "Gestante", new BigDecimal("1000.00"), 20, 0,
                new BigDecimal("50.00"),
                PromocaoPacote.de(true, new BigDecimal("800.00"), null, "Promo"))

        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        clienteId       : cliente.id,
                        pacoteId        : pacote.id,
                        dataHoraInicio  : "2026-12-10T10:00:00-03:00",
                        local           : "Estúdio Teste",
                        valorAcordado   : 1000.00,
                        percentualEntrada: 30.00,
                        aplicarPromocao : false
                ])))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.condicoesPacote.promocaoAplicada').doesNotExist())
                .andExpect(jsonPath('$.valorAcordado').value(1000.0d))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).condicoesPacote.valorTabela == 1000.0d
    }

    def "nao altera condicoes congeladas quando o catalogo muda depois da venda"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository, "Família", new BigDecimal("1000.00"), 20, 0)
        def criado = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/agenda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        clienteId     : cliente.id,
                        pacoteId      : pacote.id,
                        dataHoraInicio: "2026-12-10T10:00:00-03:00",
                        local         : "Estúdio Teste"
                ]))).andReturn())

        when:
        pacote.atualizar("Família Nova", 60, new BigDecimal("500.00"), "Novo", "#000000",
                ["5 fotos"], true, 5, 0, new BigDecimal("99.00"), null)
        pacoteRepository.saveAndFlush(pacote)

        def consultado = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/agenda/${criado.id}")).andReturn())

        then:
        consultado.valorAcordado == 1000.0d
        consultado.condicoesPacote.quantidadeFotosInclusas == 20
        consultado.condicoesPacote.valorTabela == 1000.0d
    }

    @Unroll
    def "rejeita criacao quando #descricao"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def corpo = [
                pacoteId      : pacoteId ?: pacote.id,
                dataHoraInicio: inicio,
                dataHoraFim   : fim,
                local         : local
        ]
        if (usarCliente) {
            corpo.clienteId = cliente.id
        }
        if (contato != null) {
            corpo.contato = contato
        }

        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpo)))
                .andReturn()

        then:
        resultado.response.status == statusEsperado
        JsonCorpo.mapa(resultado).codigo == codigo

        where:
        descricao                    | usarCliente | pacoteId | inicio                       | fim                         | local   | contato | statusEsperado | codigo
        "nao ha cliente nem contato" | false       | null     | "2026-12-10T10:00:00-03:00" | null                        | "Local" | null    | 422            | "CONTATO_OBRIGATORIO"
        "o fim vem antes do inicio"  | true        | null     | "2026-12-10T10:00:00-03:00" | "2026-12-10T09:00:00-03:00" | "Local" | null    | 422            | "PERIODO_INVALIDO"
        "o pacote nao existe"        | true        | "pkg-x"  | "2026-12-10T10:00:00-03:00" | null                        | "Local" | null    | 404            | "RECURSO_NAO_ENCONTRADO"
        "o local esta vazio"         | true        | null     | "2026-12-10T10:00:00-03:00" | null                        | ""      | null    | 422            | "VALIDACAO"
    }

    def "filtra agendamentos paginados por periodo e status"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def marco = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-03-10T10:00:00-03:00"))
        def abril = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-04-10T10:00:00-03:00"), StatusAgendamento.CONFIRMADO)

        when:
        def resultadoPeriodo = mockMvc.perform(get("/api/v1/agenda")
                .param("inicio", "2026-03-01T00:00:00-03:00")
                .param("fim", "2026-03-31T23:59:59-03:00"))
                .andReturn()
        def resultadoStatus = mockMvc.perform(get("/api/v1/agenda").param("status", "confirmado"))
                .andReturn()
        def porPeriodo = JsonCorpo.mapa(resultadoPeriodo)
        def porStatus = JsonCorpo.mapa(resultadoStatus)

        then:
        porPeriodo.itens.size() == 1
        porPeriodo.itens[0].id == marco.id
        porPeriodo.totalItens == 1
        porStatus.itens.size() == 1
        porStatus.itens[0].id == abril.id
    }

    def "lista o calendario por periodo obrigatorio"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def marco = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-03-10T10:00:00-03:00"))

        when:
        def resultado = mockMvc.perform(get("/api/v1/agenda/calendario")
                .param("inicio", "2026-03-01T00:00:00-03:00")
                .param("fim", "2026-03-31T23:59:59-03:00"))
                .andReturn()

        then:
        resultado.response.status == 200
        def json = JsonCorpo.lista(resultado)
        json.size() == 1
        json[0].id == marco.id
    }

    def "calendario exige periodo completo"() {
        when:
        def resultado = mockMvc.perform(get("/api/v1/agenda/calendario")).andReturn()

        then:
        resultado.response.status == 400
        JsonCorpo.mapa(resultado).codigo == "PERIODO_OBRIGATORIO"
    }

    def "pagina agendamentos com metadados"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        3.times { indice ->
            Cenario.agendamento(agendamentoRepository, cliente, pacote,
                    OffsetDateTime.parse("2026-03-${10 + indice}T10:00:00-03:00"))
        }

        when:
        def primeira = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/agenda")
                .param("pagina", "0").param("tamanho", "2")).andReturn())
        def segunda = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/agenda")
                .param("pagina", "1").param("tamanho", "2")).andReturn())

        then:
        primeira.itens.size() == 2
        primeira.totalItens == 3
        primeira.totalPaginas == 2
        primeira.temProxima
        segunda.itens.size() == 1
        segunda.temAnterior
    }

    @Unroll
    def "rejeita paginacao de agenda quando #descricao"() {
        when:
        def resultado = mockMvc.perform(get("/api/v1/agenda")
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

    @Unroll
    def "transicao de status #de -> #para responde #esperado"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                Cenario.amanhaAsDez(), StatusAgendamento.valueOf(de))

        when:
        def resultado = mockMvc.perform(patch("/api/v1/agenda/${agendamento.id}/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([status: para.toLowerCase()])))
                .andReturn()

        then:
        resultado.response.status == esperado
        if (esperado == 200) {
            JsonCorpo.mapa(resultado).status == para.toLowerCase()
        }

        where:
        de         | para       | esperado
        "RESERVADO"| "CONFIRMADO"| 200
        "CONFIRMADO"| "REALIZADO"| 200
        "REALIZADO"| "CANCELADO" | 422
        "CANCELADO"| "RESERVADO" | 200
    }

    def "confirma entrada e registra saldo final"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote)

        when:
        def entrada = mockMvc.perform(put("/api/v1/agenda/${agendamento.id}/pagamento-entrada")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 300.00, percentual: 30.00, status: "confirmado"])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.pagamentoEntrada.status').value("confirmado"))
                .andExpect(jsonPath('$.pagamentoEntrada.valor').value(300.0d))
                .andExpect(jsonPath('$.pagamentoEntrada.confirmadoPor').value("Equipe do estudio"))
                .andReturn()

        def saldo = mockMvc.perform(put("/api/v1/agenda/${agendamento.id}/pagamento-final")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 700.00, status: "confirmado", formaPagamento: "dinheiro"])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.pagamentoFinal.status').value("confirmado"))
                .andExpect(jsonPath('$.pagamentoFinal.formaPagamento').value("dinheiro"))
                .andExpect(jsonPath('$.statusEntrada').value("confirmado"))
                .andExpect(jsonPath('$.statusSaldoFinal').value("confirmado"))
                .andExpect(jsonPath('$.entradaQuitada').value(true))
                .andExpect(jsonPath('$.saldoQuitado').value(true))
                .andExpect(jsonPath('$.quitado').value(true))
                .andExpect(jsonPath('$.valorRecebido').value(1000.0d))
                .andExpect(jsonPath('$.valorPendente').value(0.0d))
                .andReturn()

        then:
        JsonCorpo.mapa(entrada).saldoRestante == 700.0d
        JsonCorpo.mapa(saldo).pagamentoFinal.recebidoEm
        JsonCorpo.mapa(saldo).status == "confirmado"
    }

    def "rejeita saldo final parcial no agendamento"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote)
        mockMvc.perform(put("/api/v1/agenda/${agendamento.id}/pagamento-entrada")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 300.00, percentual: 30.00, status: "confirmado"])))
                .andExpect(status().isOk())

        when:
        def resultado = mockMvc.perform(put("/api/v1/agenda/${agendamento.id}/pagamento-final")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 100.00, status: "confirmado", formaPagamento: "dinheiro"])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "SALDO_FINAL_INTEGRAL_OBRIGATORIO"
        agendamentoRepository.findById(agendamento.id).get().pagamentoFinal.status == PagamentoStatus.PENDENTE
    }

    def "bloqueia saldo final enquanto a entrada esta pendente"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote)

        when:
        def resultado = mockMvc.perform(put("/api/v1/agenda/${agendamento.id}/pagamento-final")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 700.00, status: "confirmado", formaPagamento: "dinheiro"])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "ENTRADA_PENDENTE"
        agendamentoRepository.findById(agendamento.id).get().pagamentoFinal.status == PagamentoStatus.PENDENTE
    }

    def "promove para realizado quando a data do ensaio ja passou"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def inicio = OffsetDateTime.now().minusDays(1).withHour(10).withMinute(0)
                .withSecond(0).withNano(0)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote, inicio)
        mockMvc.perform(put("/api/v1/agenda/${agendamento.id}/pagamento-entrada")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 300.00, percentual: 30.00, status: "confirmado"])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.status').value("confirmado"))

        when:
        def saldo = mockMvc.perform(put("/api/v1/agenda/${agendamento.id}/pagamento-final")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 700.00, status: "confirmado", formaPagamento: "dinheiro"])))
                .andExpect(status().isOk())
                .andReturn()

        then:
        JsonCorpo.mapa(saldo).status == "realizado"
        agendamentoRepository.findById(agendamento.id).get().status == StatusAgendamento.REALIZADO
    }

    def "impede excluir agendamento com contrato vinculado"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote)
        agendamento.vincularContrato("ct-teste")
        agendamentoRepository.saveAndFlush(agendamento)

        when:
        def resultado = mockMvc.perform(delete("/api/v1/agenda/${agendamento.id}")).andReturn()

        then:
        resultado.response.status == 409
        JsonCorpo.mapa(resultado).codigo == "AGENDAMENTO_COM_CONTRATO"
    }

    def "exclui agendamento sem contrato"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote)

        expect:
        mockMvc.perform(delete("/api/v1/agenda/${agendamento.id}")).andExpect(status().isNoContent())
        agendamentoRepository.findById(agendamento.id).isEmpty()
    }
}
