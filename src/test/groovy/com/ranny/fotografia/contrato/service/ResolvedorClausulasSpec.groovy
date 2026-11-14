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
                new BigDecimal("890.00"),
                ["20 fotos tratadas"],
                20,
                0,
                new BigDecimal("45.00"),
                List.of(),
                null,
                new BigDecimal("890.00"))
        def agendamento = new ContratoSnapshot.AgendamentoSnapshot(
                OffsetDateTime.parse("2026-11-20T13:00:00-03:00"),
                OffsetDateTime.parse("2026-11-20T14:30:00-03:00"),
                "Estúdio",
                new BigDecimal("890.00"))
        def snapshot = new ContratoSnapshot(cliente, pacote, agendamento, estudio, 1)
        def dados = new ResolvedorClausulas.DadosResolucao(
                "CT-2026-0001",
                Instant.parse("2026-11-03T12:00:00Z"),
                Instant.parse("2026-11-10T12:00:00Z"),
                new BigDecimal("890.00"),
                new BigDecimal("267.00"),
                new BigDecimal("30.00"),
                new BigDecimal("623.00"),
                "Pix")
        def clausula = new ContratoClausula(
                "cl-teste",
                "1. Teste",
                "{{cliente.documento}} · {{cliente.cidade}} · {{estudio.razaoSocial}} · "
                        + "{{estudio.responsavel}} · {{contrato.numero}} · {{contrato.dataEmissao}} · "
                        + "{{contrato.validade}} · {{agendamento.valor}}")

        when:
        def resolvidas = resolvedor.resolver([clausula], snapshot, dados)

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
}
