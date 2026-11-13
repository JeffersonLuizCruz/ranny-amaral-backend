package com.ranny.fotografia.contrato.api

import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.model.Cliente
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.contrato.model.ModeloContrato
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

/** Spec de integracao HTTP dos contratos administrativos. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContratoControllerSpec extends Specification {

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
    Cliente cliente
    Pacote pacote

    def setup() {
        modelo = Cenario.modelo(modeloRepository)
        cliente = Cenario.cliente(clienteRepository)
        pacote = Cenario.pacote(pacoteRepository)
        Cenario.estudio(estudioRepository)
    }

    def "cria contrato em rascunho com snapshot e clausulas resolvidas"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpoPadrao())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.statusAceite').value("rascunho"))
                .andExpect(jsonPath('$.statusEntrada').value("pendente"))
                .andExpect(jsonPath('$.statusSaldoFinal').value("pendente"))
                .andExpect(jsonPath('$.entradaQuitada').value(false))
                .andExpect(jsonPath('$.saldoQuitado').value(false))
                .andExpect(jsonPath('$.quitado').value(false))
                .andExpect(jsonPath('$.valorRecebido').value(0.0d))
                .andExpect(jsonPath('$.valorPendente').value(1000.0d))
                .andExpect(jsonPath('$.valorEntrada').value(300.0d))
                .andExpect(jsonPath('$.saldoRestante').value(700.0d))
                .andExpect(jsonPath('$.snapshot.cliente.nome').value("Marina Teste"))
                .andReturn()

        then:
        def json = JsonCorpo.mapa(resultado)
        json.numero == "CT-${LocalDate.now(FUSO).year}-0001"
        json.clausulas[0].corpo.contains("Marina Teste")
        json.clausulas[0].corpo.contains("R\$")
        json.clausulasModelo[0].corpo.contains("{{cliente.nome}}")
        json.historico[0].tipo == "criado"
    }

    def "numera contratos sequencialmente por ano"() {
        when:
        def primeiro = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpoPadrao()))).andReturn())
        def segundo = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpoPadrao()))).andReturn())

        then:
        primeiro.numero.endsWith("-0001")
        segundo.numero.endsWith("-0002")
        primeiro.id != segundo.id
    }

    @Unroll
    def "rejeita contrato invalido quando #descricao"() {
        when:
        def resultado = mockMvc.perform(post("/api/v1/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpoPadrao() + ajuste)))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).campos*.campo.contains(campo)

        where:
        descricao                | ajuste                  | campo
        "o modelo esta vazio"    | [modeloId: ""]          | "modeloId"
        "o modelo esta nulo"     | [modeloId: null]        | "modeloId"
    }

    def "impede editar contrato que saiu do rascunho"() {
        given:
        def contrato = criarContrato()
        gerarLink(contrato.id)

        when:
        def resultado = mockMvc.perform(put("/api/v1/contratos/${contrato.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpoPadrao() + [valorTotal: 1200.00])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "CONTRATO_NAO_EDITAVEL"
    }

    def "exclui rascunho e bloqueia exclusao depois de gerar link"() {
        given:
        def rascunho = criarContrato()
        def comLink = criarContrato()
        gerarLink(comLink.id)

        when:
        def exclusaoRascunho = mockMvc.perform(delete("/api/v1/contratos/${rascunho.id}")).andReturn()
        def exclusaoComLink = mockMvc.perform(delete("/api/v1/contratos/${comLink.id}")).andReturn()

        then:
        exclusaoRascunho.response.status == 204
        exclusaoComLink.response.status == 422
        contratoRepository.findById(rascunho.id).isEmpty()
        contratoRepository.findById(comLink.id).isPresent()
    }

    def "duplica contrato como novo rascunho sem aceite nem comprovante"() {
        given:
        def original = criarContrato()

        when:
        def duplicado = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos/${original.id}/duplicar"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.statusAceite').value("rascunho"))
                .andReturn())

        then:
        duplicado.id != original.id
        duplicado.numero != original.numero
        duplicado.aceite == null
        duplicado.solicitacaoAtualId == null
        duplicado.historico.find { it.tipo == "duplicado" }.detalhe.contains(original.numero as String)
    }

    def "lista os links de aceite do contrato"() {
        given:
        def contrato = criarContrato()
        def link = gerarLink(contrato.id)

        when:
        def resultado = mockMvc.perform(get("/api/v1/contratos/${contrato.id}/links-aceite"))
                .andExpect(status().isOk())
                .andReturn()

        then:
        def links = JsonCorpo.lista(resultado)
        links.size() == 1
        links[0].id == link.id
        links[0].token == link.token
    }

    def "gera link de aceite com validade padrao de sete dias"() {
        given:
        def contrato = criarContrato()

        when:
        def link = gerarLink(contrato.id)

        then:
        link.token
        link.status == "pendente"
        ChronoUnit.DAYS.between(Instant.parse(link.criadaEm as String), Instant.parse(link.expiraEm as String)) == 7
        def atualizado = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos/${contrato.id}")).andReturn())
        atualizado.statusAceite == "pendente"
        atualizado.solicitacaoAtualId == link.id
    }

    def "cancela link e devolve contrato para rascunho"() {
        given:
        def contrato = criarContrato()
        def link = gerarLink(contrato.id)

        when:
        mockMvc.perform(post("/api/v1/contratos/${contrato.id}/links-aceite/${link.id}/cancelar"))
                .andExpect(status().isNoContent())

        then:
        def atualizado = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos/${contrato.id}")).andReturn())
        atualizado.statusAceite == "rascunho"
        atualizado.solicitacaoAtualId == null
        solicitacaoRepository.findById(link.id as String).get().status == StatusSolicitacaoAceite.CANCELADA
    }

    def "confirma a entrada do contrato"() {
        given:
        def contrato = criarContrato()

        when:
        def resultado = mockMvc.perform(post("/api/v1/contratos/${contrato.id}/pagamento/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([confirmadoPor: "Ana Souza"])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.statusEntrada').value("confirmado"))
                .andExpect(jsonPath('$.entradaQuitada').value(true))
                .andExpect(jsonPath('$.valorRecebido').value(300.0d))
                .andExpect(jsonPath('$.valorPendente').value(700.0d))
                .andExpect(jsonPath('$.pagamento.confirmadoPor').value("Ana Souza"))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).pagamento.confirmadoEm
    }

    def "rejeita comprovante e registra o motivo"() {
        given:
        def contrato = criarContrato()

        when:
        def resultado = mockMvc.perform(post("/api/v1/contratos/${contrato.id}/pagamento/rejeitar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([motivo: "Comprovante ilegivel"])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.statusEntrada').value("pendente"))
                .andExpect(jsonPath('$.pagamento.rejeicao.motivo').value("Comprovante ilegivel"))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).historico.find { it.tipo == "pagamento_rejeitado" }
    }

    def "isenta a entrada"() {
        given:
        def contrato = criarContrato()

        expect:
        mockMvc.perform(post("/api/v1/contratos/${contrato.id}/pagamento/isentar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.statusEntrada').value("isento"))
    }

    def "registra o saldo final e permite reabrir quando nao confirmado"() {
        given:
        def contrato = criarContrato()
        confirmarEntrada(contrato.id)

        when:
        def recebido = mockMvc.perform(put("/api/v1/contratos/${contrato.id}/pagamento-final")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 700.00, formaPagamento: "dinheiro", recebidoPor: "Ana"])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.statusSaldoFinal').value("confirmado"))
                .andExpect(jsonPath('$.saldoQuitado').value(true))
                .andExpect(jsonPath('$.valorRecebido').value(1000.0d))
                .andExpect(jsonPath('$.valorPendente').value(0.0d))
                .andExpect(jsonPath('$.pagamentoFinal.formaPagamento').value("dinheiro"))
                .andReturn()

        def isento = mockMvc.perform(post("/api/v1/contratos/${contrato.id}/pagamento-final/isentar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.statusSaldoFinal').value("isento"))
                .andReturn()

        def reaberto = mockMvc.perform(post("/api/v1/contratos/${contrato.id}/pagamento-final/reabrir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.statusSaldoFinal').value("pendente"))
                .andReturn()

        then:
        receiptsValidos(recebido, isento, reaberto)
    }

    def "bloqueia saldo final enquanto a entrada esta pendente"() {
        given:
        def contrato = criarContrato()

        when:
        def resultado = mockMvc.perform(put("/api/v1/contratos/${contrato.id}/pagamento-final")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 700.00, formaPagamento: "dinheiro"])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "ENTRADA_PENDENTE"
    }

    def "marca o contrato como quitado quando entrada e saldo final estao confirmados"() {
        given:
        def contrato = criarContrato()
        mockMvc.perform(post("/api/v1/contratos/${contrato.id}/pagamento/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())

        when:
        def resultado = mockMvc.perform(put("/api/v1/contratos/${contrato.id}/pagamento-final")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 700.00, formaPagamento: "pix"])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.quitado').value(true))
                .andExpect(jsonPath('$.valorRecebido').value(1000.0d))
                .andExpect(jsonPath('$.valorPendente').value(0.0d))
                .andReturn()

        then:
        JsonCorpo.mapa(resultado).statusSaldoFinal == "confirmado"
    }

    def "rejeita saldo final parcial e mantem o pagamento pendente"() {
        given:
        def contrato = criarContrato()
        confirmarEntrada(contrato.id)

        when:
        mockMvc.perform(put("/api/v1/contratos/${contrato.id}/pagamento-final")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 100.00, formaPagamento: "dinheiro", recebidoPor: "Ana"])))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.codigo').value("SALDO_FINAL_INTEGRAL_OBRIGATORIO"))

        then:
        def atual = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos/${contrato.id}")).andReturn())
        atual.statusSaldoFinal == "pendente"
        atual.pagamentoFinal.status == "pendente"
    }

    def "cancela contrato pendente e propaga o cancelamento aos pagamentos"() {
        given:
        def contrato = criarContrato()
        def link = gerarLink(contrato.id)

        when:
        def cancelado = mockMvc.perform(post("/api/v1/contratos/${contrato.id}/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.statusAceite').value("cancelado"))
                .andExpect(jsonPath('$.statusEntrada').value("cancelado"))
                .andExpect(jsonPath('$.statusSaldoFinal').value("cancelado"))
                .andReturn()

        then:
        JsonCorpo.mapa(cancelado).historico.find { it.tipo == "contrato_cancelado" }
        solicitacaoRepository.findById(link.id as String).get().status == StatusSolicitacaoAceite.CANCELADA
    }

    def "filtra contratos por status de aceite"() {
        given:
        def rascunho = criarContrato()
        def pendente = criarContrato()
        gerarLink(pendente.id)

        when:
        def resultadoPendentes = mockMvc.perform(get("/api/v1/contratos")
                .param("statusAceite", "pendente"))
                .andReturn()
        def resultadoRascunhos = mockMvc.perform(get("/api/v1/contratos")
                .param("statusAceite", "rascunho"))
                .andReturn()
        def pendentes = JsonCorpo.mapa(resultadoPendentes)
        def rascunhos = JsonCorpo.mapa(resultadoRascunhos)

        then:
        pendentes.itens.size() == 1
        pendentes.itens[0].id == pendente.id
        pendentes.totalItens == 1
        rascunhos.itens.size() == 1
        rascunhos.itens[0].id == rascunho.id
    }

    def "filtra contratos por status da entrada"() {
        given:
        def confirmado = criarContrato()
        criarContrato()
        mockMvc.perform(post("/api/v1/contratos/${confirmado.id}/pagamento/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())

        when:
        def confirmados = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos")
                .param("statusEntrada", "confirmado")).andReturn())
        def pendentes = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos")
                .param("statusEntrada", "pendente")).andReturn())

        then:
        confirmados.itens.size() == 1
        confirmados.itens[0].id == confirmado.id
        pendentes.itens.size() == 1
    }

    def "filtra contratos por status do saldo final"() {
        given:
        def recebido = criarContrato()
        criarContrato()
        confirmarEntrada(recebido.id)
        mockMvc.perform(put("/api/v1/contratos/${recebido.id}/pagamento-final")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([valor: 700.00, formaPagamento: "pix"])))
                .andExpect(status().isOk())

        when:
        def confirmados = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos")
                .param("statusSaldoFinal", "confirmado")).andReturn())
        def pendentes = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos")
                .param("statusSaldoFinal", "pendente")).andReturn())

        then:
        confirmados.itens.size() == 1
        confirmados.itens[0].id == recebido.id
        pendentes.itens.size() == 1
        pendentes.itens[0].id != recebido.id
    }

    def "busca contratos por #campo"() {
        given:
        def contrato = criarContrato()

        when:
        def resultado = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos")
                .param("q", termo))
                .andReturn())

        then:
        resultado.itens.size() == 1
        resultado.itens[0].id == contrato.id
        resultado.totalItens == 1

        where:
        campo           | termo
        "numero"        | "CT-"
        "nome cliente"  | "marina"
        "nome pacote"   | "pacote teste"
    }

    def "mantem nomes de cliente e pacote desnormalizados no contrato"() {
        given:
        def contrato = criarContrato()

        when:
        def salvo = contratoRepository.findById(contrato.id as String).get()

        then:
        salvo.clienteNome == "Marina Teste"
        salvo.pacoteNome == "Pacote Teste"
    }

    def "pagina contratos e informa os metadados"() {
        given:
        3.times { criarContrato() }

        when:
        def primeira = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos")
                .param("pagina", "0").param("tamanho", "2")).andReturn())
        def segunda = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/contratos")
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
    def "rejeita paginacao de contratos quando #descricao"() {
        when:
        def resultado = mockMvc.perform(get("/api/v1/contratos")
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

    def "retorna 404 para contrato inexistente"() {
        expect:
        mockMvc.perform(get("/api/v1/contratos/ct-inexistente"))
                .andExpect(status().isNotFound())
    }

    private Map corpoPadrao() {
        return [
                modeloId         : modelo.id,
                clienteId        : cliente.id,
                pacoteId         : pacote.id,
                valorTotal       : 1000.00,
                percentualEntrada: 30.00
        ]
    }

    private Map criarContrato() {
        return JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json(corpoPadrao())))
                .andExpect(status().isCreated())
                .andReturn())
    }

    private void confirmarEntrada(String contratoId) {
        mockMvc.perform(post("/api/v1/contratos/${contratoId}/pagamento/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
    }

    private Map gerarLink(String contratoId) {
        return JsonCorpo.mapa(mockMvc.perform(post("/api/v1/contratos/${contratoId}/links-aceite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andReturn())
    }

    private static boolean receiptsValidos(def recebido, def isento, def reaberto) {
        return JsonCorpo.mapa(recebido).pagamentoFinal.recebidoEm != null
                && JsonCorpo.mapa(isento).pagamentoFinal.formaPagamento == null
                && JsonCorpo.mapa(reaberto).pagamentoFinal.status == "pendente"
    }
}
