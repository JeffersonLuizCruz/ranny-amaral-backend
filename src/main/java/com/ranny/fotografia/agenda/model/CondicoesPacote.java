package com.ranny.fotografia.agenda.model;

import com.ranny.fotografia.shared.model.FaixaFotoExtraSnapshot;
import com.ranny.fotografia.shared.model.FaixasFotoExtraConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

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

    @Convert(converter = FaixasFotoExtraConverter.class)
    @Column(name = "cond_faixas_foto_extra", columnDefinition = "text")
    private List<FaixaFotoExtraSnapshot> faixasFotoExtra;

    @Column(name = "cond_valor_tabela", precision = 10, scale = 2)
    private BigDecimal valorTabela;

    @Embedded
    private PromocaoAplicada promocaoAplicada;

    protected CondicoesPacote() {
        // Exigido pelo JPA.
    }

    private CondicoesPacote(int quantidadeFotosInclusas, int quantidadeVideosInclusos,
                            BigDecimal valorFotoExtra, BigDecimal valorTabela,
                            List<FaixaFotoExtraSnapshot> faixasFotoExtra,
                            PromocaoAplicada promocaoAplicada) {
        this.quantidadeFotosInclusas = Math.max(0, quantidadeFotosInclusas);
        this.quantidadeVideosInclusos = Math.max(0, quantidadeVideosInclusos);
        this.valorFotoExtra = valorFotoExtra == null ? BigDecimal.ZERO : valorFotoExtra;
        this.valorTabela = valorTabela == null ? BigDecimal.ZERO : valorTabela;
        this.faixasFotoExtra = faixasFotoExtra == null ? List.of() : List.copyOf(faixasFotoExtra);
        this.promocaoAplicada = promocaoAplicada;
    }

    public static CondicoesPacote de(int fotos, int videos, BigDecimal valorFotoExtra,
                                     BigDecimal valorTabela, PromocaoAplicada promocao) {
        return new CondicoesPacote(fotos, videos, valorFotoExtra, valorTabela,
                List.of(), promocao);
    }

    public static CondicoesPacote de(int fotos, int videos, BigDecimal valorFotoExtra,
                                     BigDecimal valorTabela,
                                     List<FaixaFotoExtraSnapshot> faixasFotoExtra,
                                     PromocaoAplicada promocao) {
        return new CondicoesPacote(fotos, videos, valorFotoExtra, valorTabela,
                faixasFotoExtra, promocao);
    }

    /** Copia defensiva do snapshot das faixas. */
    public List<FaixaFotoExtraSnapshot> getFaixasFotoExtra() {
        return faixasFotoExtra == null ? List.of() : List.copyOf(faixasFotoExtra);
    }
}
