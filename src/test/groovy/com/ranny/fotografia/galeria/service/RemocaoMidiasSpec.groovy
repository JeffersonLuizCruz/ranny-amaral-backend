package com.ranny.fotografia.galeria.service

import com.ranny.fotografia.arquivo.ArquivoConsulta
import com.ranny.fotografia.arquivo.ArquivoFinalidade
import com.ranny.fotografia.arquivo.ArquivoUpload
import com.ranny.fotografia.galeria.api.GaleriaDtos
import com.ranny.fotografia.galeria.model.Galeria
import com.ranny.fotografia.galeria.model.MidiaGaleria
import com.ranny.fotografia.galeria.model.MidiaStatus
import com.ranny.fotografia.galeria.model.MidiaTipo
import com.ranny.fotografia.galeria.model.SelecaoGaleria
import com.ranny.fotografia.galeria.model.SelecaoItem
import com.ranny.fotografia.galeria.repository.GaleriaRepository
import com.ranny.fotografia.galeria.repository.MidiaGaleriaRepository
import com.ranny.fotografia.galeria.repository.SelecaoGaleriaRepository
import com.ranny.fotografia.galeria.repository.SelecaoItemRepository
import com.ranny.fotografia.shared.exception.RegraDeNegocioException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RemocaoMidiasSpec extends Specification {

    @Autowired
    GaleriaRepository galeriaRepository

    @Autowired
    MidiaGaleriaRepository midiaRepository

    @Autowired
    SelecaoGaleriaRepository selecaoRepository

    @Autowired
    SelecaoItemRepository selecaoItemRepository

    @Autowired
    ArquivoConsulta arquivoConsulta

    @Autowired
    ArquivoUpload arquivoUpload

    @Autowired
    GaleriaService galeriaService

    def "remove lote de fotos, preserva videos e expurga os originais do lote"() {
        given:
        def galeria = galeriaRepository.save(criarGaleria("lote", 1))
        def foto1 = criarMidia(galeria, MidiaTipo.FOTO, "foto-1.jpg", 1)
        def foto2 = criarMidia(galeria, MidiaTipo.FOTO, "foto-2.jpg", 2)
        def video = criarMidia(galeria, MidiaTipo.VIDEO, "video.mp4", 1)

        when:
        def resultado = galeriaService.removerFotos(galeria.id, [foto1.id, foto2.id])

        then:
        resultado.quantidadeRemovida() == 2
        resultado.quantidadeIgnorada() == 0
        midiaRepository.findById(foto1.id).get().status == MidiaStatus.REMOVIDA
        midiaRepository.findById(foto2.id).get().status == MidiaStatus.REMOVIDA
        midiaRepository.findById(video.id).get().status == MidiaStatus.DISPONIVEL

        def ativos = arquivoConsulta.arquivosDaReferencia(galeria.id)
        ativos.findAll { it.finalidade() == ArquivoFinalidade.FOTO_ORIGINAL.name() }.isEmpty()
        ativos.any { it.finalidade() == ArquivoFinalidade.VIDEO_ORIGINAL.name() }
    }

    def "nao remove lote quando um video esta na lista"() {
        given:
        def galeria = galeriaRepository.save(criarGaleria("video-no-lote", 1))
        def foto = criarMidia(galeria, MidiaTipo.FOTO, "foto.jpg", 1)
        def video = criarMidia(galeria, MidiaTipo.VIDEO, "video.mp4", 1)

        when:
        galeriaService.removerFotos(galeria.id, [foto.id, video.id])

        then:
        def erro = thrown(RegraDeNegocioException)
        erro.codigo == "REMOCAO_LOTE_APENAS_FOTOS"
        midiaRepository.findById(foto.id).get().status != MidiaStatus.REMOVIDA
    }

    def "bloqueia remocao depois da selecao enviada"() {
        given:
        def galeria = criarGaleria("selecao-enviada", 0)
        galeria.registrarSelecao("selecao-1", null)
        galeriaRepository.save(galeria)
        def foto = criarMidia(galeria, MidiaTipo.FOTO, "foto.jpg", 1)

        when:
        galeriaService.removerMidia(galeria.id, foto.id)

        then:
        def erro = thrown(RegraDeNegocioException)
        erro.codigo == "MIDIA_NAO_REMOVIVEL"
        midiaRepository.findById(foto.id).get().status != MidiaStatus.REMOVIDA
    }

    def "nao remove midia ja entregue"() {
        given:
        def galeria = galeriaRepository.save(criarGaleria("entregue", 0))
        def foto = criarMidia(galeria, MidiaTipo.FOTO, "foto.jpg", 1)
        foto.entregar()
        midiaRepository.saveAndFlush(foto)

        when:
        galeriaService.removerMidia(galeria.id, foto.id)

        then:
        def erro = thrown(RegraDeNegocioException)
        erro.codigo == "MIDIA_ENTREGUE_NAO_PODE_SER_REMOVIDA"
    }

    def "limpa itens da selecao reaberta ao remover foto"() {
        given:
        def galeria = criarGaleria("reaberta", 0)
        galeria.publicarSelecao()
        galeriaRepository.save(galeria)
        def foto = criarMidia(galeria, MidiaTipo.FOTO, "foto.jpg", 1)

        def selecao = SelecaoGaleria.criar(
                galeria.id, "ACE-2026-0001", 1, 0, 0, BigDecimal.ZERO, null, null)
        selecao.reabrir()
        selecaoRepository.save(selecao)
        selecaoItemRepository.save(SelecaoItem.criar(selecao.id, foto.id, MidiaTipo.FOTO))

        when:
        galeriaService.removerMidia(galeria.id, foto.id)

        then:
        selecaoItemRepository.findBySelecaoId(selecao.id).isEmpty()
        selecaoRepository.findById(selecao.id).get().fotosSelecionadas == 0
    }

    def "expurga o original do video removido individualmente"() {
        given:
        def galeria = galeriaRepository.save(criarGaleria("video", 1))
        def video = criarMidia(galeria, MidiaTipo.VIDEO, "video.mp4", 1)

        when:
        galeriaService.removerMidia(galeria.id, video.id)

        then:
        midiaRepository.findById(video.id).get().status == MidiaStatus.REMOVIDA
        arquivoConsulta.arquivosDaReferencia(galeria.id)
                .every { it.finalidade() != ArquivoFinalidade.VIDEO_ORIGINAL.name() }
    }

    def "nao expurga original compartilhado por deduplicacao"() {
        given:
        def galeria = galeriaRepository.save(criarGaleria("dedup", 0))
        def original = arquivoUpload.salvarDisponivel(
                new ArquivoUpload.IniciarArquivo(
                        ArquivoFinalidade.FOTO_ORIGINAL,
                        galeria.id,
                        "testes/dedup",
                        "foto.jpg",
                        "image/jpeg"),
                [1, 2, 3] as byte[])
        def primeira = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, original.id(), "foto.jpg",
                "image/jpeg", 3, 1))
        def segunda = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, original.id(), "foto.jpg",
                "image/jpeg", 3, 2))

        when:
        galeriaService.removerFotos(galeria.id, [primeira.id])

        then:
        midiaRepository.findById(primeira.id).get().status == MidiaStatus.REMOVIDA
        arquivoConsulta.arquivosDaReferencia(galeria.id).any {
            it.finalidade() == ArquivoFinalidade.FOTO_ORIGINAL.name()
        }
        midiaRepository.findById(segunda.id).get().status != MidiaStatus.REMOVIDA
    }

    def "bloqueia upload de video quando o pacote nao inclui video"() {
        given:
        def galeria = galeriaRepository.save(criarGaleria("sem-video", 0))
        def request = new GaleriaDtos.IniciarMidiaUploadRequest(
                "video.mp4", "video/mp4", 1024, "a" * 64, "video", "edicao")

        when:
        galeriaService.iniciarUpload(galeria.id, request)

        then:
        def erro = thrown(RegraDeNegocioException)
        erro.codigo == "VIDEOS_NAO_INCLUSOS"
    }

    private MidiaGaleria criarMidia(Galeria galeria, MidiaTipo tipo, String nome, int ordem) {
        def finalidade = tipo == MidiaTipo.FOTO
                ? ArquivoFinalidade.FOTO_ORIGINAL : ArquivoFinalidade.VIDEO_ORIGINAL
        def mime = tipo == MidiaTipo.FOTO ? "image/jpeg" : "video/mp4"
        def arquivo = arquivoUpload.salvarDisponivel(
                new ArquivoUpload.IniciarArquivo(
                        finalidade, galeria.id, "testes/${galeria.id}", nome, mime),
                [1, 2, 3, 4] as byte[])
        return midiaRepository.save(MidiaGaleria.criar(
                galeria.id, tipo, arquivo.id(), nome, mime, 4, ordem))
    }

    private static Galeria criarGaleria(String sufixo, int videosInclusos) {
        return Galeria.criar(
                "estudio", "agendamento-" + sufixo, null, "cliente-" + sufixo,
                "Cliente " + sufixo, 10, videosInclusos, BigDecimal.ZERO)
    }
}
