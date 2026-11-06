package com.ranny.fotografia.agenda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/** Contato de uma reserva criada antes de existir um cliente cadastrado. */
@Embeddable
@Getter
public class ContatoReserva {

    @Column(name = "contato_nome", length = 120)
    private String nome;

    @Column(name = "contato_whatsapp", length = 30)
    private String whatsapp;

    protected ContatoReserva() {
        // Exigido pelo JPA.
    }

    private ContatoReserva(String nome, String whatsapp) {
        this.nome = nome;
        this.whatsapp = whatsapp;
    }

    public static ContatoReserva de(String nome, String whatsapp) {
        if ((nome == null || nome.isBlank()) && (whatsapp == null || whatsapp.isBlank())) {
            return null;
        }
        return new ContatoReserva(
                nome == null || nome.isBlank() ? null : nome.trim(),
                whatsapp == null || whatsapp.isBlank() ? null : whatsapp.trim());
    }
}
