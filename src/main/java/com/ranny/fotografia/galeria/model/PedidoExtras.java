package com.ranny.fotografia.galeria.model;

import com.ranny.fotografia.shared.financeiro.FaixaAplicada;
import com.ranny.fotografia.shared.financeiro.FaixasAplicadasConverter;
import com.ranny.fotografia.shared.model.Auditoria;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.List;
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

    @Convert(converter = FaixasAplicadasConverter.class)
    @Column(name = "detalhamento_extras", columnDefinition = "text")
    private List<FaixaAplicada> detalhamento;

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

    @Column(name = "chave_pix", length = 180)
    private String chavePix;

    @Column(name = "instrucoes_pagamento", length = 1500)
    private String instrucoesPagamento;

    @Column(name = "comprovante_arquivo_id", length = 64)
    private String comprovanteArquivoId;

    @Column(name = "comprovante_nome", length = 255)
    private String comprovanteNome;

    @Column(name = "comprovante_enviado_em")
    private Instant comprovanteEnviadoEm;

    @Embedded
    private Auditoria auditoria;

    public static PedidoExtras criar(String galeriaId, String selecaoId, int quantidade,
                                     BigDecimal valorUnitario, BigDecimal valorTotal,
                                     String chavePix, String instrucoesPagamento) {
        return criar(galeriaId, selecaoId, quantidade, valorUnitario, valorTotal,
                List.of(), chavePix, instrucoesPagamento);
    }

    public static PedidoExtras criar(String galeriaId, String selecaoId, int quantidade,
                                     BigDecimal valorUnitario, BigDecimal valorTotal,
                                     List<FaixaAplicada> detalhamento,
                                     String chavePix, String instrucoesPagamento) {
        PedidoExtras pedido = new PedidoExtras();
        pedido.id = UUID.randomUUID().toString();
        pedido.galeriaId = galeriaId;
        pedido.selecaoId = selecaoId;
        pedido.quantidadeFotos = quantidade;
        pedido.valorUnitario = valorUnitario;
        pedido.valorTotal = valorTotal;
        pedido.detalhamento = detalhamento == null ? List.of() : List.copyOf(detalhamento);
        pedido.chavePix = chavePix;
        pedido.instrucoesPagamento = instrucoesPagamento;
        pedido.status = PedidoExtrasStatus.PENDENTE;
        pedido.auditoria = Auditoria.iniciar();
        return pedido;
    }

    /** Copia defensiva do detalhamento progressivo. */
    public List<FaixaAplicada> getDetalhamento() {
        return detalhamento == null ? List.of() : List.copyOf(detalhamento);
    }

    public void pagar(FormaPagamentoSaldo forma, String por) {        this.status = PedidoExtrasStatus.PAGO;
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

    public void anexarComprovante(String arquivoId, String nome, Instant enviadoEm) {
        this.comprovanteArquivoId = arquivoId;
        this.comprovanteNome = nome;
        this.comprovanteEnviadoEm = enviadoEm;
        this.auditoria.marcarAtualizacao();
    }

    public boolean estaQuitado() {
        return status == PedidoExtrasStatus.PAGO || status == PedidoExtrasStatus.ISENTO;
    }
}
