package com.ranny.fotografia.notificacao;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificacaoWhatsAppProperties.class)
public class NotificacaoConfig {
}
