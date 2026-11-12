package com.ranny.fotografia.notificacao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao da notificacao de WhatsApp.
 *
 * <p>O provider real ainda nao esta implementado. O adapter padrao apenas
 * registra a mensagem para que a integracao futura nao exija mudar o dominio.
 */
@ConfigurationProperties(prefix = "app.notificacao")
public record NotificacaoWhatsAppProperties(
        WhatsApp whatsapp,
        String publicBaseUrl
) {

    public record WhatsApp(boolean habilitado, String provedor,
                           String webhookUrl, String webhookToken) {
    }
}
