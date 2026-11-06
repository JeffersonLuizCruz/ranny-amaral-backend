package com.ranny.fotografia.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PATTERN: Value Object (DDD) embutido ({@code @Embeddable}).
 *
 * <p>A entrada e um conceito de negocio reutilizado por {@code Agendamento} e
 * {@code Contrato}. Em vez de duplicar colunas nas duas tabelas ou criar uma
 * hierarquia de entidades, o valor e composto nos dois agregados. As colunas ja
 * nascem prefixadas com {@code entrada_} para que a mesma classe possa conviver
 * com {@link PagamentoSaldo} na mesma tabela sem colisao de nomes.
 *
 * <p>Beneficios: regras de transicao de pagamento ficam encapsuladas no objeto,
 * o modelo relacional continua consultavel (status em coluna propria) e o
 * objeto pode ser testado sem Spring.
 */
@Embeddable
@Getter
public class PagamentoEntrada {

    @Column(name = "entrada_valor", precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "entrada_percentual", precision = 5, scale = 2)
    private BigDecimal percentual;

    @Enumerated(EnumType.STRING)
    @Column(name = "entrada_status", length = 20, nullable = false)
    private PagamentoStatus status;

    @Convert(converter = ComprovantePagamentoConverter.class)
    @Column(name = "entrada_comprovante", columnDefinition = "TEXT")
    private ComprovantePagamento comprovante;

    /**
     * Referencia ao arquivo generico no object storage. O JSON de comprovante
     * continua como snapshot de exibicao/legado.
     */
    @Column(name = "entrada_arquivo_id", length = 64)
    private String arquivoId;

    @Convert(converter = RejeicaoComprovanteConverter.class)
    @Column(name = "entrada_rejeicao", columnDefinition = "TEXT")
    private RejeicaoComprovante rejeicao;

    @Column(name = "entrada_confirmado_em")
    private Instant confirmadoEm;

    @Column(name = "entrada_confirmado_por", length = 120)
    private String confirmadoPor;

    @Column(name = "entrada_observacoes", length = 1000)
    private String observacoes;

    protected PagamentoEntrada() {
        // Exigido pelo JPA.
    }

    private PagamentoEntrada(BigDecimal valor, BigDecimal percentual, PagamentoStatus status) {
        this.valor = valor;
        this.percentual = percentual;
        this.status = status;
    }

    /** Cria uma entrada pendente de confirmacao. */
    public static PagamentoEntrada pendente(BigDecimal valor, BigDecimal percentual) {
        PagamentoStatus status = valor == null || valor.signum() <= 0
                ? PagamentoStatus.ISENTO
                : PagamentoStatus.PENDENTE;
        return new PagamentoEntrada(valor, percentual, status);
    }

    /** Cria uma entrada ja confirmada (usado pelo seed). */
    public static PagamentoEntrada confirmada(
            BigDecimal valor, BigDecimal percentual, Instant em, String por) {
        PagamentoEntrada pagamento = new PagamentoEntrada(valor, percentual, PagamentoStatus.CONFIRMADO);
        pagamento.confirmadoEm = em;
        pagamento.confirmadoPor = por;
        return pagamento;
    }

    /**
     * Reconstroi o pagamento a partir do evento compacto do outbox.
     * Percentual e metadados completos permanecem no contrato.
     */
    public static PagamentoEntrada deEvento(BigDecimal valor, PagamentoStatus status) {
        return new PagamentoEntrada(
                valor,
                null,
                status == null ? PagamentoStatus.PENDENTE : status);
    }

    /** Registra o comprovante enviado e reabre a analise. */
    public void registrarComprovante(ComprovantePagamento comprovante) {
        registrarComprovante(null, comprovante);
    }

    /** Registra o comprovante e o arquivo generico associado. */
    public void registrarComprovante(String arquivoId, ComprovantePagamento comprovante) {
        this.arquivoId = arquivoId;
        this.comprovante = comprovante;
        this.rejeicao = null;
        this.status = PagamentoStatus.PENDENTE;
    }

    /** Confirma a entrada. */
    public void confirmar(Instant em, String por) {
        if (status == PagamentoStatus.CANCELADO) {
            throw new IllegalStateException("Pagamento cancelado nao pode ser confirmado.");
        }
        this.status = PagamentoStatus.CONFIRMADO;
        this.confirmadoEm = em;
        this.confirmadoPor = por;
        this.rejeicao = null;
    }

    /** Rejeita o comprovante e devolve o pagamento para pendente. */
    public void rejeitar(RejeicaoComprovante rejeicao) {
        this.status = PagamentoStatus.PENDENTE;
        this.rejeicao = rejeicao;
    }

    /** Isenta a entrada (sem cobranca). */
    public void isentar() {
        this.status = PagamentoStatus.ISENTO;
        this.comprovante = null;
        this.arquivoId = null;
        this.rejeicao = null;
        this.confirmadoEm = null;
        this.confirmadoPor = null;
    }

    /** Cancela o pagamento junto com o contrato/agendamento. */
    public void cancelar() {
        if (status != PagamentoStatus.CONFIRMADO) {
            this.status = PagamentoStatus.CANCELADO;
        }
    }

    /** Atualiza valor/percentual preservando o historico de confirmacao. */
    public void redefinir(BigDecimal valor, BigDecimal percentual) {
        this.valor = valor;
        this.percentual = percentual;
        if (valor == null || valor.signum() <= 0) {
            this.status = PagamentoStatus.ISENTO;
        } else if (status == null || status == PagamentoStatus.ISENTO) {
            this.status = PagamentoStatus.PENDENTE;
        }
    }

    /** Reabre a analise do comprovante, limpando a confirmacao anterior. */
    public void reabrirAnalise() {
        if (status == PagamentoStatus.CANCELADO) {
            throw new IllegalStateException("Pagamento cancelado nao pode ser reaberto.");
        }
        this.status = valor != null && valor.signum() > 0
                ? PagamentoStatus.PENDENTE
                : PagamentoStatus.ISENTO;
        this.confirmadoEm = null;
        this.confirmadoPor = null;
        this.rejeicao = null;
    }

    public void observar(String observacoes) {
        this.observacoes = observacoes;
    }

    public boolean estaQuitado() {
        return status != null && status.estaQuitado();
    }
}
