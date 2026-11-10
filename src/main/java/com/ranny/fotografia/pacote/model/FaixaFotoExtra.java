package com.ranny.fotografia.pacote.model;

import com.ranny.fotografia.shared.model.FaixaFotoExtraSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Faixa de preco da foto extra no catalogo.
 *
 * <p>A primeira faixa comeca sempre depois do preco base; a entidade nao
 * permite quantidade minima menor que 2.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaixaFotoExtra {

    @Column(name = "quantidade_minima", nullable = false)
    private int quantidadeMinima;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    private FaixaFotoExtra(int quantidadeMinima, BigDecimal valorUnitario) {
        this.quantidadeMinima = quantidadeMinima;
        this.valorUnitario = valorUnitario == null ? BigDecimal.ZERO : valorUnitario;
    }

    public static FaixaFotoExtra de(int quantidadeMinima, BigDecimal valorUnitario) {
        return new FaixaFotoExtra(quantidadeMinima, valorUnitario);
    }

    public FaixaFotoExtraSnapshot paraSnapshot() {
        return new FaixaFotoExtraSnapshot(quantidadeMinima, valorUnitario);
    }
}
