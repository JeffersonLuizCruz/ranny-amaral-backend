package com.ranny.fotografia.shared.financeiro

import com.ranny.fotografia.shared.model.FormaPagamentoSaldo
import com.ranny.fotografia.shared.model.PagamentoEntrada
import com.ranny.fotografia.shared.model.PagamentoSaldo
import spock.lang.Specification

import java.time.Instant

class ResumoPagamentoSpec extends Specification {

    def "considera saldo quitado somente quando o valor recebido cobre o previsto"() {
        given:
        def entrada = PagamentoEntrada.confirmada(
                new BigDecimal("300.00"), new BigDecimal("30.00"), Instant.now(), "Teste")
        def saldoParcial = PagamentoSaldo.recebido(
                new BigDecimal("100.00"), FormaPagamentoSaldo.PIX, Instant.now(), "Teste")
        def saldoIntegral = PagamentoSaldo.recebido(
                new BigDecimal("700.00"), FormaPagamentoSaldo.PIX, Instant.now(), "Teste")

        when:
        def parcial = ResumoPagamento.de(
                entrada, saldoParcial, new BigDecimal("300.00"), new BigDecimal("700.00"))
        def integral = ResumoPagamento.de(
                entrada, saldoIntegral, new BigDecimal("300.00"), new BigDecimal("700.00"))

        then:
        !parcial.saldoQuitado()
        parcial.valorRecebido().compareTo(new BigDecimal("400.00")) == 0
        parcial.valorPendente().compareTo(new BigDecimal("600.00")) == 0

        integral.saldoQuitado()
        integral.quitado()
        integral.valorRecebido().compareTo(new BigDecimal("1000.00")) == 0
        integral.valorPendente().compareTo(BigDecimal.ZERO) == 0
    }

    def "calcula pendências e trata isenção como quitada"() {
        given:
        def entradaPendente = PagamentoEntrada.pendente(
                new BigDecimal("300.00"), new BigDecimal("30.00"))
        def saldoPendente = PagamentoSaldo.pendente(new BigDecimal("700.00"))
        def entradaIsenta = PagamentoEntrada.pendente(BigDecimal.ZERO, BigDecimal.ZERO)
        def saldoIsento = PagamentoSaldo.pendente(BigDecimal.ZERO)

        when:
        def pendente = ResumoPagamento.de(
                entradaPendente, saldoPendente, new BigDecimal("300.00"), new BigDecimal("700.00"))
        def isento = ResumoPagamento.de(
                entradaIsenta, saldoIsento, BigDecimal.ZERO, BigDecimal.ZERO)

        then:
        !pendente.entradaQuitada()
        !pendente.saldoQuitado()
        pendente.valorPendente().compareTo(new BigDecimal("1000.00")) == 0

        isento.entradaQuitada()
        isento.saldoQuitado()
        isento.quitado()
        isento.valorRecebido().compareTo(BigDecimal.ZERO) == 0
        isento.valorPendente().compareTo(BigDecimal.ZERO) == 0
    }
}
