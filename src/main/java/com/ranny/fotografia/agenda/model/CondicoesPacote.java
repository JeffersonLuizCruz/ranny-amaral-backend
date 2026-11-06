package com.ranny.fotografia.agenda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * PATTERN: Value Object embutido (snapshot comercial).
 *
 * <p>As condicoes do pacote sao copiadas para o agendamento no momento da
 * criacao. Alterar o catalogo depois nao muda vendas ja fechadas — essa e a
 * principal invariante do modulo de agenda.
 */
@Embeddable
@Getter
public class CondicoesPacote {

    @Column(name = "cond_fotos_inclusas")
    private int quantidadeFotosInclusas;

    @Column(name = "cond_videos_inclusos")
    private int quantidadeVideosInclusos;

    @Column(name = "cond_valor_foto_extra", precision = 10, scale = 2)
    private BigDecimal valorFotoExtra;

    @Column(name = "cond_valor_tabela", precision = 10, scale = 2)
    private BigDecimal valorTabela;

    @Embedded
    private PromocaoAplicada promocaoAplicada;

    protected CondicoesPacote() {
        // Exigido pelo JPA.
    }

    private CondicoesPacote(int quantidadeFotosInclusas, int quantidadeVideosInclusos,
                            BigDecimal valorFotoExtra, BigDecimal valorTabela,
                            PromocaoAplicada promocaoAplicada) {
        this.quantidadeFotosInclusas = Math.max(0, quantidadeFotosInclusas);
        this.quantidadeVideosInclusos = Math.max(0, quantidadeVideosInclusos);
        this.valorFotoExtra = valorFotoExtra == null ? BigDecimal.ZERO : valorFotoExtra;
        this.valorTabela = valorTabela == null ? BigDecimal.ZERO : valorTabela;
        this.promocaoAplicada = promocaoAplicada;
    }

    public static CondicoesPacote de(int fotos, int videos, BigDecimal valorFotoExtra,
                                     BigDecimal valorTabela, PromocaoAplicada promocao) {
        return new CondicoesPacote(fotos, videos, valorFotoExtra, valorTabela, promocao);
    }
}
