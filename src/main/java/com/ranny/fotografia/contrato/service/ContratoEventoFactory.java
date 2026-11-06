package com.ranny.fotografia.contrato.service;

import com.ranny.fotografia.contrato.model.Contrato;
import com.ranny.fotografia.shared.events.ContratoStatusAlteradoEvent;

/**
 * PATTERN: Factory Method para o evento de integração contrato -> agenda.
 *
 * <p>Centraliza a montagem do payload compacto, garantindo que os dois pontos
 * de publicação (comandos administrativos e aceite público) enviem exatamente
 * o mesmo formato.
 */
final class ContratoEventoFactory {

    private ContratoEventoFactory() {
    }

    static ContratoStatusAlteradoEvent de(Contrato contrato) {
        return new ContratoStatusAlteradoEvent(
                contrato.getId(),
                contrato.getAgendamentoId(),
                contrato.getStatusAceite().getValor(),
                contrato.getStatusPagamento().getValor(),
                contrato.getPagamento().getValor(),
                contrato.getPagamento().getStatus().getValor(),
                contrato.getPagamentoFinal().getValor(),
                contrato.getPagamentoFinal().getStatus().getValor());
    }
}
