package com.ranny.fotografia.galeria.api

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.arquivo.storage.FakeObjectStorage
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.galeria.model.MidiaStatus
import com.ranny.fotografia.galeria.model.MidiaTipo
import com.ranny.fotografia.galeria.repository.MidiaGaleriaRepository
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
class GaleriaControllerSpec extends Specification {

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
    MidiaGaleriaRepository midiaRepository

    def "nao publica selecao de ensaio nao realizado"() {
        given:
        def cliente = Cenario.cliente(clienteRepository, "Cliente Nao Realizado")
        def pacote = Cenario.pacote(pacoteRepository, "Galeria", new BigDecimal("1000.00"), 1, 0)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                Cenario.amanhaAsDez(), StatusAgendamento.RESERVADO)

        def galeria = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/galerias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([agendamentoId: agendamento.id])))
                .andExpect(status().isOk())
                .andReturn())

        when:
        def resultado = mockMvc.perform(post("/api/v1/galerias/${galeria.id}/selecao/publicar"))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "ENSAIO_NAO_REALIZADO"
    }

    def "fluxo completo de galeria: upload, selecao e entrega"() {
        given:
        def cliente = Cenario.cliente(clienteRepository, "Marina Galeria")
        def pacote = Cenario.pacote(pacoteRepository, "Galeria", new BigDecimal("1000.00"), 1, 0)
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

        def imagem = imagemJpeg()
        storage.simular(imagem.length, "image/jpeg", imagem)

        when:
        def galeria = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/galerias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([agendamentoId: agendamento.id])))
                .andExpect(status().isOk())
                .andReturn())

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
                .andExpect(jsonPath('$.estrategia').value("PUT"))
                .andReturn())

        def midia = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeria.id}/midias/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: [], tipo: "foto"])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.status').value("disponivel"))
                .andReturn())

        assert midiaRepository.countByGaleriaIdAndTipoAndStatus(
                galeria.id, MidiaTipo.FOTO, MidiaStatus.DISPONIVEL) == 1

        def link = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeria.id}/selecao/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        def publica = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/public/galerias/${link.token}"))
                .andExpect(status().isOk())
                .andReturn())

        def selecao = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/public/galerias/${link.token}/selecao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([fotoIds: [midia.id], videoIds: []])))
                .andExpect(status().isOk())
                .andReturn())

        mockMvc.perform(post("/api/v1/galerias/${galeria.id}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${galeria.id}/pronta-entrega"))
                .andExpect(status().isOk())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeria.id}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        def download = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/public/galerias/${entrega.token}/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([midiaId: midia.id])))
                .andExpect(status().isOk())
                .andReturn())

        then:
        galeria.status == "aguardando_upload"
        publica.midias.size() == 1
        publica.midias[0].id == midia.id
        publica.midias[0].thumbUrl != null
        selecao.protocolo.startsWith("SEL-")
        selecao.fotosExtras == 0
        entrega.token != null
        download.url.startsWith("http://fake-storage/")
    }

    private static byte[] imagemJpeg() {
        def imagem = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB)
        def graphics = imagem.createGraphics()
        graphics.setColor(Color.BLUE)
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
