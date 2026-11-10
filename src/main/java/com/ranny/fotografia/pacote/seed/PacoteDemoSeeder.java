package com.ranny.fotografia.pacote.seed;

import com.ranny.fotografia.pacote.model.FaixaFotoExtra;
import com.ranny.fotografia.pacote.model.Pacote;
import com.ranny.fotografia.pacote.model.PromocaoPacote;
import com.ranny.fotografia.pacote.repository.PacoteRepository;
import com.ranny.fotografia.shared.seed.DadosDemo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Seed idempotente do catalogo de pacotes. */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(20)
public class PacoteDemoSeeder implements ApplicationRunner {

    private final PacoteRepository pacoteRepository;

    public PacoteDemoSeeder(PacoteRepository pacoteRepository) {
        this.pacoteRepository = pacoteRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (pacoteRepository.count() > 0) {
            return;
        }
        pacoteRepository.save(Pacote.restaurar(
                DadosDemo.PACOTE_GESTANTE, "Ensaio Gestante Standard", 90,
                new BigDecimal("890.00"),
                "Ensaio em estúdio com direção de poses e iluminação suave.",
                "#7C3AED",
                List.of("1 look com troca", "20 fotos tratadas", "Galeria online por 90 dias", "1 impressão 20x30"),
                true, 20, 0, new BigDecimal("45.00"),
                PromocaoPacote.de(true, new BigDecimal("690.00"), null, "Condição especial de lançamento")));
        pacoteRepository.save(Pacote.restaurar(
                DadosDemo.PACOTE_CORPORATIVO, "Ensaio Corporativo Premium", 120,
                new BigDecimal("1490.00"),
                "Retratos profissionais para equipes, executivos e LinkedIn.",
                "#0284C7",
                List.of("2 looks", "40 fotos tratadas", "1 vídeo institucional curto", "Retoque premium",
                        "Licença de uso comercial", "Galeria online por 180 dias"),
                true, 40, 1, new BigDecimal("55.00"), null));
        pacoteRepository.save(Pacote.restaurar(
                DadosDemo.PACOTE_FAMILIA, "Ensaio Família Essencial", 60,
                new BigDecimal("590.00"),
                "Registro afetivo em estúdio ou externo, com direção leve e natural.",
                "#D97706",
                List.of("1 look", "15 fotos tratadas", "Galeria online por 60 dias", "1 foto impressa"),
                true, 15, 0, new BigDecimal("35.00"), null));
        pacoteRepository.save(Pacote.restaurar(
                DadosDemo.PACOTE_RETRATO, "Ensaio Retrato Promocional", 45,
                new BigDecimal("300.00"),
                "Pacote promocional de retratos com 20 fotos por um valor fechado.",
                "#E11D48",
                List.of("20 fotos tratadas", "Galeria online por 30 dias"),
                true, 20, 0, new BigDecimal("15.00"),
                List.of(FaixaFotoExtra.de(11, new BigDecimal("10.00"))),
                PromocaoPacote.de(true, new BigDecimal("200.00"), null, "Leve 20 fotos por R$ 10 cada")));
    }
}
