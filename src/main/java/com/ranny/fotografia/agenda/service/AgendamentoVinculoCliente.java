package com.ranny.fotografia.agenda.service;

import com.ranny.fotografia.agenda.repository.AgendamentoRepository;
import com.ranny.fotografia.shared.ports.VerificadorVinculoCliente;
import org.springframework.stereotype.Component;

/**
 * PATTERN: Strategy (implementacao da porta) + Adapter.
 *
 * <p>Depende apenas do repositorio de agendamentos (e nao do {@code AgendaService})
 * para evitar ciclo: cliente -> porta <- agenda, sem que cliente conheca agenda.
 */
@Component
public class AgendamentoVinculoCliente implements VerificadorVinculoCliente {

    private final AgendamentoRepository agendamentoRepository;

    public AgendamentoVinculoCliente(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @Override
    public boolean possuiVinculo(String clienteId) {
        return agendamentoRepository.existsByClienteId(clienteId);
    }
}
