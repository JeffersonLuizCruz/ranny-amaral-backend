package com.ranny.fotografia.galeria.model;

import com.ranny.fotografia.shared.model.Auditoria;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Pedido de fotos extras gerado pela selecao. */
@Entity
@Table(name = "pedidos_extras")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PedidoExtras {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "galeria_id", nullable = false, length = 64)
    private String galeriaId;

    @Column(name = "selecao_id", nullable = false, length = 64)
    private String selecaoId;

    @Column(name = "quantidade_fotos", nullable = false)
    private int quantidadeFotos;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PedidoExtrasStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamentoSaldo formaPagamento;

    @Column(name = "confirmado_em")
    private Instant confirmadoEm;

    @Column(name = "confirmado_por", length = 120)
    private String confirmadoPor;

    @Column(name = "liberacao_motivo", length = 500)
    private String liberacaoMotivo;

    @Column(name = "liberado_por", length = 120)
    private String liberadoPor;

    @Column(name = "liberado_em")
    private Instant liberadoEm;

    @Embedded
    private Auditoria auditoria;

    public static PedidoExtras criar(String galeriaId, String selecaoId, int quantidade,
                                     BigDecimal valorUnitario, BigDecimal valorTotal) {
        PedidoExtras pedido = new PedidoExtras();
        pedido.id = UUID.randomUUID().toString();
        pedido.galeriaId = galeriaId;
        pedido.selecaoId = selecaoId;
        pedido.quantidadeFotos = quantidade;
        pedido.valorUnitario = valorUnitario;
        pedido.valorTotal = valorTotal;
        pedido.status = PedidoExtrasStatus.PENDENTE;
        pedido.auditoria = Auditoria.iniciar();
        return pedido;
    }

    public void pagar(FormaPagamentoSaldo forma, String por) {
        this.status = PedidoExtrasStatus.PAGO;
        this.formaPagamento = forma;
        this.confirmadoEm = Instant.now();
        this.confirmadoPor = por;
        this.auditoria.marcarAtualizacao();
    }

    public void isentar(String por) {
        this.status = PedidoExtrasStatus.ISENTO;
        this.confirmadoEm = Instant.now();
        this.confirmadoPor = por;
        this.auditoria.marcarAtualizacao();
    }

    public void liberarManual(String motivo, String por) {
        this.status = PedidoExtrasStatus.ISENTO;
        this.liberacaoMotivo = motivo;
        this.liberadoPor = por;
        this.liberadoEm = Instant.now();
        this.auditoria.marcarAtualizacao();
    }

    public void cancelar() {
        this.status = PedidoExtrasStatus.CANCELADO;
        this.auditoria.marcarAtualizacao();
    }

    public boolean estaQuitado() {
        return status == PedidoExtrasStatus.PAGO || status == PedidoExtrasStatus.ISENTO;
    }
}
