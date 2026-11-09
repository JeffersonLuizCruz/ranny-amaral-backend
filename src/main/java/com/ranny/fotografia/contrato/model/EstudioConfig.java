package com.ranny.fotografia.contrato.model;

import com.ranny.fotografia.shared.model.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Configuracao unica do estudio (singleton de persistencia).
 *
 * <p>PATTERN: Singleton (no nivel do agregado) + Rich Domain Model.
 *
 * <p>O id fixo {@value #ID} garante que exista no maximo um registro. O service
 * cria a configuracao padrao no primeiro acesso e atualiza sempre a mesma linha,
 * sem tabela de chave-valor.
 */
@Entity
@Table(name = "estudio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EstudioConfig {

    public static final String ID = "estudio";

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(name = "razao_social", length = 160)
    private String razaoSocial;

    @Column(length = 20)
    private String cnpj;

    @Column(length = 200)
    private String endereco;

    @Column(length = 120)
    private String cidade;

    @Column(length = 2)
    private String estado;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String telefone;

    @Column(length = 120)
    private String responsavel;

    @Column(length = 180)
    private String site;

    @Column(length = 120)
    private String foro;

    @Column(name = "percentual_entrada_padrao", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualEntradaPadrao;

    @Column(name = "chave_pix", length = 180)
    private String chavePix;

    @Column(name = "instrucoes_pagamento", length = 1500)
    private String instrucoesPagamento;

    @Column(name = "chave_pix_extras", length = 180)
    private String chavePixExtras;

    @Column(name = "instrucoes_pagamento_extras", length = 1500)
    private String instrucoesPagamentoExtras;

    @Column(name = "prazo_pagamento_dias")
    private Integer prazoPagamentoDias;

    @Embedded
    private Auditoria auditoria;

    public static EstudioConfig padrao() {
        EstudioConfig estudio = new EstudioConfig();
        estudio.id = ID;
        estudio.auditoria = Auditoria.iniciar();
        estudio.aplicar("Estudio Fotografico", null, null, null, null, null, null, null,
                null, null, null, BigDecimal.valueOf(30), null, null, null, null, 3);
        return estudio;
    }

    /** Usado pelo seed de desenvolvimento. */
    public static EstudioConfig restaurar(String nome, String razaoSocial, String cnpj, String endereco,
                                          String cidade, String estado, String email, String telefone,
                                          String responsavel, String site, String foro,
                                          BigDecimal percentualEntradaPadrao, String chavePix,
                                          String instrucoesPagamento, String chavePixExtras,
                                          String instrucoesPagamentoExtras, Integer prazoPagamentoDias) {
        EstudioConfig estudio = new EstudioConfig();
        estudio.id = ID;
        estudio.auditoria = Auditoria.iniciar();
        estudio.aplicar(nome, razaoSocial, cnpj, endereco, cidade, estado, email, telefone,
                responsavel, site, foro, percentualEntradaPadrao, chavePix, instrucoesPagamento,
                chavePixExtras, instrucoesPagamentoExtras, prazoPagamentoDias);
        return estudio;
    }

    public void atualizar(String nome, String razaoSocial, String cnpj, String endereco,
                          String cidade, String estado, String email, String telefone,
                          String responsavel, String site, String foro,
                          BigDecimal percentualEntradaPadrao, String chavePix,
                          String instrucoesPagamento, String chavePixExtras,
                          String instrucoesPagamentoExtras, Integer prazoPagamentoDias) {
        aplicar(nome, razaoSocial, cnpj, endereco, cidade, estado, email, telefone,
                responsavel, site, foro, percentualEntradaPadrao, chavePix, instrucoesPagamento,
                chavePixExtras, instrucoesPagamentoExtras, prazoPagamentoDias);
        auditoria.marcarAtualizacao();
    }

    private void aplicar(String nome, String razaoSocial, String cnpj, String endereco,
                         String cidade, String estado, String email, String telefone,
                         String responsavel, String site, String foro,
                         BigDecimal percentualEntradaPadrao, String chavePix,
                         String instrucoesPagamento, String chavePixExtras,
                         String instrucoesPagamentoExtras, Integer prazoPagamentoDias) {
        this.nome = nome;
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.endereco = endereco;
        this.cidade = cidade;
        this.estado = estado;
        this.email = email;
        this.telefone = telefone;
        this.responsavel = responsavel;
        this.site = site;
        this.foro = foro;
        this.percentualEntradaPadrao = percentualEntradaPadrao == null
                ? BigDecimal.valueOf(30)
                : percentualEntradaPadrao;
        this.chavePix = chavePix;
        this.instrucoesPagamento = instrucoesPagamento;
        this.chavePixExtras = chavePixExtras;
        this.instrucoesPagamentoExtras = instrucoesPagamentoExtras;
        this.prazoPagamentoDias = prazoPagamentoDias;
    }
}
