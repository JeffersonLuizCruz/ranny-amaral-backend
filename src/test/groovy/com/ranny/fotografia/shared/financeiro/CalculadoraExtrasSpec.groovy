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
    def "aplica a faixa alcancada a todas as #quantidade extras"() {
        given:
        def faixas = [new FaixaFotoExtraSnapshot(3, new BigDecimal("10.00"))]

        when:
        def resultado = CalculadoraExtras.calcular(new BigDecimal("15.00"), faixas, quantidade)

        then:
        resultado.valorTotal == valorEsperado
        resultado.detalhamento*.quantidade == (quantidade > 0 ? [quantidade] : [])
        quantidade == 0 || resultado.detalhamento[0].valorUnitario() == unitarioEsperado

        where:
        quantidade | valorEsperado | unitarioEsperado
        0          | 0.00          | null
        1          | 15.00         | new BigDecimal("15.00")
        2          | 30.00         | new BigDecimal("15.00")
        3          | 30.00         | new BigDecimal("10.00")
        4          | 40.00         | new BigDecimal("10.00")
        5          | 50.00         | new BigDecimal("10.00")
    }

    def "aplica a maior faixa alcancada a todas as fotos"() {
        given:
        def faixas = [
                new FaixaFotoExtraSnapshot(2, new BigDecimal("30.00")),
                new FaixaFotoExtraSnapshot(4, new BigDecimal("20.00"))
        ]

        when:
        def resultado = CalculadoraExtras.calcular(new BigDecimal("50.00"), faixas, 5)

        then:
        resultado.valorTotal == 100.00
        resultado.valorMedioUnitario == 20.00
        resultado.detalhamento == [
                new FaixaAplicada(5, new BigDecimal("20.00"), new BigDecimal("100.00"))
        ]
    }

    def "quantidade zero nao gera detalhamento"() {
        when:
        def resultado = CalculadoraExtras.calcular(new BigDecimal("15.00"),
                [new FaixaFotoExtraSnapshot(3, new BigDecimal("10.00"))], 0)

        then:
        resultado.valorTotal == 0.00
        resultado.valorMedioUnitario == 0.00
        resultado.detalhamento.isEmpty()
    }

    def "delta sem cobertura equivale ao calculo total"() {
        given:
        def faixas = [new FaixaFotoExtraSnapshot(3, new BigDecimal("10.00"))]

        when:
        def delta = CalculadoraExtras.calcularDelta(
                new BigDecimal("15.00"), faixas, 4, 0)

        then:
        delta.quantidade == 4
        delta.valorTotal == 40.00
        delta.detalhamento == [
                new FaixaAplicada(4, new BigDecimal("10.00"), new BigDecimal("40.00"))
        ]
    }

    def "delta zero na virada de faixa nao cobra nada"() {
        given:
        def faixas = [new FaixaFotoExtraSnapshot(3, new BigDecimal("10.00"))]

        when:
        def delta = CalculadoraExtras.calcularDelta(
                new BigDecimal("15.00"), faixas, 3, 2)

        then:
        delta.quantidade == 1
        delta.valorTotal == 0.00
        delta.detalhamento.isEmpty()
    }

    def "delta apos a virada cobra a diferenca da faixa"() {
        given:
        def faixas = [new FaixaFotoExtraSnapshot(3, new BigDecimal("10.00"))]

        when:
        def delta = CalculadoraExtras.calcularDelta(
                new BigDecimal("15.00"), faixas, 4, 3)

        then:
        delta.quantidade == 1
        delta.valorTotal == 10.00
        delta.detalhamento == [
                new FaixaAplicada(1, new BigDecimal("10.00"), new BigDecimal("10.00"))
        ]
    }

    def "delta entre duas quantidades abaixo e acima da faixa"() {
        given:
        def faixas = [new FaixaFotoExtraSnapshot(3, new BigDecimal("10.00"))]

        when:
        def delta = CalculadoraExtras.calcularDelta(
                new BigDecimal("15.00"), faixas, 4, 2)

        then:
        delta.quantidade == 2
        delta.valorTotal == 10.00
        delta.detalhamento == [
                new FaixaAplicada(2, new BigDecimal("5.00"), new BigDecimal("10.00"))
        ]
    }

    def "delta sem faixas usa o preco base"() {
        when:
        def delta = CalculadoraExtras.calcularDelta(
                new BigDecimal("50.00"), List.of(), 4, 2)

        then:
        delta.quantidade == 2
        delta.valorTotal == 100.00
        delta.detalhamento == [
                new FaixaAplicada(2, new BigDecimal("50.00"), new BigDecimal("100.00"))
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

    def "rejeita cobertura maior que a quantidade total"() {
        when:
        CalculadoraExtras.calcularDelta(
                new BigDecimal("15.00"), List.of(), 2, 3)

        then:
        thrown(IllegalArgumentException)
    }
}
