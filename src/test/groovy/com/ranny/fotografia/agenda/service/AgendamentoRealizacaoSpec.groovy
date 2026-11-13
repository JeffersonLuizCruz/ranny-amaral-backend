package com.ranny.fotografia.agenda.service

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.pacote.repository.PacoteRepository
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo
import com.ranny.fotografia.shared.model.PagamentoEntrada
import com.ranny.fotografia.shared.model.PagamentoSaldo
import com.ranny.fotografia.support.Cenario
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.time.Instant
import java.time.OffsetDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgendamentoRealizacaoSpec extends Specification {

    @Autowired
    AgendaService agendaService

    @Autowired
    AgendamentoRepository agendamentoRepository

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PacoteRepository pacoteRepository

    def "promove apenas atendimentos confirmados com data passada e pagamentos quitados"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def passado = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.now().minusDays(1).withHour(10).withMinute(0)
                        .withSecond(0).withNano(0))
        def futuro = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.now().plusDays(2).withHour(10).withMinute(0)
                        .withSecond(0).withNano(0))
        prepararPagamentosQuitados(passado)
        prepararPagamentosQuitados(futuro)
        agendamentoRepository.saveAndFlush(passado)
        agendamentoRepository.saveAndFlush(futuro)

        when:
        def promovidos = agendaService.promoverRealizados()

        then:
        promovidos == 1
        agendamentoRepository.findById(passado.id).get().status == StatusAgendamento.REALIZADO
        agendamentoRepository.findById(futuro.id).get().status == StatusAgendamento.CONFIRMADO
    }

    def "nao promove atendimento com entrada pendente"() {
        given:
        def cliente = Cenario.cliente(clienteRepository)
        def pacote = Cenario.pacote(pacoteRepository)
        def passado = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                OffsetDateTime.now().minusDays(1).withHour(10).withMinute(0)
                        .withSecond(0).withNano(0))
        passado.atualizarPagamentoFinal(PagamentoSaldo.recebido(
                new BigDecimal("700.00"), FormaPagamentoSaldo.PIX, Instant.now(), "Teste"))
        passado.alterarStatus(StatusAgendamento.CONFIRMADO)
        agendamentoRepository.saveAndFlush(passado)

        when:
        def promovidos = agendaService.promoverRealizados()

        then:
        promovidos == 0
        agendamentoRepository.findById(passado.id).get().status == StatusAgendamento.CONFIRMADO
    }

    private static void prepararPagamentosQuitados(def agendamento) {
        agendamento.atualizarPagamentoEntrada(
                new BigDecimal("300.00"), new BigDecimal("30.00"),
                PagamentoEntrada.confirmada(
                        new BigDecimal("300.00"), new BigDecimal("30.00"),
                        Instant.now(), "Teste"))
        agendamento.atualizarPagamentoFinal(PagamentoSaldo.recebido(
                new BigDecimal("700.00"), FormaPagamentoSaldo.PIX, Instant.now(), "Teste"))
        agendamento.alterarStatus(StatusAgendamento.CONFIRMADO)
    }
}
