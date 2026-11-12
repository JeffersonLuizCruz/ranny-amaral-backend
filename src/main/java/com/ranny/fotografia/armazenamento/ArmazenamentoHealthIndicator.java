package com.ranny.fotografia.armazenamento;

import com.ranny.fotografia.armazenamento.model.AmostraArmazenamento;
import com.ranny.fotografia.armazenamento.repository.AmostraArmazenamentoRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Expoe o alerta de disco no actuator.
 *
 * <p>Fica em DEGRADED quando o disco cruza o percentual configurado ou quando o
 * livre chega perto da reserva, para o operador ver antes do upload falhar.
 */
@Component
public class ArmazenamentoHealthIndicator implements HealthIndicator {

    private final AmostraArmazenamentoRepository repository;
    private final ArmazenamentoProperties properties;

    public ArmazenamentoHealthIndicator(AmostraArmazenamentoRepository repository,
                                        ArmazenamentoProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public Health health() {
        return repository.findFirstByOrderByColetadoEmDesc()
                .map(this::avaliar)
                .orElseGet(() -> Health.up()
                        .withDetail("coleta", "ainda nao executada")
                        .build());
    }

    private Health avaliar(AmostraArmazenamento amostra) {
        Long total = amostra.getTotalBytes();
        Long livre = amostra.getLivreBytes();
        Health.Builder builder = Health.up()
                .withDetail("coletadoEm", amostra.getColetadoEm().toString())
                .withDetail("origemColeta", amostra.getOrigemColeta().name())
                .withDetail("objetosBytes", amostra.getObjetosBytes())
                .withDetail("outrosBytes", amostra.getOutrosBytes())
                .withDetail("reservaBytes", amostra.getReservaBytes());
        if (total == null || livre == null || total <= 0) {
            return builder.withDetail("alerta", "INDISPONIVEL").build();
        }
        long usado = Math.max(0, total - livre);
        int percentual = (int) Math.round(usado * 100.0 / total);
        String alerta = "OK";
        if (livre <= properties.getReservaBytes()) {
            alerta = "CRITICO";
        } else if (percentual >= properties.getPercentualAlerta()) {
            alerta = "ATENCAO";
        }
        builder.withDetail("percentualUsado", percentual).withDetail("alerta", alerta);
        return "OK".equals(alerta) ? builder.build() : builder.status("DEGRADED").build();
    }
}
