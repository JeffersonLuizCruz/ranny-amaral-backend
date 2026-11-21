package com.ranny.fotografia.contrato.service

import com.ranny.fotografia.contrato.model.documento.ContratoClausula
import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot
import com.ranny.fotografia.shared.model.TipoDocumento
import spock.lang.Specification

import java.time.Instant
import java.time.OffsetDateTime

class ResolvedorClausulasSpec extends Specification {

    def resolvedor = new ResolvedorClausulas()

    def "resolve os tokens usados pelo contrato base CRM"() {
        given:
        def clausula = new ContratoClausula(
                "cl-teste",
                "1. Teste",
                "{{cliente.documento}} · {{cliente.cidade}} · {{estudio.razaoSocial}} · "
                        + "{{estudio.responsavel}} · {{contrato.numero}} · {{contrato.dataEmissao}} · "
                        + "{{contrato.validade}} · {{agendamento.valor}}")

        when:
        def resolvidas = resolvedor.resolver([clausula], snapshot(null), dados(new BigDecimal("890.00")))

        then:
        def texto = resolvidas[0].corpo()
        texto.contains("529.982.247-25")
        texto.contains("São Paulo")
        texto.contains("Ranny Amaral LTDA")
        texto.contains("Ranny Amaral")
        texto.contains("CT-2026-0001")
        texto.contains("03/11/2026")
        texto.contains("10/11/2026")
        texto.contains('R$')
    }

    def "resolve os tokens de deslocamento quando a taxa e repassada"() {
        given:
        def clausula = new ContratoClausula(
                "cl-teste",
                "1. Teste",
                "{{deslocamento.valor}} — {{deslocamento.descricao}}")

        when:
        def resolvidas = resolvedor.resolver([clausula], snapshot(deslocamentoRepassado()), dados(new BigDecimal("250.00")))

        then:
        def texto = resolvidas[0].corpo()
        texto.contains("50,00")
        texto.contains("repassada ao contratante")
        texto.contains("250,00")
        // O modelo ja usa os tokens: a clausula de sistema nao e duplicada.
        resolvidas.size() == 1
    }

    def "insere clausula de deslocamento antes da clausula de pagamento"() {
        given:
        def pacote = new ContratoClausula("cl-pacote", "3. Pacote", "Valor do pacote: {{pacote.valor}}.")
        def pagamento = new ContratoClausula(
                "cl-pagamento",
                "4. Pagamento",
                "Total de {{contrato.valorTotal}} e entrada de {{contrato.valorEntrada}}.")

        when:
        def resolvidas = resolvedor.resolver([pacote, pagamento],
                snapshot(deslocamentoRepassado()), dados(new BigDecimal("250.00")))

        then:
        resolvidas.size() == 3
        resolvidas[0].id() == "cl-pacote"
        resolvidas[1].id() == "cl-deslocamento-sistema"
        resolvidas[1].corpo().contains("50,00")
        resolvidas[1].corpo().contains("250,00")
        resolvidas[2].id() == "cl-pagamento"
    }

    def "nao insere clausula quando a taxa e absorvida"() {
        given:
        def pacote = new ContratoClausula("cl-pacote", "3. Pacote", "Valor do pacote: {{pacote.valor}}.")
        def pagamento = new ContratoClausula(
                "cl-pagamento", "4. Pagamento", "Total de {{contrato.valorTotal}}.")
        def absorvido = new ContratoSnapshot.DeslocamentoSnapshot(
                new BigDecimal("50.00"), false, BigDecimal.ZERO, new BigDecimal("50.00"))

        when:
        def resolvidas = resolvedor.resolver([pacote, pagamento],
                snapshot(absorvido), dados(new BigDecimal("200.00")))

        then:
        resolvidas.size() == 2
        resolvidas.every { !it.id().contains("deslocamento") }
    }

    private static ContratoSnapshot.DeslocamentoSnapshot deslocamentoRepassado() {
        return new ContratoSnapshot.DeslocamentoSnapshot(
                new BigDecimal("50.00"), true,
                new BigDecimal("50.00"), BigDecimal.ZERO)
    }

    private static ContratoSnapshot snapshot(ContratoSnapshot.DeslocamentoSnapshot deslocamento) {
        def estudio = new ContratoSnapshot.EstudioSnapshot(
                "Ranny Amaral Fotografia",
                "Ranny Amaral LTDA",
                "00.000.000/0001-00",
                "Rua das Flores, 123",
                "São Paulo",
                "SP",
                "contato@ranny.com.br",
                "(11) 99999-9999",
                "Ranny Amaral",
                "www.ranny.com.br",
                "São Paulo/SP",
                new BigDecimal("30.00"),
                "pix@ranny.com.br",
                "Pague via Pix.",
                3)
        def cliente = new ContratoSnapshot.ClienteSnapshot(
                "Marina Alves",
                "(11) 91234-5678",
                "marina@teste.com",
                "529.982.247-25",
                TipoDocumento.CPF,
                "São Paulo")
        def pacote = new ContratoSnapshot.PacoteSnapshot(
                "Ensaio Gestante",
                90,
                new BigDecimal("200.00"),
                ["20 fotos tratadas"],
                20,
                0,
                new BigDecimal("45.00"),
                List.of(),
                null,
                new BigDecimal("200.00"))
        def agendamento = new ContratoSnapshot.AgendamentoSnapshot(
                OffsetDateTime.parse("2026-11-20T13:00:00-03:00"),
                OffsetDateTime.parse("2026-11-20T14:30:00-03:00"),
                "Estúdio",
                new BigDecimal("250.00"))
        return new ContratoSnapshot(cliente, pacote, agendamento, estudio, deslocamento, 1)
    }

    private static ResolvedorClausulas.DadosResolucao dados(BigDecimal valorTotal) {
        return new ResolvedorClausulas.DadosResolucao(
                "CT-2026-0001",
                Instant.parse("2026-11-03T12:00:00Z"),
                Instant.parse("2026-11-10T12:00:00Z"),
                valorTotal,
                valorTotal.multiply(new BigDecimal("0.30")).setScale(2),
                new BigDecimal("30.00"),
                valorTotal.multiply(new BigDecimal("0.70")).setScale(2),
                "Pix")
    }
}
