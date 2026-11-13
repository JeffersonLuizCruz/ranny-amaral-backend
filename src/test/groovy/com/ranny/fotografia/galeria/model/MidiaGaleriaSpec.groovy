package com.ranny.fotografia.galeria.model

import spock.lang.Specification

class MidiaGaleriaSpec extends Specification {

    def "foto pronta na origem dispensa edicao e RAW"() {
        given:
        def midia = MidiaGaleria.criar(
                'g1', MidiaTipo.FOTO, 'arquivo-1', 'foto.jpg', 'image/jpeg',
                1024, 1, MidiaFinalidade.PRONTA)

        when:
        midia.disponivel(1200, 800, null)

        then:
        midia.prontaNaOrigem()
        midia.prontaParaEntrega(false)
        midia.prontaParaEntrega(true)
    }

    def "foto de edicao so fica pronta com versao editada ou RAW liberado"() {
        given:
        def midia = MidiaGaleria.criar(
                'g1', MidiaTipo.FOTO, 'arquivo-1', 'foto.jpg', 'image/jpeg',
                1024, 1, MidiaFinalidade.EDICAO)
        midia.disponivel(1200, 800, null)

        expect:
        !midia.prontaNaOrigem()
        !midia.prontaParaEntrega(false)

        when:
        midia.vincularEditada('arquivo-editado', 1200, 800)

        then:
        midia.prontaParaEntrega(false)
    }

    def "RAW liberado torna a foto de edicao entregavel"() {
        given:
        def midia = MidiaGaleria.criar(
                'g1', MidiaTipo.FOTO, 'arquivo-1', 'foto.jpg', 'image/jpeg',
                1024, 1)
        midia.disponivel(1200, 800, null)

        expect:
        midia.prontaParaEntrega(true)
    }
}
