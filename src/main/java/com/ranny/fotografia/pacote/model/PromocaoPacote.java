package com.ranny.fotografia.pacote.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PATTERN: Value Object embutido para a promocao do catalogo.
 *
 * <p>A promocao e um atributo opcional do pacote. Modela-la como embeddable
 * mantem a tabela de pacotes enxuta e permite que o mesmo objeto seja copiado
 * para o snapshot comercial no momento da venda.
 */
@Embeddable
@Getter
public class PromocaoPacote {

    @Column(name = "promocao_ativa")
    private Boolean ativa;

    @Column(name = "promocao_valor", precision = 10, scale = 2)
    private BigDecimal valorPromocional;

    @Column(name = "promocao_valida_ate")
    private LocalDate validaAte;

    @Column(name = "promocao_rotulo", length = 120)
    private String rotulo;

    protected PromocaoPacote() {
        // Exigido pelo JPA.
    }

    private PromocaoPacote(Boolean ativa, BigDecimal valorPromocional, LocalDate validaAte, String rotulo) {
        this.ativa = ativa;
        this.valorPromocional = valorPromocional;
        this.validaAte = validaAte;
        this.rotulo = rotulo;
    }

    public static PromocaoPacote de(Boolean ativa, BigDecimal valorPromocional,
                                    LocalDate validaAte, String rotulo) {
        if (ativa == null && valorPromocional == null && validaAte == null && rotulo == null) {
            return null;
        }
        return new PromocaoPacote(
                Boolean.TRUE.equals(ativa),
                valorPromocional,
                validaAte,
                rotulo);
    }

    public boolean estaAtiva() {
        return Boolean.TRUE.equals(ativa);
    }
}
