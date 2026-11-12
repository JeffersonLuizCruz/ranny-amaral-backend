package com.ranny.fotografia.galeria.api

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.arquivo.ArquivoStatus
import com.ranny.fotografia.arquivo.repository.ArquivoRepository
import com.ranny.fotografia.arquivo.storage.FakeObjectStorage
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.galeria.model.GaleriaStatus
import com.ranny.fotografia.galeria.repository.GaleriaRepository
import com.ranny.fotografia.galeria.service.ExpurgoGaleriaService
import com.ranny.fotografia.pacote.repository.PacoteRepository
import com.ranny.fotografia.shared.exception.RegraDeNegocioException
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
class ExpurgoGaleriaSpec extends Specification {

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

    @Autowired
    ArquivoRepository arquivoRepository

    @Autowired
    ExpurgoGaleriaService expurgoService

    @Autowired
    FakeObjectStorage storage

    def setup() {
        storage.objetos().clear()
    }

    def "expurgo remove binarios, revoga o link e preserva metadados"() {
        given: "uma galeria entregue com ZIP concluido"
        def cenario = galeriaEntregue()
        def token = cenario.token

        mockMvc.perform(get("/api/v1/public/galerias/${token}/download-zip")
                .param("versao", "auto"))
                .andExpect(status().isOk())

        when: "o staff consulta o armazenamento antes do expurgo"
        def antes = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/galerias/${cenario.galeriaId}/armazenamento"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        antes.elegivel
        antes.zipConcluidoEm
        antes.totalBytes > 0
        antes.quantidadeArquivos > 0
        antes.grupos.size() > 0
        antes.confirmacaoEsperada == cenario.clienteNome

        when: "o ADMIN agenda e o job executa"
        def solicitacao = expurgoService.solicitar(cenario.galeriaId, "Cliente baixou o ZIP",
                cenario.clienteNome, "admin@teste.com")
        expurgoService.executar(cenario.galeriaId)

        then:
        solicitacao.expurgoStatus() == "agendado"
        solicitacao.bytesEstimados() > 0

        and: "a galeria fica terminal com os bytes liberados"
        def galeria = galeriaRepository.findById(cenario.galeriaId).orElseThrow()
        galeria.status == GaleriaStatus.EXPURGADA
        galeria.expurgoStatus.valor() == "executado"
        galeria.expurgoEfetivadoEm != null
        galeria.bytesExpurgados > 0

        and: "nenhum binario da midia permanece no storage"
        storage.objetos().isEmpty()

        and: "as linhas de arquivo ficam como expurgadas"
        cenario.arquivoIds.every { id ->
            arquivoRepository.findById(id).orElseThrow().status == ArquivoStatus.EXPURGADO
        }

        and: "o link publico responde 410"
        mockMvc.perform(get("/api/v1/public/galerias/${token}"))
                .andExpect(status().isGone())

        and: "o detalhe continua existindo, agora sem binarios"
        def depois = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/galerias/${cenario.galeriaId}/armazenamento"))
                .andExpect(status().isOk())
                .andReturn())
        depois.totalBytes == 0
        depois.quantidadeArquivos == 0
        depois.bytesExpurgados > 0
        !depois.elegivel
        depois.motivoBloqueio != null
    }

    def "nao expurga sem ZIP concluido"() {
        given: "galeria entregue mas sem download do cliente"
        def cenario = galeriaEntregue()

        when:
        def detalhe = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/galerias/${cenario.galeriaId}/armazenamento"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        !detalhe.elegivel
        detalhe.motivoBloqueio.contains("ZIP")

        when: "o ADMIN tenta agendar mesmo assim"
        expurgoService.solicitar(cenario.galeriaId, "tentativa", cenario.clienteNome, "admin")

        then:
        def erro = thrown(RegraDeNegocioException)
        erro.codigo == "EXPURGO_NAO_ELEGIVEL"
    }

    def "confirmacao exige o nome do cliente"() {
        given:
        def cenario = galeriaEntregue()
        mockMvc.perform(get("/api/v1/public/galerias/${cenario.token}/download-zip")
                .param("versao", "auto"))
                .andExpect(status().isOk())

        when:
        expurgoService.solicitar(cenario.galeriaId, "motivo", "EXPURGAR", "admin")

        then:
        def erro = thrown(RegraDeNegocioException)
        erro.codigo == "CONFIRMACAO_INVALIDA"
    }

    def "comprovante de extras nao e expurgado"() {
        given: "galeria com comprovante anexado ao prefixo de extras"
        def cenario = galeriaEntregue()
        String chaveExtras = "estudios/estudio/galerias/${cenario.galeriaId}/extras/pedido-1/comprovante.pdf".toString()
        storage.salvar(chaveExtras, "%PDF-1.7".getBytes("UTF-8"), "application/pdf")

        mockMvc.perform(get("/api/v1/public/galerias/${cenario.token}/download-zip")
                .param("versao", "auto"))
                .andExpect(status().isOk())

        when:
        expurgoService.solicitar(cenario.galeriaId, "Cliente baixou", cenario.clienteNome, "admin")
        expurgoService.executar(cenario.galeriaId)

        then:
        storage.objetos().containsKey(chaveExtras)
    }

    private Map galeriaEntregue() {
        def cliente = Cenario.cliente(clienteRepository, "Cliente Expurgo")
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

        def original = imagemJpeg(Color.BLUE)
        def arquivoOriginal = enviarFoto(galeria.id, "foto.jpg", original)
        def link = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeria.id}/selecao/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        mockMvc.perform(post("/api/v1/public/galerias/${link.token}/selecao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([fotoIds: [arquivoOriginal.midiaId], videoIds: []])))
                .andExpect(status().isOk())

        def editada = imagemJpeg(Color.RED)
        def arquivoEditada = enviarVersaoEditada(galeria.id, arquivoOriginal.midiaId, editada)
        mockMvc.perform(post("/api/v1/galerias/${galeria.id}/entrega/liberar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([midiaIds: [arquivoOriginal.midiaId]])))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${galeria.id}/entrega/publicar"))
                .andExpect(status().isOk())

        return [
                galeriaId  : galeria.id as String,
                clienteNome: "Cliente Expurgo",
                token      : link.token as String,
                arquivoIds : [arquivoOriginal.arquivoId, arquivoEditada.arquivoId]
        ]
    }

    private Map enviarFoto(String galeriaId, String nome, byte[] imagem) {
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
        def midia = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: [], tipo: "foto"])))
                .andExpect(status().isOk())
                .andReturn())
        return [midiaId: midia.id, arquivoId: sessao.arquivoId]
    }

    private Map enviarVersaoEditada(String galeriaId, String midiaId, byte[] imagem) {
        storage.simular(imagem.length, "image/jpeg", imagem)
        def sessao = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/${midiaId}/editada/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([
                                nomeArquivo           : "editada.jpg",
                                mimeType              : "image/jpeg",
                                tamanho               : imagem.length,
                                checksumSha256        : sha256(imagem),
                                liberarAutomaticamente: true
                        ])))
                .andExpect(status().isOk())
                .andReturn())
        mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/${midiaId}/editada/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: []])))
                .andExpect(status().isOk())
        return [arquivoId: sessao.arquivoId]
    }

    private static byte[] imagemJpeg(Color cor) {
        def imagem = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB)
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
