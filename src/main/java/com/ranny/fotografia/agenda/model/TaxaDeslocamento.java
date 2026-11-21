package com.ranny.fotografia.agenda.model;

import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.financeiro.CalculadoraPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * PATTERN: Value Object embutido.
 *
 * <p>A taxa de deslocamento pertence ao ensaio, nao ao catalogo: o studio
 * informa o valor e decide se repassa ao cliente ou absorve o custo. Quando
 * repassada, o valor entra no total acordado e, consequentemente, na entrada e
 * no saldo. Quando absorvida, fica registrada apenas para controle interno.
 */
@Embeddable
@Getter
public class TaxaDeslocamento {

    @Column(name = "taxa_deslocamento", precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "taxa_deslocamento_repassada")
    private boolean repassada;

    protected TaxaDeslocamento() {
        // Exigido pelo JPA.
    }

    private TaxaDeslocamento(BigDecimal valor, boolean repassada) {
        this.valor = valor;
        this.repassada = repassada;
    }

    public static TaxaDeslocamento nenhuma() {
        return new TaxaDeslocamento(BigDecimal.ZERO, false);
    }

    public static TaxaDeslocamento de(BigDecimal valor, boolean repassada) {
        BigDecimal normalizado = CalculadoraPagamento.normalizar(valor);
        if (normalizado.signum() < 0) {
            throw new RegraDeNegocioException("TAXA_DESLOCAMENTO_INVALIDA",
                    "A taxa de deslocamento nao pode ser negativa.");
        }
        return new TaxaDeslocamento(normalizado, repassada);
    }

    public BigDecimal getValor() {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public boolean isRepassada() {
        return repassada && getValor().signum() > 0;
    }

    /** Parte da taxa que compoe o valor cobrado do cliente. */
    public BigDecimal valorCobrado() {
        return isRepassada() ? getValor() : BigDecimal.ZERO;
    }

    /** Parte da taxa assumida pelo estudio. */
    public BigDecimal valorAbsorvido() {
        return isRepassada() ? BigDecimal.ZERO : getValor();
    }
}
