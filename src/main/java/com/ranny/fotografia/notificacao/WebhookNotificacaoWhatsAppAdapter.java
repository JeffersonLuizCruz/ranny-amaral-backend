package com.ranny.fotografia.notificacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter real e provider-agnostic: envia um POST JSON para um webhook.
 *
 * <p>Serve para Z-API, n8n, gateway interno ou qualquer provedor que aceite
 * webhook. O contrato do payload e intencionalmente simples; a implementacao
 * direta da Meta Cloud API pode substituir esta classe sem mudar o dominio.
 */
@Component
@ConditionalOnProperty(name = "app.notificacao.whatsapp.provedor",
        havingValue = "webhook")
public class WebhookNotificacaoWhatsAppAdapter implements NotificacaoWhatsAppPort {

    private final NotificacaoWhatsAppProperties propriedades;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebhookNotificacaoWhatsAppAdapter(NotificacaoWhatsAppProperties propriedades,
                                              ObjectMapper objectMapper) {
        this.propriedades = propriedades;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void enviar(NotificacaoWhatsAppCommand comando) {
        NotificacaoWhatsAppProperties.WhatsApp whatsapp = propriedades.whatsapp();
        if (whatsapp == null || !whatsapp.habilitado()) {
            return;
        }
        if (whatsapp.webhookUrl() == null || whatsapp.webhookUrl().isBlank()) {
            throw new IllegalStateException(
                    "app.notificacao.whatsapp.webhook-url nao configurada.");
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to", somenteDigitos(comando.whatsapp()));
            payload.put("template", "lote_liberado");
            payload.put("clienteNome", comando.clienteNome());
            payload.put("link", comando.link());
            payload.put("quantidadeLiberada", comando.quantidadeLiberada());
            payload.put("totalLiberado", comando.totalLiberado());
            payload.put("totalSelecionado", comando.totalSelecionado());

            HttpRequest.Builder request = HttpRequest.newBuilder(
                            URI.create(whatsapp.webhookUrl()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload)));

            if (whatsapp.webhookToken() != null && !whatsapp.webhookToken().isBlank()) {
                request.header("Authorization", "Bearer " + whatsapp.webhookToken());
            }

            HttpResponse<String> response = httpClient.send(
                    request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Webhook WhatsApp respondeu HTTP " + response.statusCode()
                                + ": " + response.body());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio de WhatsApp interrompido.", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) {
                throw state;
            }
            throw new IllegalStateException("Falha ao enviar WhatsApp pelo webhook.", ex);
        }
    }

    private static String somenteDigitos(String telefone) {
        return telefone == null ? null : telefone.replaceAll("\\D", "");
    }
}
