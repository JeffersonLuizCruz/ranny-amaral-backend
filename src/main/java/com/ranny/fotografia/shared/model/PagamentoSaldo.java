package com.ranny.fotografia.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PATTERN: Value Object embutido para o saldo final recebido pelo estudio.
 *
 * <p>O saldo nao usa comprovante publico: e um registro interno da equipe.
 * Separar essa classe de {@link PagamentoEntrada} deixa explicitas as
 * diferencas de regra (forma de pagamento, recebido por, sem upload) sem
 * condicionais espalhadas pelos services.
 */
@Embeddable
@Getter
public class PagamentoSaldo {

    @Column(name = "saldo_valor", precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "saldo_status", length = 20)
    private PagamentoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "saldo_forma_pagamento", length = 20)
    private FormaPagamentoSaldo formaPagamento;

    @Column(name = "saldo_recebido_em")
    private Instant recebidoEm;

    @Column(name = "saldo_recebido_por", length = 120)
    private String recebidoPor;

    @Column(name = "saldo_observacoes", length = 1000)
    private String observacoes;

    protected PagamentoSaldo() {
        // Exigido pelo JPA.
    }

    private PagamentoSaldo(BigDecimal valor, PagamentoStatus status) {
        this.valor = valor;
        this.status = status;
    }

    /** Cria um saldo pendente (ou isento quando nao ha valor a receber). */
    public static PagamentoSaldo pendente(BigDecimal valor) {
        PagamentoStatus status = valor == null || valor.signum() <= 0
                ? PagamentoStatus.ISENTO
                : PagamentoStatus.PENDENTE;
        return new PagamentoSaldo(valor, status);
    }

    /** Cria um saldo ja recebido (usado pelo seed). */
    public static PagamentoSaldo recebido(
            BigDecimal valor, FormaPagamentoSaldo forma, Instant em, String por) {
        PagamentoSaldo saldo = new PagamentoSaldo(valor, PagamentoStatus.CONFIRMADO);
        saldo.formaPagamento = forma;
        saldo.recebidoEm = em;
        saldo.recebidoPor = por;
        return saldo;
    }

    /** Reconstroi o saldo a partir do evento compacto do outbox. */
    public static PagamentoSaldo deEvento(BigDecimal valor, PagamentoStatus status) {
        return new PagamentoSaldo(valor, status == null ? PagamentoStatus.PENDENTE : status);
    }

    /** Registra o recebimento do saldo. */
    public void receber(BigDecimal valor, FormaPagamentoSaldo forma, Instant em, String por, String observacoes) {
        if (status == PagamentoStatus.CANCELADO) {
            throw new IllegalStateException("Saldo cancelado nao pode ser recebido.");
        }
        this.valor = valor;
        this.formaPagamento = forma;
        this.recebidoEm = em;
        this.recebidoPor = por;
        this.observacoes = observacoes;
        this.status = PagamentoStatus.CONFIRMADO;
    }

    /** Marca o saldo como isento. */
    public void isentar() {
        this.status = PagamentoStatus.ISENTO;
        this.formaPagamento = null;
        this.recebidoEm = null;
        this.recebidoPor = null;
    }

    /** Reabre o saldo para novo recebimento (nao permitido se cancelado). */
    public void reabrir() {
        if (status == PagamentoStatus.CANCELADO) {
            throw new IllegalStateException("Saldo cancelado nao pode ser reaberto.");
        }
        this.status = valor != null && valor.signum() > 0
                ? PagamentoStatus.PENDENTE
                : PagamentoStatus.ISENTO;
        this.recebidoEm = null;
        this.recebidoPor = null;
        this.formaPagamento = null;
    }

    /** Cancela o saldo junto com o contrato/agendamento. */
    public void cancelar() {
        if (status != PagamentoStatus.CONFIRMADO) {
            this.status = PagamentoStatus.CANCELADO;
        }
    }

    /** Atualiza o valor previsto sem alterar o estado de recebimento. */
    public void redefinirValor(BigDecimal valor) {
        this.valor = valor;
        if (status == null) {
            this.status = valor != null && valor.signum() > 0
                    ? PagamentoStatus.PENDENTE
                    : PagamentoStatus.ISENTO;
        }
    }

    public void observar(String observacoes) {
        this.observacoes = observacoes;
    }

    public boolean estaQuitado() {
        return status != null && status.estaQuitado();
    }
}
