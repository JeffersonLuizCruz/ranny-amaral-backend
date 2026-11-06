package com.ranny.fotografia.contrato

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.contrato.repository.ContratoRepository
import com.ranny.fotografia.contrato.repository.EstudioConfigRepository
import com.ranny.fotografia.contrato.repository.ModeloContratoRepository
import com.ranny.fotografia.contrato.repository.SolicitacaoAceiteRepository
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
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Verifica a integracao contrato -> agenda via eventos.
 *
 * <p>Este spec NAO usa {@code @Transactional}: o listener
 * {@code @ApplicationModuleListener} so roda depois do commit da transacao do
 * contrato. Por isso os dados sao persistidos de verdade e limpos no
 * {@code cleanup:}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoAgendaSyncSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ContratoRepository contratoRepository

    @Autowired
    SolicitacaoAceiteRepository solicitacaoRepository

    @Autowired
    ModeloContratoRepository modeloRepository

    @Autowired
    EstudioConfigRepository estudioRepository

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PacoteRepository pacoteRepository

    @Autowired
    AgendamentoRepository agendamentoRepository

    def cleanup() {
        solicitacaoRepository.deleteAll()
        contratoRepository.deleteAll()
        agendamentoRepository.deleteAll()
        clienteRepository.deleteAll()
        pacoteRepository.deleteAll()
        modeloRepository.deleteAll()
        estudioRepository.deleteAll()
    }

    /** Aguarda a entrega assíncrona do evento (registro durável). */
    private static void aguardar(Closure<Boolean> condicao) {
        long limite = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < limite) {
            if (condicao.call()) return
            Thread.sleep(50)
        }
        throw new AssertionError('Condição não satisfeita dentro do prazo de 5s')
    }

    def "excluir contrato rascunho desvincula o agendamento"() {
        given:
        def modelo = Cenario.modelo(modeloRepository)
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote)

        when:
        def contrato = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        modeloId         : modelo.id,
                        clienteId        : cliente.id,
                        agendamentoId    : agendamento.id,
                        pacoteId         : pacote.id,
                        valorTotal       : 1000.00,
                        percentualEntrada: 30.00
                ])))
                .andExpect(status().isCreated())
                .andReturn())

        aguardar { agendamentoRepository.findById(agendamento.id).get().contratoId == contrato.id }

        then:
        agendamentoRepository.findById(agendamento.id).get().contratoId == contrato.id

        when:
        mockMvc.perform(delete("/api/v1/contratos/${contrato.id}"))
                .andExpect(status().isNoContent())

        aguardar { agendamentoRepository.findById(agendamento.id).get().contratoId == null }

        then:
        agendamentoRepository.findById(agendamento.id).get().contratoId == null
    }

    def "sincroniza contrato, aceite e pagamento com o agendamento"() {
        given:
        def modelo = Cenario.modelo(modeloRepository)
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote)

        when:
        def contrato = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        modeloId         : modelo.id,
                        clienteId        : cliente.id,
                        agendamentoId    : agendamento.id,
                        pacoteId         : pacote.id,
                        valorTotal       : 1000.00,
                        percentualEntrada: 30.00
                ])))
                .andExpect(status().isCreated())
                .andReturn())

        aguardar { agendamentoRepository.findById(agendamento.id).get().contratoId == contrato.id }
        def aposCriacao = agendamentoRepository.findById(agendamento.id).get()

        then:
        aposCriacao.contratoId == contrato.id
        aposCriacao.status == StatusAgendamento.RESERVADO

        when:
        def link = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos/${contrato.id}/links-aceite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andReturn())

        mockMvc.perform(post("/api/v1/public/contratos/${link.token}/aceite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        nomeCompleto : cliente.nome,
                        tipoDocumento: "cpf",
                        documento    : "123.456.789-00",
                        cidade       : "São Paulo",
                        whatsapp     : cliente.whatsapp
                ])))
                .andExpect(status().isOk())

        def aposAceite = agendamentoRepository.findById(agendamento.id).get()

        then:
        aposAceite.status == StatusAgendamento.RESERVADO

        when:
        mockMvc.perform(post("/api/v1/contratos/${contrato.id}/pagamento/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([confirmadoPor: "Equipe do estudio"])))
                .andExpect(status().isOk())

        aguardar {
            agendamentoRepository.findById(agendamento.id).get().status == StatusAgendamento.CONFIRMADO
        }
        def aposPagamento = agendamentoRepository.findById(agendamento.id).get()

        then:
        aposPagamento.status == StatusAgendamento.CONFIRMADO
        aposPagamento.pagamentoEntrada.status == PagamentoStatus.CONFIRMADO
    }
}
