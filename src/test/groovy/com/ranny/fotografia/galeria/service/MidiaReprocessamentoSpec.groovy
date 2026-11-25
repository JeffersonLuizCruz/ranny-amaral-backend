package com.ranny.fotografia.galeria.service

import com.ranny.fotografia.arquivo.ArquivoConsulta
import com.ranny.fotografia.arquivo.ArquivoFinalidade
import com.ranny.fotografia.arquivo.ArquivoUpload
import com.ranny.fotografia.galeria.model.Galeria
import com.ranny.fotografia.galeria.model.MidiaGaleria
import com.ranny.fotografia.galeria.model.MidiaStatus
import com.ranny.fotografia.galeria.model.MidiaTipo
import com.ranny.fotografia.galeria.repository.GaleriaRepository
import com.ranny.fotografia.galeria.repository.MidiaDerivadoRepository
import com.ranny.fotografia.galeria.repository.MidiaGaleriaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Regressao do incidente de producao: remover e reenviar a mesma foto fazia o
 * processamento colidir com os derivados antigos (uq_arquivos_checksum_disponivel)
 * e a midia ficava presa em PROCESSANDO.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MidiaReprocessamentoSpec extends Specification {

    @Autowired
    GaleriaRepository galeriaRepository

    @Autowired
    MidiaGaleriaRepository midiaRepository

    @Autowired
    MidiaDerivadoRepository derivadoRepository

    @Autowired
    ArquivoConsulta arquivoConsulta

    @Autowired
    ArquivoUpload arquivoUpload

    @Autowired
    GaleriaService galeriaService

    def "reaproveita derivados identicos quando duas midias usam o mesmo original"() {
        given:
        def imagem = imagemJpeg()
        def galeria = galeriaRepository.save(criarGaleria("reuso"))
        def original = arquivoUpload.salvarDisponivel(
                new ArquivoUpload.IniciarArquivo(
                        ArquivoFinalidade.FOTO_ORIGINAL,
                        galeria.id,
                        "testes/reuso",
                        "foto.jpg",
                        "image/jpeg"),
                imagem)
        def primeira = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, original.id(), "foto.jpg",
                "image/jpeg", imagem.length, 1))
        galeriaService.processarMidiaSolicitada(
                galeria.id, primeira.id, original.id(), false)
        primeira.remover()
        midiaRepository.saveAndFlush(primeira)
        def segunda = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, original.id(), "foto.jpg",
                "image/jpeg", imagem.length, 2))

        when:
        galeriaService.processarMidiaSolicitada(
                galeria.id, segunda.id, original.id(), false)

        then:
        def processada = midiaRepository.findById(segunda.id).get()
        processada.status == MidiaStatus.DISPONIVEL
        derivadoRepository.findByMidiaId(segunda.id).size() == 4
        derivadoRepository.findByMidiaId(segunda.id).collect { it.arquivoId }.toSet() ==
                derivadoRepository.findByMidiaId(primeira.id).collect { it.arquivoId }.toSet()
        derivados(galeria.id).size() == 4
    }

    def "remove derivados e preserva o original ao excluir a midia"() {
        given:
        def imagem = imagemJpeg()
        def galeria = galeriaRepository.save(criarGaleria("exclusao"))
        def original = arquivoUpload.salvarDisponivel(
                new ArquivoUpload.IniciarArquivo(
                        ArquivoFinalidade.FOTO_ORIGINAL,
                        galeria.id,
                        "testes/exclusao",
                        "foto.jpg",
                        "image/jpeg"),
                imagem)
        def midia = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, original.id(), "foto.jpg",
                "image/jpeg", imagem.length, 1))
        galeriaService.processarMidiaSolicitada(galeria.id, midia.id, original.id(), false)

        when:
        galeriaService.removerMidia(galeria.id, midia.id)

        then:
        midiaRepository.findById(midia.id).get().status == MidiaStatus.REMOVIDA
        derivadoRepository.findByMidiaId(midia.id).isEmpty()
        derivados(galeria.id).isEmpty()
        arquivoConsulta.arquivosDaReferencia(galeria.id).any {
            it.finalidade() == ArquivoFinalidade.FOTO_ORIGINAL.name()
        }
    }

    def "reprocessa midia em falha no modo sincrono"() {
        given:
        def imagem = imagemJpeg()
        def galeria = galeriaRepository.save(criarGaleria("reprocesso"))
        def original = arquivoUpload.salvarDisponivel(
                new ArquivoUpload.IniciarArquivo(
                        ArquivoFinalidade.FOTO_ORIGINAL,
                        galeria.id,
                        "testes/reprocesso",
                        "foto.jpg",
                        "image/jpeg"),
                imagem)
        def midia = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, original.id(), "foto.jpg",
                "image/jpeg", imagem.length, 1))
        galeriaService.processarMidiaSolicitada(galeria.id, midia.id, original.id(), false)
        midia.falhar("erro simulado")
        midiaRepository.saveAndFlush(midia)

        when:
        galeriaService.reprocessarMidia(galeria.id, midia.id)

        then:
        def atual = midiaRepository.findById(midia.id).get()
        atual.status == MidiaStatus.DISPONIVEL
        atual.erroProcessamento == null
        derivadoRepository.findByMidiaId(midia.id).size() == 4
        derivados(galeria.id).size() == 4
    }

    def "reprocessa em lote apenas fotos presas ou com falha"() {
        given:
        def imagem = imagemJpeg()
        def galeria = galeriaRepository.save(criarGaleria("lote"))
        def original = arquivoUpload.salvarDisponivel(
                new ArquivoUpload.IniciarArquivo(
                        ArquivoFinalidade.FOTO_ORIGINAL,
                        galeria.id,
                        "testes/lote",
                        "foto.jpg",
                        "image/jpeg"),
                imagem)
        def disponivel = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, original.id(), "foto.jpg",
                "image/jpeg", imagem.length, 1))
        galeriaService.processarMidiaSolicitada(
                galeria.id, disponivel.id, original.id(), false)
        def presa = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, original.id(), "foto.jpg",
                "image/jpeg", imagem.length, 2))

        when:
        def resultado = galeriaService.reprocessarPendentes(galeria.id)

        then:
        resultado.quantidade() == 1
        midiaRepository.findById(presa.id).get().status == MidiaStatus.DISPONIVEL
        midiaRepository.findById(disponivel.id).get().status == MidiaStatus.DISPONIVEL
        derivados(galeria.id).size() == 4
    }

    private def derivados(String galeriaId) {
        return arquivoConsulta.arquivosDaReferencia(galeriaId)
                .findAll { it.finalidade() == ArquivoFinalidade.FOTO_DERIVADO.name() }
    }

    private static Galeria criarGaleria(String sufixo) {
        return Galeria.criar(
                "estudio", "agendamento-" + sufixo, null, "cliente-" + sufixo,
                "Cliente " + sufixo, 2, 0, BigDecimal.ZERO)
    }

    private static byte[] imagemJpeg() {
        def imagem = new BufferedImage(140, 90, BufferedImage.TYPE_INT_RGB)
        def graphics = imagem.createGraphics()
        graphics.setColor(Color.BLUE)
        graphics.fillRect(0, 0, imagem.width, imagem.height)
        graphics.dispose()
        def saida = new ByteArrayOutputStream()
        ImageIO.write(imagem, "jpg", saida)
        return saida.toByteArray()
    }
}
