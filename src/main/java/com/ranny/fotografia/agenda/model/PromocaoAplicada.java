package com.ranny.fotografia.agenda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

/** Promocao efetivamente aplicada no momento da venda (snapshot comercial). */
@Embeddable
@Getter
public class PromocaoAplicada {

    @Column(name = "cond_promocao_valor", precision = 10, scale = 2)
    private BigDecimal valorPromocional;

    @Column(name = "cond_promocao_rotulo", length = 120)
    private String rotulo;

    protected PromocaoAplicada() {
        // Exigido pelo JPA.
    }

    private PromocaoAplicada(BigDecimal valorPromocional, String rotulo) {
        this.valorPromocional = valorPromocional;
        this.rotulo = rotulo;
    }

    public static PromocaoAplicada de(BigDecimal valorPromocional, String rotulo) {
        return new PromocaoAplicada(valorPromocional, rotulo);
    }
}
