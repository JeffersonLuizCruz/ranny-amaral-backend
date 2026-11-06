package com.ranny.fotografia.shared

import com.ranny.fotografia.shared.events.ContratoStatusAlteradoEvent
import com.ranny.fotografia.shared.json.JsonApoio
import spock.lang.Specification

/**
 * Garante que o evento persistido pelo outbox pode ser serializado, lido
 * novamente e cabe em {@code varchar(255)} (coluna do event_publication).
 */
class EventSerializationSpec extends Specification {

    def "serializa, desserializa e limita o evento de contrato"() {
        given:
        def evento = new ContratoStatusAlteradoEvent(
                "4f8b2c1e-9a3d-4e6f-8a1b-2c3d4e5f6a7b",
                "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d",
                "aceito",
                "confirmado",
                new BigDecimal("300.00"),
                "confirmado",
                new BigDecimal("700.00"),
                "confirmado")

        when:
        def json = JsonApoio.escrever(evento)
        def lido = JsonApoio.ler(json, ContratoStatusAlteradoEvent)

        then:
        json.length() <= 255
        lido.contratoId() == evento.contratoId()
        lido.agendamentoId() == evento.agendamentoId()
        lido.statusAceite() == evento.statusAceite()
        lido.entradaValor() == evento.entradaValor()
        lido.saldoStatus() == evento.saldoStatus()
    }
}
