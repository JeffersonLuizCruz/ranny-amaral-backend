package com.ranny.fotografia.cliente.model;

import com.ranny.fotografia.shared.model.Auditoria;
import com.ranny.fotografia.shared.model.TipoDocumento;
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

import java.util.UUID;

/**
 * PATTERN: Rich Domain Model + Composition (Auditoria embutida).
 *
 * <p>A entidade nao expoe setters: o estado so muda por metodos que expressam a
 * intencao do negocio ({@code atualizar}, {@code atualizarDocumento}). Isso
 * impede alteracoes acidentais a partir de DTOs e concentra as invariantes no
 * dominio. A auditoria vem por composicao, nao por heranca.
 *
 * <p>IDs sao UUID em texto ({@code varchar(64)}): o frontend usa IDs opacos e os
 * seeds precisam de IDs estaveis. Nao ha {@code @Data} para evitar
 * {@code equals}/{@code hashCode}/{@code toString} gerados sobre estado mutavel.
 */
@Entity
@Table(name = "clientes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cliente {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 30)
    private String whatsapp;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 10)
    private TipoDocumento tipoDocumento;

    @Column(length = 120)
    private String cidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClienteOrigem origem;

    @Column(length = 1000)
    private String observacoes;

    @Embedded
    private Auditoria auditoria;

    public static Cliente criar(String nome, String whatsapp, String email, String documento,
                                TipoDocumento tipoDocumento, String cidade,
                                ClienteOrigem origem, String observacoes) {
        Cliente cliente = new Cliente();
        cliente.id = UUID.randomUUID().toString();
        cliente.auditoria = Auditoria.iniciar();
        cliente.aplicar(nome, whatsapp, email, documento, tipoDocumento, cidade, origem, observacoes);
        return cliente;
    }

    /** Usado pelo seed de desenvolvimento para preservar IDs de demonstracao. */
    public static Cliente restaurar(String id, String nome, String whatsapp, String email,
                                    String documento, TipoDocumento tipoDocumento, String cidade,
                                    ClienteOrigem origem, String observacoes) {
        Cliente cliente = new Cliente();
        cliente.id = id;
        cliente.auditoria = Auditoria.iniciar();
        cliente.aplicar(nome, whatsapp, email, documento, tipoDocumento, cidade, origem, observacoes);
        return cliente;
    }

    public void atualizar(String nome, String whatsapp, String email, String documento,
                          TipoDocumento tipoDocumento, String cidade,
                          ClienteOrigem origem, String observacoes) {
        aplicar(nome, whatsapp, email, documento, tipoDocumento, cidade, origem, observacoes);
        auditoria.marcarAtualizacao();
    }

    /**
     * Atualiza apenas os dados pessoais confirmados no aceite, preservando a
     * origem e as observacoes internas do CRM.
     */
    public void atualizarDadosPessoais(String nome, String whatsapp, String email,
                                       String documento, TipoDocumento tipoDocumento, String cidade) {
        this.nome = nome;
        this.whatsapp = whatsapp;
        this.email = email == null || email.isBlank() ? this.email : email;
        this.documento = documento;
        this.tipoDocumento = tipoDocumento;
        this.cidade = cidade;
        auditoria.marcarAtualizacao();
    }

    private void aplicar(String nome, String whatsapp, String email, String documento,
                         TipoDocumento tipoDocumento, String cidade,
                         ClienteOrigem origem, String observacoes) {
        this.nome = nome;
        this.whatsapp = whatsapp;
        this.email = email;
        this.documento = documento;
        this.tipoDocumento = tipoDocumento;
        this.cidade = cidade;
        this.origem = origem;
        this.observacoes = observacoes;
    }
}
