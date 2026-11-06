package com.ranny.fotografia.financeiro.api

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.contrato.repository.EstudioConfigRepository
import com.ranny.fotografia.contrato.repository.ModeloContratoRepository
import com.ranny.fotografia.galeria.model.Galeria
import com.ranny.fotografia.galeria.model.PedidoExtras
import com.ranny.fotografia.galeria.repository.GaleriaRepository
import com.ranny.fotografia.galeria.repository.PedidoExtrasRepository
import com.ranny.fotografia.pacote.repository.PacoteRepository
import com.ranny.fotografia.shared.model.PagamentoEntrada
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

import java.time.Instant
import java.time.OffsetDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FinanceiroControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PacoteRepository pacoteRepository

    @Autowired
    AgendamentoRepository agendamentoRepository

    @Autowired
    ModeloContratoRepository modeloRepository

    @Autowired
    EstudioConfigRepository estudioRepository

    @Autowired
    GaleriaRepository galeriaRepository

    @Autowired
    PedidoExtrasRepository extrasRepository

    def "consolida o mes, ignora cancelados e outros meses e inclui extras pendentes"() {
        given:
        def cliente = Cenario.cliente(clienteRepository, "Marina Financeiro")
        def pacote = Cenario.pacote(pacoteRepository, "Financeiro", new BigDecimal("1000.00"), 1, 0)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-11-12T10:00:00-03:00"), StatusAgendamento.REALIZADO)
        agendamento.atualizarPagamentoEntrada(
                new BigDecimal("300.00"), new BigDecimal("30.00"),
                PagamentoEntrada.confirmada(new BigDecimal("300.00"), new BigDecimal("30.00"),
                        Instant.now(), "Teste"))
        agendamentoRepository.saveAndFlush(agendamento)

        def galeria = Galeria.criar("estudio", agendamento.id, null, cliente.id,
                cliente.nome, 1, 0, new BigDecimal("50.00"))
        galeriaRepository.save(galeria)
        extrasRepository.save(PedidoExtras.criar(
                galeria.id, "sel-fin-1", 1, new BigDecimal("50.00"), new BigDecimal("50.00")))

        Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-11-20T10:00:00-03:00"), StatusAgendamento.CANCELADO)
        Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-12-20T10:00:00-03:00"), StatusAgendamento.REALIZADO)

        when:
        def resultado = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/financeiro/mensal").param("mes", "2026-11"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        resultado.mes == "2026-11"
        resultado.quantidade == 1
        resultado.totalPrevisto == 1050.0d
        resultado.totalRecebido == 300.0d
        resultado.totalAReceber == 750.0d
        resultado.totalExtras == 50.0d
        resultado.totalExtrasPendentes == 50.0d
        resultado.quantidadeSaldosPendentes == 1
        resultado.quantidadeExtrasPendentes == 1
        resultado.entradas.size() == 1
        resultado.entradas[0].clienteNome == "Marina Financeiro"
        resultado.entradas[0].statusEntrada == "confirmado"
        resultado.entradas[0].statusFinal == "pendente"
        resultado.entradas[0].statusExtras == "pendente"
    }

    def "usa os valores do contrato quando existir"() {
        given:
        def cliente = Cenario.cliente(clienteRepository, "Cliente Contrato")
        def pacote = Cenario.pacote(pacoteRepository, "Contrato", new BigDecimal("1000.00"), 1, 0)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.parse("2026-11-15T10:00:00-03:00"), StatusAgendamento.REALIZADO)
        def modelo = Cenario.modelo(modeloRepository)
        Cenario.estudio(estudioRepository)

        mockMvc.perform(post("/api/v1/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        modeloId : modelo.id,
                        clienteId: cliente.id,
                        pacoteId : pacote.id,
                        agendamentoId: agendamento.id,
                        valorTotal: 2000.00,
                        percentualEntrada: 30.00
                ])))
                .andExpect(status().isCreated())

        when:
        def resultado = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/financeiro/mensal").param("mes", "2026-11"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        resultado.quantidade == 1
        resultado.entradas[0].contratoId != null
        resultado.entradas[0].valorTotal == 2000.0d
        resultado.entradas[0].valorEntrada == 600.0d
        resultado.entradas[0].saldoRestante == 1400.0d
    }

    def "rejeita mes invalido"() {
        when:
        def resultado = mockMvc.perform(
                get("/api/v1/financeiro/mensal").param("mes", "11/2026"))
                .andReturn()

        then:
        resultado.response.status == 400
        JsonCorpo.mapa(resultado).codigo == "MES_INVALIDO"
    }
}
