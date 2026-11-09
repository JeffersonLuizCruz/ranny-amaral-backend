package com.ranny.fotografia.galeria.api

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.arquivo.storage.FakeObjectStorage
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.galeria.model.MidiaStatus
import com.ranny.fotografia.galeria.model.MidiaTipo
import com.ranny.fotografia.galeria.repository.DownloadGaleriaRepository
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

    @Autowired
    DownloadGaleriaRepository downloadRepository

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

    def "nao publica selecao com saldo final parcial"() {
        given:
        def cliente = Cenario.cliente(clienteRepository, "Cliente Saldo Parcial")
        def pacote = Cenario.pacote(pacoteRepository, "Galeria", new BigDecimal("1000.00"), 1, 0)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                Cenario.amanhaAsDez(), StatusAgendamento.REALIZADO)
        agendamento.atualizarPagamentoEntrada(
                new BigDecimal("300.00"), new BigDecimal("30.00"),
                PagamentoEntrada.confirmada(new BigDecimal("300.00"), new BigDecimal("30.00"),
                        Instant.now(), "Teste"))
        agendamento.atualizarPagamentoFinal(
                PagamentoSaldo.recebido(new BigDecimal("100.00"), FormaPagamentoSaldo.PIX,
                        Instant.now(), "Teste"))
        agendamentoRepository.saveAndFlush(agendamento)

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
        JsonCorpo.mapa(resultado).codigo == "SALDO_PENDENTE"
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
        mockMvc.perform(post("/api/v1/galerias/${galeria.id}/entrega/liberar-raw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([por: "Teste"])))
                .andExpect(status().isOk())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeria.id}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        def download = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/public/galerias/${entrega.token}/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([midiaId: midia.id, versao: "original"])))
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

    def "envia versao editada de foto selecionada e entrega a versao final"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Versao Editada")
        def imagemEditada = imagemJpeg(160, 100, Color.RED)
        storage.simular(imagemEditada.length, "image/jpeg", imagemEditada)

        when:
        def sessao = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/midias/${preparado.midiaId}/editada/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([
                                nomeArquivo   : "foto-editada.jpg",
                                mimeType      : "image/jpeg",
                                tamanho       : imagemEditada.length,
                                checksumSha256: sha256(imagemEditada)
                        ])))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.estrategia').value("PUT"))
                .andReturn())

        def editada = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/midias/${preparado.midiaId}/editada/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: []])))
                .andExpect(status().isOk())
                .andReturn())

        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        def download = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/public/galerias/${entrega.token}/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([midiaId: preparado.midiaId])))
                .andExpect(status().isOk())
                .andReturn())

        then:
        editada.editadaDisponivel
        editada.status == "disponivel"
        editada.editadaUrl != null
        editada.thumbUrl != null
        download.url.contains("/editadas/")
        download.nomeArquivo == "foto.jpg"
    }

    def "rejeita versao editada de midia nao selecionada"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Sem Selecao", true, false)
        def imagemEditada = imagemJpeg(140, 90, Color.GREEN)
        storage.simular(imagemEditada.length, "image/jpeg", imagemEditada)

        when:
        def resultado = mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/midias/${preparado.extraMidiaId}/editada/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([
                                nomeArquivo   : "nao-selecionada.jpg",
                                mimeType      : "image/jpeg",
                                tamanho       : imagemEditada.length,
                                checksumSha256: sha256(imagemEditada)
                        ])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "MIDIA_NAO_SELECIONADA"
    }

    def "rejeita versao editada de video"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Video", false, true)

        when:
        def resultado = mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/midias/${preparado.videoId}/editada/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([
                                nomeArquivo   : "video.mp4",
                                mimeType      : "image/jpeg",
                                tamanho       : 1024,
                                checksumSha256: "a" * 64
                        ])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "EDICAO_APENAS_FOTOS"
    }

    def "remove versao editada e volta a usar o original"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Remove Edicao")
        def imagemEditada = imagemJpeg(150, 95, Color.ORANGE)
        storage.simular(imagemEditada.length, "image/jpeg", imagemEditada)
        def sessao = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/midias/${preparado.midiaId}/editada/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([
                                nomeArquivo   : "editada.jpg",
                                mimeType      : "image/jpeg",
                                tamanho       : imagemEditada.length,
                                checksumSha256: sha256(imagemEditada)
                        ])))
                .andExpect(status().isOk())
                .andReturn())
        mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/midias/${preparado.midiaId}/editada/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: []])))
                .andExpect(status().isOk())

        when:
        mockMvc.perform(delete(
                "/api/v1/galerias/${preparado.galeriaId}/midias/${preparado.midiaId}/editada"))
                .andExpect(status().isOk())

        def midia = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/galerias/${preparado.galeriaId}/midias"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        !midia.itens[0].editadaDisponivel
        midia.itens[0].editadaUrl == null
    }

    def "nao publica entrega sem versao editada e sem raw liberado"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Sem Entrega")
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())

        when:
        def resultado = mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "FOTOS_SEM_ENTREGA"
    }

    def "libera raw e entrega o arquivo original"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Raw")
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())

        when:
        def liberada = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/liberar-raw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([por: "Ranny"])))
                .andExpect(status().isOk())
                .andReturn())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())
        def download = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/public/galerias/${entrega.token}/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([midiaId: preparado.midiaId, versao: "original"])))
                .andExpect(status().isOk())
                .andReturn())

        then:
        liberada.rawLiberado
        liberada.rawLiberadoPor == "Ranny"
        download.url.contains("/midias/")
        !download.url.contains("/editadas/")
    }

    def "bloqueia download original quando raw nao foi liberado"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Raw Bloqueado")
        enviarVersaoEditada(preparado)
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        when:
        def resultado = mockMvc.perform(
                post("/api/v1/public/galerias/${entrega.token}/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([midiaId: preparado.midiaId, versao: "original"])))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "RAW_NAO_LIBERADO"
    }

    def "nao revoga raw com foto selecionada sem edicao"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Raw Em Uso")
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/entrega/liberar-raw"))
                .andExpect(status().isOk())

        when:
        def resultado = mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/revogar-raw"))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "RAW_EM_USO"
    }

    def "revoga raw quando todas as fotos tem versao editada"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Revoga Raw")
        enviarVersaoEditada(preparado, Color.CYAN)
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/entrega/liberar-raw"))
                .andExpect(status().isOk())

        when:
        def resultado = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/revogar-raw"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        !resultado.rawLiberado
        resultado.rawLiberadoEm == null
        resultado.rawLiberadoPor == null
    }

    def "nao expoe original no payload publico sem liberar raw"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Payload Sem Raw")
        enviarVersaoEditada(preparado)
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        when:
        def publica = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/public/galerias/${entrega.token}"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        publica.midias[0].originalUrl == null
        publica.midias[0].editadaUrl != null
    }

    def "expoe original no payload publico apos liberar raw"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Payload Com Raw")
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/entrega/liberar-raw"))
                .andExpect(status().isOk())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        when:
        def publica = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/public/galerias/${entrega.token}"))
                .andExpect(status().isOk())
                .andReturn())

        then:
        publica.midias[0].originalUrl != null
    }

    def "nao republica selecao depois do envio"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Republicar")

        when:
        def resultado = mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/selecao/publicar"))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "SELECAO_NAO_PUBLICAVEL"
    }

    def "nao reabre selecao apos a entrega"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente Reabrir Entrega")
        enviarVersaoEditada(preparado)
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())

        when:
        def resultado = mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/selecao/reabrir"))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "SELECAO_NAO_REABRIVEL"
    }

    def "zip registra downloads por arquivo"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente ZIP Registro")
        enviarVersaoEditada(preparado, Color.MAGENTA)
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/entrega/liberar-raw"))
                .andExpect(status().isOk())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        when:
        def validacao = JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/public/galerias/${entrega.token}/download-zip/validar")
                        .param("versao", "editada"))
                .andExpect(status().isOk())
                .andReturn())
        mockMvc.perform(get("/api/v1/public/galerias/${entrega.token}/download-zip")
                .param("versao", "editada"))
                .andExpect(status().isOk())

        then:
        validacao.quantidade == 1
        downloadRepository.count() == 1
    }

    private Map prepararGaleriaComSelecao(String nomeCliente, boolean comExtra = false,
                                          boolean comVideo = false) {
        def cliente = Cenario.cliente(clienteRepository, nomeCliente)
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

        def galeria = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/galerias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([agendamentoId: agendamento.id])))
                .andExpect(status().isOk())
                .andReturn())

        def midiaId = enviarFoto(galeria.id, "foto.jpg", imagemJpeg())
        String extraMidiaId = null
        if (comExtra) {
            extraMidiaId = enviarFoto(galeria.id, "extra.jpg",
                    imagemJpeg(100, 100, Color.MAGENTA))
        }
        String videoId = null
        if (comVideo) {
            videoId = enviarVideo(galeria.id, "video.mp4", "0000ftypisom-video".bytes)
        }

        def link = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeria.id}/selecao/publicar"))
                .andExpect(status().isOk())
                .andReturn())
        mockMvc.perform(post("/api/v1/public/galerias/${link.token}/selecao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([fotoIds: [midiaId], videoIds: []])))
                .andExpect(status().isOk())

        return [galeriaId: galeria.id, midiaId: midiaId,
                extraMidiaId: extraMidiaId, videoId: videoId]
    }

    def "baixa zip com versoes editadas e originais"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente ZIP")
        enviarVersaoEditada(preparado, Color.PINK)
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/entrega/liberar-raw"))
                .andExpect(status().isOk())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        when:
        def zipEditado = mockMvc.perform(
                get("/api/v1/public/galerias/${entrega.token}/download-zip")
                        .param("versao", "editada"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("galeria.zip")))
                .andReturn()

        def zipOriginal = mockMvc.perform(
                get("/api/v1/public/galerias/${entrega.token}/download-zip")
                        .param("versao", "original"))
                .andExpect(status().isOk())
                .andReturn()

        then:
        def bytesEditado = zipEditado.response.contentAsByteArray
        bytesEditado.length > 0
        bytesEditado[0] == (byte) 0x50
        bytesEditado[1] == (byte) 0x4B
        zipOriginal.response.contentAsByteArray.length > 0
    }

    def "nao baixa zip original sem liberar raw"() {
        given:
        def preparado = prepararGaleriaComSelecao("Cliente ZIP Bloqueado")
        enviarVersaoEditada(preparado, Color.YELLOW)
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/edicao/iniciar"))
                .andExpect(status().isOk())
        mockMvc.perform(post("/api/v1/galerias/${preparado.galeriaId}/pronta-entrega"))
                .andExpect(status().isOk())
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        when:
        def resultado = mockMvc.perform(
                get("/api/v1/public/galerias/${entrega.token}/download-zip")
                        .param("versao", "original"))
                .andReturn()

        then:
        resultado.response.status == 422
        JsonCorpo.mapa(resultado).codigo == "RAW_NAO_LIBERADO"
    }

    private void enviarVersaoEditada(Map preparado, Color cor = Color.RED) {
        def imagem = imagemJpeg(150, 95, cor)
        storage.simular(imagem.length, "image/jpeg", imagem)
        def sessao = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/midias/${preparado.midiaId}/editada/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([
                                nomeArquivo   : "editada.jpg",
                                mimeType      : "image/jpeg",
                                tamanho       : imagem.length,
                                checksumSha256: sha256(imagem)
                        ])))
                .andExpect(status().isOk())
                .andReturn())
        mockMvc.perform(
                post("/api/v1/galerias/${preparado.galeriaId}/midias/${preparado.midiaId}/editada/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: []])))
                .andExpect(status().isOk())
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

    private String enviarVideo(String galeriaId, String nome, byte[] video) {
        storage.simular(video.length, "video/mp4", video)
        def sessao = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/galerias/${galeriaId}/midias/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        nomeArquivo   : nome,
                        mimeType      : "video/mp4",
                        tamanho       : video.length,
                        checksumSha256: sha256(video),
                        tipo          : "video"
                ])))
                .andExpect(status().isOk())
                .andReturn())
        return JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: [], tipo: "video"])))
                .andExpect(status().isOk())
                .andReturn()).id
    }

    private static byte[] imagemJpeg() {
        return imagemJpeg(120, 80, Color.BLUE)
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
