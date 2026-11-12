package com.ranny.fotografia.armazenamento;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracao do monitoramento de armazenamento.
 *
 * <p>Nao existe tamanho fixo de disco: total e livre sao medidos no filesystem
 * configurado em {@code volume-path}. O disco da VPS hospeda fotos, Postgres e
 * infraestrutura, por isso a reserva e absoluta e configurable.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.armazenamento")
public class ArmazenamentoProperties {

    /** Caminho montado do volume de dados do Garage (mesmo disco do Postgres). */
    private String volumePath = "/var/lib/garage/data";

    /** Margem de seguranca que nunca deve ser ocupada (default 10 GB). */
    private long reservaBytes = 10L * 1024 * 1024 * 1024;

    /** Percentual de uso do disco que dispara alerta. */
    private int percentualAlerta = 85;

    /** Liga a coleta periodica. */
    private boolean coletaHabilitada = true;

    /** Cron da coleta diaria. */
    private String coletaCron = "0 15 3 * * *";

    /** Janela usada para estimar crescimento e dias restantes. */
    private Duration janelaEstimativa = Duration.ofDays(14);
}
