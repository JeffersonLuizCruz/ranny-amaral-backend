package com.ranny.fotografia.galeria.service

import com.ranny.fotografia.galeria.model.Galeria
import com.ranny.fotografia.galeria.model.MidiaGaleria
import com.ranny.fotografia.galeria.model.MidiaStatus
import com.ranny.fotografia.galeria.model.MidiaTipo
import com.ranny.fotografia.galeria.repository.GaleriaRepository
import com.ranny.fotografia.galeria.repository.MidiaGaleriaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * A falha e registrada em transacao propria (REQUIRES_NEW), fora da transacao
 * que sofreu rollback. Por isso o cenario nao roda dentro de @Transactional.
 */
@SpringBootTest
@ActiveProfiles("test")
class MidiaProcessamentoFalhaSpec extends Specification {

    @Autowired
    GaleriaRepository galeriaRepository

    @Autowired
    MidiaGaleriaRepository midiaRepository

    @Autowired
    GaleriaService galeriaService

    private Galeria galeria
    private MidiaGaleria midia

    def "persiste FALHA mesmo quando a tentativa de processamento sofre rollback"() {
        given:
        galeria = galeriaRepository.save(Galeria.criar(
                "estudio", "agendamento-falha", null, "cliente-falha",
                "Cliente Falha", 1, 0, BigDecimal.ZERO))
        midia = midiaRepository.save(MidiaGaleria.criar(
                galeria.id, MidiaTipo.FOTO, "arquivo-inexistente", "foto.jpg",
                "image/jpeg", 1, 1))
        assert midia.status == MidiaStatus.PROCESSANDO

        when:
        galeriaService.registrarFalhaProcessamento(
                galeria.id, midia.id, "erro simulado no processamento")

        then:
        def atual = midiaRepository.findById(midia.id).get()
        atual.status == MidiaStatus.FALHA
        atual.erroProcessamento == "erro simulado no processamento"
    }

    def cleanup() {
        if (midia != null) {
            midiaRepository.deleteById(midia.id)
        }
        if (galeria != null) {
            galeriaRepository.deleteById(galeria.id)
        }
    }
}
