package com.ranny.fotografia.galeria.api

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
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
@TestPropertySource(properties = ["app.galeria.zip-max-bytes=1000"])
@Transactional
class GaleriaZipLimitSpec extends Specification {

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

    def "bloqueia zip acima do limite configurado"() {
        given:
        def cliente = Cenario.cliente(clienteRepository, "Cliente Zip Limite")
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

        def imagemOriginal = imagemJpeg(800, 600, Color.BLUE)
        storage.simular(imagemOriginal.length, "image/jpeg", imagemOriginal)
        def midiaId = enviarFoto(galeria.id, "foto.jpg", imagemOriginal)
        def link = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeria.id}/selecao/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        mockMvc.perform(post("/api/v1/public/galerias/${link.token}/selecao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([fotoIds: [midiaId], videoIds: []])))
                .andExpect(status().isOk())

        def imagemEditada = imagemJpeg(800, 600, Color.RED)
        storage.simular(imagemEditada.length, "image/jpeg", imagemEditada)
        enviarVersaoEditada(galeria.id, midiaId, imagemEditada)
        mockMvc.perform(post("/api/v1/galerias/${galeria.id}/entrega/liberar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([midiaIds: [midiaId]])))
                .andExpect(status().isOk())

        when:
        def resultado = mockMvc.perform(
                get("/api/v1/public/galerias/${link.token}/download-zip/validar")
                        .param("versao", "auto"))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "ZIP_MUITO_GRANDE"
        imagemOriginal.length > 1000
        imagemEditada.length > 1000
    }

    private String enviarFoto(String galeriaId, String nome, byte[] imagem) {
        storage.simular(imagem.length, "image/jpeg", imagem)
        def sessao = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/galerias/${galeriaId}/midias/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        nomeArquivo   : nome,
                        mimeType      : "image/jpeg",
                        tamanho       : imagem.length,
                        checksumSha256: sha256(imagem),
                        tipo          : "foto"
                ])))
                .andExpect(status().isOk())
                .andReturn())
        return JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: [], tipo: "foto"])))
                .andExpect(status().isOk())
                .andReturn()).id
    }

    private void enviarVersaoEditada(String galeriaId, String midiaId, byte[] imagem) {
        storage.simular(imagem.length, "image/jpeg", imagem)
        def sessao = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/${midiaId}/editada/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([
                                nomeArquivo          : "editada.jpg",
                                mimeType             : "image/jpeg",
                                tamanho              : imagem.length,
                                checksumSha256       : sha256(imagem),
                                liberarAutomaticamente: true
                        ])))
                .andExpect(status().isOk())
                .andReturn())
        mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/${midiaId}/editada/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: []])))
                .andExpect(status().isOk())
    }

    private static byte[] imagemJpeg(int largura, int altura, Color cor) {
        def imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB)
        def graphics = imagem.createGraphics()
        graphics.setColor(cor)
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
