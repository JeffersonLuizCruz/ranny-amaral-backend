package com.ranny.fotografia.armazenamento.api

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.armazenamento.service.ArmazenamentoService
import com.ranny.fotografia.armazenamento.service.ColetorArmazenamento
import com.ranny.fotografia.arquivo.repository.ArquivoRepository
import com.ranny.fotografia.arquivo.storage.FakeObjectStorage
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.pacote.repository.PacoteRepository
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo
import com.ranny.fotografia.shared.model.PagamentoEntrada
import com.ranny.fotografia.shared.model.PagamentoSaldo
import com.ranny.fotografia.support.Cenario
import com.ranny.fotografia.support.JsonCorpo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = [
        "app.armazenamento.volume-path=\${java.io.tmpdir}",
        "app.armazenamento.reserva-bytes=1073741824",
        "app.armazenamento.percentual-alerta=99"
])
class ArmazenamentoSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PacoteRepository pacoteRepository

    @Autowired
    AgendamentoRepository agendamentoRepository

    @Autowired
    FakeObjectStorage storage

    @Autowired
    ArquivoRepository arquivoRepository

    @Autowired
    ArmazenamentoService armazenamentoService

    @Autowired
    ColetorArmazenamento coletor

    def setup() {
        storage.objetos().clear()
    }

    def "resumo mede o volume, o catalogo e separa a infraestrutura"() {
        given: "uma galeria com midia enviada"
        def galeriaId = criarGaleriaComMidia()

        when:
        def resumo = JsonCorpo.mapa(mockMvc.perform(get("/api/v1/armazenamento"))
                .andExpect(status().isOk())
                .andReturn())

        then: "o disco foi medido de verdade"
        resumo.origemColeta == "FILESYSTEM"
        resumo.totalBytes > 0
        resumo.livreBytes > 0
        resumo.percentualUsado >= 0
        resumo.percentualUsado <= 100
        resumo.alerta in ["OK", "ATENCAO", "CRITICO"]

        and: "o catalogo entrou no consumo do CRM"
        resumo.objetosBytes > 0
        resumo.crmBytes >= resumo.objetosBytes
        resumo.totais.originaisBytes > 0

        and: "a infraestrutura nao e negativa"
        resumo.outrosBytes >= 0

        when:
        def galerias = JsonCorpo.lista(mockMvc.perform(
                get("/api/v1/armazenamento/galerias"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        def item = galerias.find { it.galeriaId == galeriaId }
        item != null
        item.totalBytes > 0
        item.quantidadeArquivos > 0
        !item.elegivel
        item.motivoBloqueio != null
    }

    def "reconciliacao compara bucket e catalogo sem apagar nada"() {
        given:
        criarGaleriaComMidia()
        // O fake nao materializa uploads PUT; aqui todos os objetos do catalogo
        // passam a existir no bucket para exercitar a comparacao.
        arquivoRepository.findAll().each { arquivo ->
            storage.salvar(arquivo.objectKey, [1, 2, 3] as byte[], arquivo.mimeType)
        }

        when:
        def reconciliacao = armazenamentoService.reconciliar()

        then:
        reconciliacao.objetosStorage() > 0
        reconciliacao.objetosCatalogo() > 0
        reconciliacao.somenteStorage() == 0
        reconciliacao.somenteCatalogo() == 0
        reconciliacao.bytesSomenteStorage() == 0
        reconciliacao.amostraSomenteStorage().isEmpty()
        reconciliacao.amostraSomenteCatalogo().isEmpty()

        and: "nada foi removido"
        storage.objetos().size() == reconciliacao.objetosStorage()
    }

    def "coleta manual grava nova amostra"() {
        when:
        def primeiro = coletor.coletar()
        def segundo = coletor.coletar()

        then:
        primeiro.coletadoEm
        segundo.coletadoEm
        !segundo.coletadoEm.isBefore(primeiro.coletadoEm)
        segundo.origemColeta.name() == "FILESYSTEM"
    }

    private String criarGaleriaComMidia() {
        def cliente = Cenario.cliente(clienteRepository, "Cliente Armazenamento")
        def pacote = Cenario.pacote(pacoteRepository, "Galeria", new BigDecimal("1000.00"),
                1, 0, new BigDecimal("50.00"), null, List.of())
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                Cenario.amanhaAsDez(), StatusAgendamento.REALIZADO)
        agendamento.atualizarPagamentoEntrada(
                new BigDecimal("300.00"), new BigDecimal("30.00"),
                PagamentoEntrada.confirmada(new BigDecimal("300.00"), new BigDecimal("30.00"),
                        Instant.now(), "Teste"))
        agendamento.atualizarPagamentoFinal(
                PagamentoSaldo.recebido(new BigDecimal("700.00"), FormaPagamentoSaldo.PIX,
                        Instant.now(), "Teste"))
        agendamentoRepository.saveAndFlush(agendamento)

        def galeria = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/galerias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([agendamentoId: agendamento.id])))
                .andExpect(status().isOk())
                .andReturn())

        def imagem = imagemJpeg()
        storage.simular(imagem.length, "image/jpeg", imagem)
        def sessao = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/galerias/${galeria.id}/midias/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        nomeArquivo   : "foto.jpg",
                        mimeType      : "image/jpeg",
                        tamanho       : imagem.length,
                        checksumSha256: sha256(imagem),
                        tipo          : "foto"
                ])))
                .andExpect(status().isOk())
                .andReturn())
        mockMvc.perform(
                post("/api/v1/galerias/${galeria.id}/midias/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: [], tipo: "foto"])))
                .andExpect(status().isOk())
        return galeria.id as String
    }

    private static byte[] imagemJpeg() {
        def imagem = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB)
        def graphics = imagem.createGraphics()
        graphics.setColor(Color.GREEN)
        graphics.fillRect(0, 0, imagem.width, imagem.height)
        graphics.dispose()
        def saida = new ByteArrayOutputStream()
        ImageIO.write(imagem, "jpg", saida)
        return saida.toByteArray()
    }

    private static String sha256(byte[] conteudo) {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(conteudo))
    }
}
