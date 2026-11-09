package com.ranny.fotografia.dashboard.api

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.galeria.model.Galeria
import com.ranny.fotografia.galeria.repository.GaleriaRepository
import com.ranny.fotografia.pacote.repository.PacoteRepository
import com.ranny.fotografia.support.Cenario
import com.ranny.fotografia.support.JsonCorpo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.time.YearMonth

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PacoteRepository pacoteRepository

    @Autowired
    AgendamentoRepository agendamentoRepository

    @Autowired
    GaleriaRepository galeriaRepository

    def "retorna dashboard vazio quando nao ha dados"() {
        when:
        def body = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/dashboard")
                .param("data", "2026-11-06"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        body.data == "2026-11-06"
        body.resumo.ensaiosHoje == 0
        body.resumo.ensaiosSemana == 0
        body.resumo.contratosSaldoPendente == 0
        body.proximosEnsaios.size() == 0
        body.pendencias.size() == 0
        body.financeiroMes.mes == "2026-11"
        body.financeiroMes.totalPrevisto == 0.0d
    }

    def "agrega ensaio, galeria e financeiro do dia"() {
        given:
        def cliente = Cenario.cliente(clienteRepository, "Cliente Dashboard")
        def pacote = Cenario.pacote(pacoteRepository, "Pacote Dashboard",
                new BigDecimal("1000.00"), 1, 0)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                Cenario.amanhaAsDez(), StatusAgendamento.REALIZADO)
        def galeria = Galeria.criar("estudio", agendamento.id, null, cliente.id,
                cliente.nome, 1, 0, BigDecimal.ZERO)
        galeria.publicarSelecao()
        galeriaRepository.save(galeria)

        when:
        def dataConsulta = agendamento.dataHoraInicio.toLocalDate().toString()
        def body = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/dashboard")
                .param("data", dataConsulta))
                .andExpect(status().isOk())
                .andReturn())

        then:
        body.resumo.ensaiosSemana == 1
        body.proximosEnsaios.size() == 1
        body.proximosEnsaios[0].clienteNome == "Cliente Dashboard"
        body.proximosEnsaios[0].status == "realizado"
        body.resumo.galeriasEmSelecao == 1
        body.pendencias.any { it.tipo == "GALERIA" && it.prioridade == "BAIXA" }
        body.financeiroMes.mes == YearMonth.from(agendamento.dataHoraInicio).toString()
        body.financeiroMes.totalPrevisto == 1000.0d
    }

    def "rejeita data invalida"() {
        when:
        def resultado = mockMvc.perform(get("/api/v1/dashboard")
                .param("data", "2026-13-40"))
                .andReturn()

        then:
        resultado.response.status == 400
        JsonCorpo.mapa(resultado).codigo == "DATA_INVALIDA"
    }
}
