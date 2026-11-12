package com.ranny.fotografia.notificacao

import com.ranny.fotografia.cliente.ClienteConsulta
import com.ranny.fotografia.cliente.ClienteResumo
import com.ranny.fotografia.shared.events.LoteLiberadoEvent
import spock.lang.Specification

class NotificacaoWhatsAppListenerSpec extends Specification {

    def "monta o comando com link publico e whatsapp do cliente"() {
        given:
        def porta = new PortaFake()
        def clienteConsulta = Stub(ClienteConsulta) {
            buscar("cli-1") >> Optional.of(new ClienteResumo(
                    "cli-1", "Marina", "11999998888", null, null, null, null))
        }
        def propriedades = new NotificacaoWhatsAppProperties(
                new NotificacaoWhatsAppProperties.WhatsApp(false, "log"),
                "https://app.exemplo.com/")
        def listener = new NotificacaoWhatsAppListener(porta, propriedades, clienteConsulta)

        when:
        listener.aoLiberarLote(new LoteLiberadoEvent(
                "g1", "cli-1", "tok-1", 2, 4, 10))

        then:
        porta.comando != null
        porta.comando.galeriaId() == "g1"
        porta.comando.clienteNome() == "Marina"
        porta.comando.whatsapp() == "11999998888"
        porta.comando.link() == "https://app.exemplo.com/galeria/tok-1"
        porta.comando.quantidadeLiberada() == 2
        porta.comando.totalLiberado() == 4
        porta.comando.totalSelecionado() == 10
    }

    def "ignora o evento quando o cliente nao tem whatsapp"() {
        given:
        def porta = new PortaFake()
        def clienteConsulta = Stub(ClienteConsulta) {
            buscar("cli-1") >> Optional.of(new ClienteResumo(
                    "cli-1", "Marina", null, null, null, null, null))
        }
        def propriedades = new NotificacaoWhatsAppProperties(
                new NotificacaoWhatsAppProperties.WhatsApp(false, "log"),
                "https://app.exemplo.com")
        def listener = new NotificacaoWhatsAppListener(porta, propriedades, clienteConsulta)

        when:
        listener.aoLiberarLote(new LoteLiberadoEvent(
                "g1", "cli-1", "tok-1", 1, 1, 1))

        then:
        porta.comando == null
    }

    private static class PortaFake implements NotificacaoWhatsAppPort {
        NotificacaoWhatsAppCommand comando

        @Override
        void enviar(NotificacaoWhatsAppCommand comando) {
            this.comando = comando
        }
    }
}
