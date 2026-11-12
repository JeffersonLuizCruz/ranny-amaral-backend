package com.ranny.fotografia.armazenamento;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita a coleta agendada e as propriedades do modulo. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(ArmazenamentoProperties.class)
public class ArmazenamentoConfig {
}
