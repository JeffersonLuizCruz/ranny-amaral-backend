package com.ranny.fotografia.galeria.service

import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.Arrays

class ThumbnailatorProcessadorMidiaSpec extends Specification {

    def "gera versoes limpas e marcadas para thumb e web"() {
        given:
        def imagem = new BufferedImage(200, 140, BufferedImage.TYPE_INT_RGB)
        def graphics = imagem.createGraphics()
        graphics.setColor(new Color(40, 80, 120))
        graphics.fillRect(0, 0, imagem.width, imagem.height)
        graphics.dispose()
        def saida = new ByteArrayOutputStream()
        ImageIO.write(imagem, 'jpg', saida)

        when:
        def resultado = new ThumbnailatorProcessadorMidia()
                .processar(saida.toByteArray(), 'foto.jpg', 'Estudio Teste')

        then:
        resultado.derivados().size() == 4
        resultado.derivados().count { it.marcada() } == 2
        resultado.derivados().findAll { it.marcada() }.every {
            it.conteudo().length > 0
        }
        !Arrays.equals(
                resultado.derivados().find { it.tipo() == 'THUMB' && !it.marcada() }
                        .conteudo(),
                resultado.derivados().find { it.tipo() == 'THUMB' && it.marcada() }
                        .conteudo())
    }
}
