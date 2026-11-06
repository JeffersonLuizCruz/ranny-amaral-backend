package com.ranny.fotografia.galeria.service

import com.ranny.fotografia.arquivo.ArquivoFinalidade
import com.ranny.fotografia.arquivo.ArquivoUpload
import com.ranny.fotografia.galeria.event.MidiaProcessamentoEvent
import com.ranny.fotografia.galeria.model.Galeria
import com.ranny.fotografia.galeria.model.MidiaGaleria
import com.ranny.fotografia.galeria.model.MidiaStatus
import com.ranny.fotografia.galeria.model.MidiaTipo
import com.ranny.fotografia.galeria.repository.GaleriaRepository
import com.ranny.fotografia.galeria.repository.MidiaDerivadoRepository
import com.ranny.fotografia.galeria.repository.MidiaGaleriaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage

@SpringBootTest(properties = "app.galeria.processamento-async=true")
@ActiveProfiles("test")
class MidiaProcessamentoAsyncSpec extends Specification {

    @Autowired
    GaleriaRepository galeriaRepository

    @Autowired
    MidiaGaleriaRepository midiaRepository

    @Autowired
    MidiaDerivadoRepository derivadoRepository

    @Autowired
    ArquivoUpload arquivoUpload

    @Autowired
    ApplicationEventPublisher eventos

    @Autowired
    PlatformTransactionManager transactionManager

    def "processa derivados em background depois do commit"() {
        given:
        def imagem = imagemJpeg()
        def galeria = galeriaRepository.save(Galeria.criar(
                "estudio", "agendamento-async", null, "cliente-async",
                "Cliente Async", 1, 0, BigDecimal.ZERO))
        def arquivo = arquivoUpload.salvarDisponivel(
                new ArquivoUpload.IniciarArquivo(
                        ArquivoFinalidade.FOTO_ORIGINAL,
                        galeria.id,
                        "testes/async",
                        "foto.jpg",
                        "image/jpeg"),
                imagem)
        def midia = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, arquivo.id(), "foto.jpg",
                "image/jpeg", imagem.length, 1))

        when:
        new TransactionTemplate(transactionManager).executeWithoutResult {
            eventos.publishEvent(new MidiaProcessamentoEvent(
                    galeria.id, midia.id, arquivo.id(), false))
        }

        def processada = aguardarProcessamento(midia.id)

        then:
        processada != null
        processada.status == MidiaStatus.DISPONIVEL
        processada.largura != null
        !derivadoRepository.findByMidiaId(midia.id).isEmpty()
    }

    private MidiaGaleria aguardarProcessamento(String midiaId) {
        long limite = System.currentTimeMillis() + 5000
        MidiaGaleria ultima = null
        while (System.currentTimeMillis() < limite) {
            ultima = midiaRepository.findById(midiaId).orElse(null)
            if (ultima != null && ultima.status == MidiaStatus.DISPONIVEL) {
                return ultima
            }
            Thread.sleep(100)
        }
        return ultima
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
