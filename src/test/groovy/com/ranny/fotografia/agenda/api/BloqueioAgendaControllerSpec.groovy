package com.ranny.fotografia.agenda.api

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.agenda.repository.BloqueioAgendaRepository
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

import java.time.OffsetDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/** Spec de integracao HTTP dos bloqueios pontuais da agenda. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BloqueioAgendaControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    AgendamentoRepository agendamentoRepository

    @Autowired
    BloqueioAgendaRepository bloqueioRepository

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PacoteRepository pacoteRepository

    def "cria bloqueio de dia inteiro no fuso do estudio"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda/bloqueios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        data      : "2026-12-25",
                        diaInteiro: true,
                        motivo    : "Natal"
                ])))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.diaInteiro').value(true))
                .andExpect(jsonPath('$.data').value("2026-12-25"))
                .andExpect(jsonPath('$.motivo').value("Natal"))
                .andReturn()

        then:
        def resposta = JsonCorpo.mapa(resultado)
        def salvo = bloqueioRepository.findById(resposta.id as String).get()
        salvo.inicio.toInstant() == OffsetDateTime.parse("2026-12-25T00:00:00-03:00").toInstant()
        salvo.fim.toInstant() == OffsetDateTime.parse("2026-12-26T00:00:00-03:00").toInstant()
    }

    def "cria bloqueio de intervalo"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda/bloqueios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        data      : "2026-12-10",
                        diaInteiro: false,
                        horaInicio: "14:00",
                        horaFim   : "18:00",
                        motivo    : "Manutenção"
                ])))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.horaInicio').value("14:00"))
                .andExpect(jsonPath('$.horaFim').value("18:00"))
                .andReturn()

        then:
        def salvo = bloqueioRepository.findById(JsonCorpo.mapa(resultado).id as String).get()
        salvo.inicio.toInstant() == OffsetDateTime.parse("2026-12-10T14:00:00-03:00").toInstant()
        salvo.fim.toInstant() == OffsetDateTime.parse("2026-12-10T18:00:00-03:00").toInstant()
    }

    def "rejeita bloqueio de intervalo sem horarios"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda/bloqueios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        data      : "2026-12-10",
                        diaInteiro: false,
                        motivo    : "Sem horário"
                ])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "HORARIO_OBRIGATORIO"
    }

    def "rejeita bloqueio com fim antes do inicio"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda/bloqueios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        data      : "2026-12-10",
                        diaInteiro: false,
                        horaInicio: "18:00",
                        horaFim   : "14:00",
                        motivo    : "Inválido"
                ])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "PERIODO_INVALIDO"
    }

    def "recusa bloqueio sobre ensaio ativo"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-12-10T10:00:00-03:00"))

        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda/bloqueios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        data      : "2026-12-10",
                        diaInteiro: false,
                        horaInicio: "10:00",
                        horaFim   : "12:00",
                        motivo    : "Conflito"
                ])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "BLOQUEIO_CONFLITA_COM_AGENDAMENTO"
        bloqueioRepository.count() == 0
        agendamentoRepository.findById(agendamento.id).isPresent()
    }

    def "permite bloqueio sobre ensaio cancelado"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-12-10T10:00:00-03:00"), StatusAgendamento.CANCELADO)

        expect:
        mockMvc.perform(post("/api/v1/agenda/bloqueios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        data      : "2026-12-10",
                        diaInteiro: false,
                        horaInicio: "10:00",
                        horaFim   : "12:00",
                        motivo    : "Horário liberado"
                ])))
                .andExpect(status().isCreated())
    }

    def "impede criar ensaio sobre bloqueio"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        Cenario.bloqueio(bloqueioRepository,
                OffsetDateTime.parse("2026-12-10T14:00:00-03:00"),
                OffsetDateTime.parse("2026-12-10T18:00:00-03:00"))

        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        clienteId     : cliente.id,
                        pacoteId      : pacote.id,
                        dataHoraInicio: "2026-12-10T15:00:00-03:00",
                        local         : "Estúdio Teste"
                ])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "HORARIO_BLOQUEADO"
        agendamentoRepository.count() == 0
    }

    def "permite ensaio que termina exatamente no inicio do bloqueio"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        Cenario.bloqueio(bloqueioRepository,
                OffsetDateTime.parse("2026-12-10T11:30:00-03:00"),
                OffsetDateTime.parse("2026-12-10T18:00:00-03:00"))

        expect:
        mockMvc.perform(post("/api/v1/agenda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        clienteId     : cliente.id,
                        pacoteId      : pacote.id,
                        dataHoraInicio: "2026-12-10T10:00:00-03:00",
                        local         : "Estúdio Teste"
                ])))
                .andExpect(status().isCreated())
    }

    def "impede remarcar ensaio para dentro de bloqueio"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-12-10T08:00:00-03:00"))
        Cenario.bloqueio(bloqueioRepository,
                OffsetDateTime.parse("2026-12-10T14:00:00-03:00"),
                OffsetDateTime.parse("2026-12-10T18:00:00-03:00"))

        when:
        def resultado = mockMvc.perform(put("/api/v1/agenda/${agendamento.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        clienteId     : cliente.id,
                        pacoteId      : pacote.id,
                        dataHoraInicio: "2026-12-10T15:00:00-03:00",
                        local         : "Estúdio Teste"
                ])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "HORARIO_BLOQUEADO"
    }

    def "impede mover bloqueio para cima de ensaio ativo"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-12-10T10:00:00-03:00"))
        def bloqueio = Cenario.bloqueio(bloqueioRepository,
                OffsetDateTime.parse("2026-12-11T10:00:00-03:00"),
                OffsetDateTime.parse("2026-12-11T12:00:00-03:00"))

        when:
        def resultado = mockMvc.perform(put("/api/v1/agenda/bloqueios/${bloqueio.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        data      : "2026-12-10",
                        diaInteiro: false,
                        horaInicio: "10:00",
                        horaFim   : "12:00",
                        motivo    : "Mover para conflito"
                ])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "BLOQUEIO_CONFLITA_COM_AGENDAMENTO"
    }

    def "recusa bloqueio sobreposto a outro bloqueio"() {
        given:
        Cenario.bloqueio(bloqueioRepository,
                OffsetDateTime.parse("2026-12-10T10:00:00-03:00"),
                OffsetDateTime.parse("2026-12-10T12:00:00-03:00"))

        when:
        def resultado = mockMvc.perform(post("/api/v1/agenda/bloqueios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        data      : "2026-12-10",
                        diaInteiro: false,
                        horaInicio: "11:00",
                        horaFim   : "13:00",
                        motivo    : "Sobreposto"
                ])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "BLOQUEIO_SOBREPOSTO"
    }

    def "lista bloqueios que intersectam o periodo"() {
        given:
        def dezembro = Cenario.bloqueio(bloqueioRepository,
                OffsetDateTime.parse("2026-12-10T10:00:00-03:00"),
                OffsetDateTime.parse("2026-12-10T12:00:00-03:00"))
        Cenario.bloqueio(bloqueioRepository,
                OffsetDateTime.parse("2026-12-20T00:00:00-03:00"),
                OffsetDateTime.parse("2026-12-21T00:00:00-03:00"), true)

        when:
        def resultado = mockMvc.perform(get("/api/v1/agenda/bloqueios")
                .param("inicio", "2026-12-01T00:00:00-03:00")
                .param("fim", "2026-12-15T23:59:59-03:00"))
                .andReturn()

        then:
        resultado.response.status == 200
        def lista = JsonCorpo.lista(resultado)
        lista.size() == 1
        lista[0].id == dezembro.id
    }

    def "exige periodo completo na listagem"() {
        when:
        def resultado = mockMvc.perform(get("/api/v1/agenda/bloqueios")).andReturn()

        then:
        resultado.response.status == 400
        JsonCorpo.mapa(resultado).codigo == "PERIODO_OBRIGATORIO"
    }

    def "impede reabrir ensaio cancelado dentro de bloqueio"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-12-10T10:00:00-03:00"), StatusAgendamento.CANCELADO)
        Cenario.bloqueio(bloqueioRepository,
                OffsetDateTime.parse("2026-12-10T09:00:00-03:00"),
                OffsetDateTime.parse("2026-12-10T12:00:00-03:00"))

        when:
        def resultado = mockMvc.perform(patch("/api/v1/agenda/${agendamento.id}/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([status: "reservado"])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "HORARIO_BLOQUEADO"
        agendamentoRepository.findById(agendamento.id).get().status == StatusAgendamento.CANCELADO
    }

    def "exclui bloqueio"() {
        given:
        def bloqueio = Cenario.bloqueio(bloqueioRepository,
                OffsetDateTime.parse("2026-12-10T10:00:00-03:00"),
                OffsetDateTime.parse("2026-12-10T12:00:00-03:00"))

        expect:
        mockMvc.perform(delete("/api/v1/agenda/bloqueios/${bloqueio.id}"))
                .andExpect(status().isNoContent())
        bloqueioRepository.findById(bloqueio.id).isEmpty()
    }
}
