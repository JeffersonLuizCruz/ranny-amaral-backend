package com.ranny.fotografia.contrato.model

import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot
import com.ranny.fotografia.shared.json.JsonApoio
import spock.lang.Specification

/** Garante compatibilidade com snapshots gravados antes da taxa de deslocamento. */
class ContratoSnapshotJsonSpec extends Specification {

    def "le snapshot antigo sem o campo deslocamento"() {
        given:
        def json = '''
            {
              "cliente": { "nome": "Marina Alves" },
              "pacote": null,
              "agendamento": null,
              "estudio": { "nome": "Estúdio" },
              "modeloVersao": 1
            }
        '''

        when:
        ContratoSnapshot snapshot = JsonApoio.ler(json, ContratoSnapshot)

        then:
        snapshot != null
        snapshot.deslocamento() == null
        snapshot.cliente().nome() == "Marina Alves"
        snapshot.modeloVersao() == 1
    }

    def "preserva o deslocamento no round-trip do snapshot"() {
        given:
        def original = new ContratoSnapshot(
                new ContratoSnapshot.ClienteSnapshot("Marina Alves", null, null, null, null, null),
                null,
                null,
                new ContratoSnapshot.EstudioSnapshot("Estúdio", null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null),
                new ContratoSnapshot.DeslocamentoSnapshot(
                        new BigDecimal("50.00"), true,
                        new BigDecimal("50.00"), BigDecimal.ZERO),
                1)

        when:
        ContratoSnapshot lido = JsonApoio.ler(JsonApoio.escrever(original), ContratoSnapshot)

        then:
        lido.deslocamento().valor() == new BigDecimal("50.00")
        lido.deslocamento().repassado()
        lido.deslocamento().valorCobrado() == new BigDecimal("50.00")
    }
}
