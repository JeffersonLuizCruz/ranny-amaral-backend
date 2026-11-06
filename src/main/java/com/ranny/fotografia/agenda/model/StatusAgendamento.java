package com.ranny.fotografia.agenda.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Estado operacional do ensaio.
 *
 * <p>PATTERN: State (simplificado). O enum concentra o grafo de transicoes
 * validas, evitando que cada service reimplemente as mesmas regras.
 *
 * <p>Regras: um ensaio realizado nao volta atras (o historico ja aconteceu);
 * um cancelamento pode ser reaberto como reserva; qualquer estado ativo pode ser
 * cancelado.
 */
public enum StatusAgendamento {
    RESERVADO("reservado"),
    CONFIRMADO("confirmado"),
    REALIZADO("realizado"),
    CANCELADO("cancelado");

    private final String valor;

    StatusAgendamento(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusAgendamento de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (StatusAgendamento status : values()) {
            if (status.valor.equalsIgnoreCase(valor)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de agendamento invalido: " + valor);
    }

    public boolean podeTransicionarPara(StatusAgendamento destino) {
        if (destino == null || destino == this) {
            return true;
        }
        return switch (this) {
            case RESERVADO -> destino == CONFIRMADO || destino == REALIZADO || destino == CANCELADO;
            case CONFIRMADO -> destino == RESERVADO || destino == REALIZADO || destino == CANCELADO;
            case REALIZADO -> false;
            case CANCELADO -> destino == RESERVADO;
        };
    }
}
