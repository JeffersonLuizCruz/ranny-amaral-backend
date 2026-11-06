package com.ranny.fotografia.contrato.api

import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.model.ClienteOrigem
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.contrato.model.ModeloContrato
import com.ranny.fotografia.contrato.model.SolicitacaoAceite
import com.ranny.fotografia.contrato.model.documento.StatusAceite
import com.ranny.fotografia.contrato.model.documento.StatusSolicitacaoAceite
import com.ranny.fotografia.contrato.repository.ContratoRepository
import com.ranny.fotografia.contrato.repository.EstudioConfigRepository
import com.ranny.fotografia.contrato.repository.ModeloContratoRepository
import com.ranny.fotografia.contrato.repository.SolicitacaoAceiteRepository
import com.ranny.fotografia.pacote.model.Pacote
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

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/** Spec de integracao das rotas publicas de aceite. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AceitePublicoSpec extends Specification {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo")

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

    ModeloContrato modelo
    Pacote pacote

    def setup() {
        modelo = Cenario.modelo(modeloRepository)
        pacote = Cenario.pacote(pacoteRepository)
        Cenario.estudio(estudioRepository)
    }

    def "consulta link valido e retorna contrato e solicitacao"() {
        given:
        def contrato = criarContratoSemCliente()
        def link = gerarLink(contrato.id)

        when:
        def resultado = mockMvc.perform(get("/api/v1/public/contratos/${link.token}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.solicitacao.status').value("pendente"))
                .andExpect(jsonPath('$.contrato.id').value(contrato.id))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).contrato.snapshot.cliente.nome == "Juliana Prado"
    }

    def "retorna 404 para token inexistente"() {
        expect:
        mockMvc.perform(get("/api/v1/public/contratos/token-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.codigo').value("RECURSO_NAO_ENCONTRADO"))
    }

    def "marca link como visualizado"() {
        given:
        def contrato = criarContratoSemCliente()
        def link = gerarLink(contrato.id)

        when:
        mockMvc.perform(post("/api/v1/public/contratos/${link.token}/visualizar"))
                .andExpect(status().isNoContent())

        then:
        def consulta = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/public/contratos/${link.token}")).andReturn())
        consulta.solicitacao.status == "visualizada"
        consulta.solicitacao.visualizadaEm != null
        def contratoAtualizado = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos/${contrato.id}")).andReturn())
        contratoAtualizado.historico.find { it.tipo == "link_visualizado" }
    }

    def "aceite cria cliente, gera protocolo e vincula ao contrato"() {
        given:
        def contrato = criarContratoSemCliente()
        def link = gerarLink(contrato.id)
        def antes = clienteRepository.count()

        when:
        def resultado = mockMvc.perform(post("/api/v1/public/contratos/${link.token}/aceite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        nomeCompleto  : "Juliana Prado",
                        tipoDocumento : "cpf",
                        documento     : "123.456.789-00",
                        cidade        : "São Paulo",
                        whatsapp      : "(11) 91234-5678"
                ])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.contrato.statusAceite').value("aceito"))
                .andExpect(jsonPath('$.solicitacao.status').value("aceita"))
                .andReturn()

        then:
        def json = JsonCorpo.mapa(resultado)
        json.protocolo == "ACE-${LocalDate.now(FUSO).year}-0001"
        json.contrato.aceite.protocolo == json.protocolo
        json.contrato.aceite.hashConteudo != null
        json.contrato.clienteId != null
        clienteRepository.count() == antes + 1
        clienteRepository.findById(json.contrato.clienteId as String).get().origem == ClienteOrigem.CONTRATO
    }

    def "aceite com comprovante deixa a entrada pendente de confirmacao"() {
        given:
        def contrato = criarContratoSemCliente()
        def link = gerarLink(contrato.id)
        def corpo = [
                nomeCompleto : "Juliana Prado",
                tipoDocumento: "rg",
                documento    : "12.345.678-9",
                cidade       : "São Paulo",
                comprovante  : [arquivoKey: "comprovantes/1.pdf", nomeArquivo: "1.pdf",
                                mimeType: "application/pdf", tamanho: 2048]
        ]

        when:
        def resultado = mockMvc.perform(post("/api/v1/public/contratos/${link.token}/aceite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.contrato.statusPagamento').value("pendente"))
                .andExpect(jsonPath('$.contrato.pagamento.comprovante.arquivoKey').value("comprovantes/1.pdf"))
                .andReturn()

        then:
        def json = JsonCorpo.mapa(resultado)
        json.contrato.historico.find { it.tipo == "comprovante_enviado" }
    }

    def "impede reutilizar um link ja aceito"() {
        given:
        def contrato = criarContratoSemCliente()
        def link = gerarLink(contrato.id)
        aceitar(link.token)

        when:
        def resultado = mockMvc.perform(post("/api/v1/public/contratos/${link.token}/aceite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(dadosAceite())))
                .andReturn()

        then:
        resultado.response.status == 409
        JsonCorpo.mapa(resultado).codigo == "LINK_JA_ACEITO"
    }

    def "registra comprovante enviado apos o aceite"() {
        given:
        def contrato = criarContratoSemCliente()
        def link = gerarLink(contrato.id)
        aceitar(link.token)

        when:
        def resultado = mockMvc.perform(post("/api/v1/public/contratos/${link.token}/comprovante")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([arquivoKey: "comprovantes/2.pdf", nomeArquivo: "2.pdf",
                                         mimeType: "application/pdf", tamanho: 1024])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.contrato.pagamento.comprovante.arquivoKey').value("comprovantes/2.pdf"))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).contrato.historico.find { it.tipo == "comprovante_enviado" }
    }

    def "link expirado responde 410 e marca contrato como expirado"() {
        given:
        def contrato = criarContratoSemCliente()
        gerarLink(contrato.id)
        def tokenExpirado = "token-expirado"
        solicitacaoRepository.save(SolicitacaoAceite.restaurar(
                "req-expirada", contrato.id, tokenExpirado,
                StatusSolicitacaoAceite.PENDENTE,
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.DAYS),
                null, null, null))

        when:
        def resultado = mockMvc.perform(get("/api/v1/public/contratos/${tokenExpirado}")).andReturn()

        then:
        resultado.response.status == 410
        JsonCorpo.mapa(resultado).codigo == "LINK_EXPIRADO"
        contratoRepository.findById(contrato.id as String).get().statusAceite == StatusAceite.EXPIRADO
    }

    def "link cancelado responde 410"() {
        given:
        def contrato = criarContratoSemCliente()
        def link = gerarLink(contrato.id)
        mockMvc.perform(post("/api/v1/contratos/${contrato.id}/links-aceite/${link.id}/cancelar"))
                .andExpect(status().isNoContent())

        expect:
        mockMvc.perform(get("/api/v1/public/contratos/${link.token}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath('$.codigo').value("LINK_CANCELADO"))
    }

    @Unroll
    def "rejeita aceite invalido quando #descricao"() {
        given:
        def contrato = criarContratoSemCliente()
        def link = gerarLink(contrato.id)

        when:
        def resultado = mockMvc.perform(post("/api/v1/public/contratos/${link.token}/aceite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(dadosAceite() + ajuste)))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).campos*.campo.contains(campo)

        where:
        descricao              | ajuste                     | campo
        "o nome esta vazio"    | [nomeCompleto: ""]         | "nomeCompleto"
        "o documento esta vazio"| [documento: ""]           | "documento"
        "o tipo e nulo"        | [tipoDocumento: null]      | "tipoDocumento"
        "a cidade esta vazia"  | [cidade: ""]               | "cidade"
    }

    private Map criarContratoSemCliente() {
        def agendamento = Cenario.agendamento(agendamentoRepository, null, pacote)
        return JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        modeloId         : modelo.id,
                        agendamentoId    : agendamento.id,
                        pacoteId         : pacote.id,
                        valorTotal       : 1000.00,
                        percentualEntrada: 30.00
                ])))
                .andExpect(status().isCreated())
                .andReturn())
    }

    private Map gerarLink(String contratoId) {
        return JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos/${contratoId}/links-aceite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andReturn())
    }

    private Map aceitar(String token) {
        return JsonCorpo.mapa(mockMvc.perform(post("/api/v1/public/contratos/${token}/aceite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(dadosAceite())))
                .andExpect(status().isOk())
                .andReturn())
    }

    private static Map dadosAceite() {
        return [
                nomeCompleto : "Juliana Prado",
                tipoDocumento: "cpf",
                documento    : "123.456.789-00",
                cidade       : "São Paulo",
                whatsapp     : "(11) 91234-5678"
        ]
    }
}
