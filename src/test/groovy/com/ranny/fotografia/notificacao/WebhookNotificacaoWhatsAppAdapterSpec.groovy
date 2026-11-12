package com.ranny.fotografia.notificacao

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class WebhookNotificacaoWhatsAppAdapterSpec extends Specification {

    def "envia payload e bearer token para o webhook"() {
        given:
        def capturado = [body: null, auth: null]
        def server = servidor(200) { exchange ->
            capturado.body = exchange.requestBody.text
            capturado.auth = exchange.requestHeaders.getFirst("Authorization")
        }
        def porta = server.address.port
        def propriedades = new NotificacaoWhatsAppProperties(
                new NotificacaoWhatsAppProperties.WhatsApp(
                        true, "webhook",
                        "http://localhost:${porta}/webhook", "token-qa"),
                "http://app.local")
        def adapter = new WebhookNotificacaoWhatsAppAdapter(
                propriedades, new ObjectMapper())

        when:
        adapter.enviar(new NotificacaoWhatsAppCommand(
                "g1", "c1", "Marina", "(11) 99999-8888",
                "http://app.local/galeria/tok-1", 2, 4, 10))

        then:
        capturado.auth == "Bearer token-qa"
        capturado.body.contains('"to":"11999998888"')
        capturado.body.contains('"template":"lote_liberado"')
        capturado.body.contains('"link":"http://app.local/galeria/tok-1"')
        capturado.body.contains('"quantidadeLiberada":2')
        capturado.body.contains('"totalLiberado":4')
        capturado.body.contains('"totalSelecionado":10')

        cleanup:
        server.stop(0)
    }

    def "falha quando o webhook responde erro"() {
        given:
        def server = servidor(500) { exchange -> }
        def porta = server.address.port
        def propriedades = new NotificacaoWhatsAppProperties(
                new NotificacaoWhatsAppProperties.WhatsApp(
                        true, "webhook",
                        "http://localhost:${porta}/webhook", null),
                "http://app.local")
        def adapter = new WebhookNotificacaoWhatsAppAdapter(
                propriedades, new ObjectMapper())

        when:
        adapter.enviar(new NotificacaoWhatsAppCommand(
                "g1", "c1", "Marina", "11999998888",
                "http://app.local/galeria/tok-1", 1, 1, 1))

        then:
        thrown(IllegalStateException)

        cleanup:
        server.stop(0)
    }

    def "nao envia quando a flag esta desligada"() {
        given:
        def propriedades = new NotificacaoWhatsAppProperties(
                new NotificacaoWhatsAppProperties.WhatsApp(
                        false, "webhook", "http://localhost:1/webhook", null),
                "http://app.local")
        def adapter = new WebhookNotificacaoWhatsAppAdapter(
                propriedades, new ObjectMapper())

        when:
        adapter.enviar(new NotificacaoWhatsAppCommand(
                "g1", "c1", "Marina", "11999998888",
                "http://app.local/galeria/tok-1", 1, 1, 1))

        then:
        noExceptionThrown()
    }

    private static HttpServer servidor(int status,
                                       Closure<Void> captura) {
        def server = HttpServer.create(new java.net.InetSocketAddress(0), 0)
        server.createContext("/webhook", { exchange ->
            captura.call(exchange)
            exchange.sendResponseHeaders(status, -1)
            exchange.close()
        } as HttpHandler)
        server.start()
        return server
    }
}
