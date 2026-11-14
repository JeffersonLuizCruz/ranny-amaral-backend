package com.ranny.fotografia.armazenamento.service

import com.ranny.fotografia.armazenamento.ArmazenamentoProperties
import com.ranny.fotografia.armazenamento.model.AmostraArmazenamento
import com.ranny.fotografia.armazenamento.model.OrigemColeta
import com.ranny.fotografia.armazenamento.repository.AmostraArmazenamentoRepository
import com.ranny.fotografia.arquivo.ArquivoConsulta
import com.ranny.fotografia.galeria.GaleriaConsulta
import com.ranny.fotografia.galeria.GaleriaLimpeza
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit

class ArmazenamentoLimpezaSpec extends Specification {

    private static final long GB = 1024L * 1024 * 1024

    def repository = Mock(AmostraArmazenamentoRepository)
    def coletor = Mock(ColetorArmazenamento)
    def arquivoConsulta = Mock(ArquivoConsulta)
    def galeriaConsulta = Mock(GaleriaConsulta)
    def galeriaLimpeza = Mock(GaleriaLimpeza)
    def properties = new ArmazenamentoProperties()

    def "agenda as galerias mais antigas ate atingir o alvo"() {
        given:
        properties.percentualLimpeza = 90
        properties.percentualAlvo = 80
        def amostra = AmostraArmazenamento.registrar(
                OrigemColeta.FILESYSTEM, 100 * GB, 5 * GB,
                0, 0, null, 0, 0, 0, 0, 0, 10 * GB)
        repository.findFirstByOrderByColetadoEmDesc() >> Optional.of(amostra)

        def agora = Instant.now()
        def g1 = resumo("g1", agora.minus(3, ChronoUnit.DAYS))
        def g2 = resumo("g2", agora.minus(2, ChronoUnit.DAYS))
        def g3 = resumo("g3", agora.minus(1, ChronoUnit.DAYS))
        def g4 = resumo("g4", agora)
        galeriaConsulta.galeriasParaArmazenamento() >> [g4, g2, g1, g3]

        def totais = [
                g1: new ArquivoConsulta.TotaisReferencia("g1", 6 * GB, 1),
                g2: new ArquivoConsulta.TotaisReferencia("g2", 8 * GB, 1),
                g3: new ArquivoConsulta.TotaisReferencia("g3", 10 * GB, 1),
                g4: new ArquivoConsulta.TotaisReferencia("g4", 20 * GB, 1)
        ]
        arquivoConsulta.totaisPorReferencia(_) >> totais

        List<String> selecionadas = []
        def service = new ArmazenamentoService(
                repository, coletor, arquivoConsulta, galeriaConsulta, galeriaLimpeza, properties)

        when:
        def resultado = service.limparAte(80)

        then:
        1 * galeriaLimpeza.agendar(_, _) >> { args ->
            selecionadas.addAll(args[0] as List<String>)
            3
        }
        selecionadas == ["g1", "g2", "g3"]
        resultado.galeriasAgendadas() == 3
        resultado.bytesEstimados() == 24 * GB
        resultado.percentualAntes() == 95
        resultado.percentualDepoisEstimado() == 71
        resultado.atingiuAlvo()
    }

    private static GaleriaConsulta.ResumoArmazenamento resumo(String id, Instant entregueEm) {
        return new GaleriaConsulta.ResumoArmazenamento(
                id, "Cliente " + id, "entregue", entregueEm, entregueEm, entregueEm,
                "nenhum", null, 0, true, null)
    }
}
