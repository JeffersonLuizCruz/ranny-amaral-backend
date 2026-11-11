package com.ranny.fotografia.shared.financeiro

import com.ranny.fotografia.shared.model.FaixaFotoExtraSnapshot
import spock.lang.Specification
import spock.lang.Unroll

class CalculadoraExtrasSpec extends Specification {

    def "sem faixas usa o preco base"() {
        when:
        def resultado = CalculadoraExtras.calcular(new BigDecimal("50.00"), List.of(), 3)

        then:
        resultado.valorTotal == 150.00
        resultado.valorMedioUnitario == 50.00
        resultado.detalhamento.size() == 1
        resultado.detalhamento[0] == new FaixaAplicada(3, new BigDecimal("50.00"), new BigDecimal("150.00"))
    }

    @Unroll
    def "calcula progressivamente #quantidade extras"() {
        given:
        def faixas = [new FaixaFotoExtraSnapshot(11, new BigDecimal("10.00"))]

        when:
        def resultado = CalculadoraExtras.calcular(new BigDecimal("15.00"), faixas, quantidade)

        then:
        resultado.valorTotal == valorEsperado

        where:
        quantidade | valorEsperado
        0          | 0.00
        1          | 15.00
        10         | 150.00
        11         | 160.00
        15         | 200.00
    }

    def "aplica mais de uma faixa e detalha cada trecho"() {
        given:
        def faixas = [
                new FaixaFotoExtraSnapshot(2, new BigDecimal("30.00")),
                new FaixaFotoExtraSnapshot(4, new BigDecimal("20.00"))
        ]

        when:
        def resultado = CalculadoraExtras.calcular(new BigDecimal("50.00"), faixas, 5)

        then:
        resultado.valorTotal == 150.00
        resultado.detalhamento*.quantidade == [1, 2, 2]
        resultado.detalhamento*.valorUnitario == [
                new BigDecimal("50.00"),
                new BigDecimal("30.00"),
                new BigDecimal("20.00")
        ]
        resultado.detalhamento*.subtotal == [
                new BigDecimal("50.00"),
                new BigDecimal("60.00"),
                new BigDecimal("40.00")
        ]
    }

    def "quantidade zero nao gera detalhamento"() {
        when:
        def resultado = CalculadoraExtras.calcular(new BigDecimal("15.00"),
                [new FaixaFotoExtraSnapshot(11, new BigDecimal("10.00"))], 0)

        then:
        resultado.valorTotal == 0.00
        resultado.valorMedioUnitario == 0.00
        resultado.detalhamento.isEmpty()
    }

    def "delta sem cobertura equivale ao calculo total"() {
        given:
        def faixas = [new FaixaFotoExtraSnapshot(11, new BigDecimal("10.00"))]

        when:
        def delta = CalculadoraExtras.calcularDelta(
                new BigDecimal("15.00"), faixas, 12, 0)

        then:
        delta.quantidade == 12
        delta.valorTotal == 170.00
        delta.detalhamento*.quantidade == [10, 2]
    }

    def "delta cobra apenas as fotos marginais dentro da mesma faixa"() {
        when:
        def delta = CalculadoraExtras.calcularDelta(
                new BigDecimal("50.00"), List.of(), 4, 2)

        then:
        delta.quantidade == 2
        delta.valorTotal == 100.00
        delta.detalhamento.size() == 1
        delta.detalhamento[0] == new FaixaAplicada(
                2, new BigDecimal("50.00"), new BigDecimal("100.00"))
    }

    def "delta atravessa faixa progressiva"() {
        given:
        def faixas = [
                new FaixaFotoExtraSnapshot(2, new BigDecimal("30.00")),
                new FaixaFotoExtraSnapshot(4, new BigDecimal("20.00"))
        ]

        when:
        def delta = CalculadoraExtras.calcularDelta(
                new BigDecimal("50.00"), faixas, 4, 1)

        then:
        delta.quantidade == 3
        delta.valorTotal == 80.00
        delta.detalhamento*.quantidade == [2, 1]
        delta.detalhamento*.valorUnitario == [
                new BigDecimal("30.00"),
                new BigDecimal("20.00")
        ]
        delta.detalhamento*.subtotal == [
                new BigDecimal("60.00"),
                new BigDecimal("20.00")
        ]
    }

    def "delta zero nao gera cobranca"() {
        when:
        def delta = CalculadoraExtras.calcularDelta(
                new BigDecimal("50.00"), List.of(), 3, 3)

        then:
        delta.quantidade == 0
        delta.valorTotal == 0.00
        delta.detalhamento.isEmpty()
    }
}
