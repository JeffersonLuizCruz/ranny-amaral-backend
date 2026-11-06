package com.ranny.fotografia.agenda.seed;

import com.ranny.fotografia.agenda.model.Agendamento;
import com.ranny.fotografia.agenda.model.CondicoesPacote;
import com.ranny.fotografia.agenda.model.ContatoReserva;
import com.ranny.fotografia.agenda.model.StatusAgendamento;
import com.ranny.fotografia.agenda.repository.AgendamentoRepository;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoSaldo;
import com.ranny.fotografia.shared.seed.DadosDemo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Seed idempotente da agenda de demonstracao. */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(30)
public class AgendaDemoSeeder implements ApplicationRunner {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final AgendamentoRepository agendamentoRepository;

    public AgendaDemoSeeder(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (agendamentoRepository.count() > 0) {
            return;
        }

        Agendamento gestante = Agendamento.restaurar(
                DadosDemo.AGENDAMENTO_GESTANTE,
                DadosDemo.CLIENTE_MARINA,
                null,
                DadosDemo.PACOTE_GESTANTE,
                DadosDemo.CONTRATO_GESTANTE,
                em(5, 10, 0),
                em(5, 11, 30),
                "Estúdio Photoizer",
                StatusAgendamento.RESERVADO,
                "Levar looks neutros. Confirmar horário no dia anterior.",
                new BigDecimal("890.00"), new BigDecimal("267.00"), new BigDecimal("30.00"),
                CondicoesPacote.de(20, 0, new BigDecimal("45.00"), new BigDecimal("890.00"), null));

        Agendamento corporativo = Agendamento.restaurar(
                DadosDemo.AGENDAMENTO_CORPORATIVO,
                null,
                ContatoReserva.de("Juliana Prado", "(11) 91234-5678"),
                DadosDemo.PACOTE_CORPORATIVO,
                DadosDemo.CONTRATO_CORPORATIVO,
                em(12, 14, 0),
                em(12, 16, 0),
                "Escritório do cliente",
                StatusAgendamento.RESERVADO,
                "Reserva criada sem cliente. Contrato em rascunho aguardando envio.",
                new BigDecimal("1490.00"), new BigDecimal("447.00"), new BigDecimal("30.00"),
                CondicoesPacote.de(40, 1, new BigDecimal("55.00"), new BigDecimal("1490.00"), null));

        Agendamento familia = Agendamento.restaurar(
                DadosDemo.AGENDAMENTO_FAMILIA,
                DadosDemo.CLIENTE_MARINA,
                null,
                DadosDemo.PACOTE_FAMILIA,
                DadosDemo.CONTRATO_FAMILIA,
                em(20, 9, 0),
                em(20, 10, 0),
                "Parque Ibirapuera (externo)",
                StatusAgendamento.REALIZADO,
                "Ensaio realizado. Saldo final recebido em dinheiro.",
                new BigDecimal("590.00"), new BigDecimal("177.00"), new BigDecimal("30.00"),
                CondicoesPacote.de(15, 0, new BigDecimal("35.00"), new BigDecimal("590.00"), null));
        familia.getPagamentoEntrada().confirmar(Instant.now().minus(3, ChronoUnit.DAYS), "Equipe Photoizer");
        familia.atualizarPagamentoFinal(PagamentoSaldo.recebido(
                new BigDecimal("413.00"), FormaPagamentoSaldo.DINHEIRO,
                Instant.now().minus(1, ChronoUnit.DAYS), "Equipe Photoizer"));

        agendamentoRepository.save(gestante);
        agendamentoRepository.save(corporativo);
        agendamentoRepository.save(familia);
    }

    private static OffsetDateTime em(int dia, int hora, int minuto) {
        return LocalDate.now(FUSO)
                .withDayOfMonth(dia)
                .atTime(hora, minuto)
                .atZone(FUSO)
                .toOffsetDateTime();
    }
}
