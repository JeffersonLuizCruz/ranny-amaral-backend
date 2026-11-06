package com.ranny.fotografia.agenda.service;

import com.ranny.fotografia.agenda.repository.AgendamentoRepository;
import com.ranny.fotografia.shared.ports.VerificadorVinculoPacote;
import org.springframework.stereotype.Component;

/** PATTERN: Strategy — verificacao de vinculo de pacote sem acoplamento a pacote. */
@Component
public class AgendamentoVinculoPacote implements VerificadorVinculoPacote {

    private final AgendamentoRepository agendamentoRepository;

    public AgendamentoVinculoPacote(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @Override
    public boolean possuiVinculo(String pacoteId) {
        return agendamentoRepository.existsByPacoteId(pacoteId);
    }
}
