package com.ranny.fotografia.contrato.model;

import com.ranny.fotografia.contrato.model.converter.ContratoClausulasConverter;
import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import com.ranny.fotografia.shared.model.Auditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Modelo (template) de contrato.
 *
 * <p>PATTERN: Prototype (duplicacao) + Rich Domain Model.
 *
 * <p>Duplicar um modelo e uma operacao de dominio: gera um novo agregado com o
 * mesmo conteudo, versao inicial e sem compartilhar referencias mutaveis. A
 * versao e incrementada a cada edicao para que o snapshot registre qual versao
 * do texto foi aceita.
 */
@Entity
@Table(name = "modelos_contrato")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModeloContrato {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Convert(converter = ContratoClausulasConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<ContratoClausula> clausulas;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false)
    private int versao;

    @Embedded
    private Auditoria auditoria;

    public static ModeloContrato criar(String nome, String descricao,
                                       List<ContratoClausula> clausulas, boolean ativo) {
        ModeloContrato modelo = new ModeloContrato();
        modelo.id = UUID.randomUUID().toString();
        modelo.auditoria = Auditoria.iniciar();
        modelo.aplicar(nome, descricao, clausulas, ativo, 1);
        return modelo;
    }

    /** Usado pelo seed de desenvolvimento. */
    public static ModeloContrato restaurar(String id, String nome, String descricao,
                                           List<ContratoClausula> clausulas, boolean ativo, int versao) {
        ModeloContrato modelo = new ModeloContrato();
        modelo.id = id;
        modelo.auditoria = Auditoria.iniciar();
        modelo.aplicar(nome, descricao, clausulas, ativo, versao);
        return modelo;
    }

    public void atualizar(String nome, String descricao, List<ContratoClausula> clausulas, boolean ativo) {
        aplicar(nome, descricao, clausulas, ativo, this.versao + 1);
        auditoria.marcarAtualizacao();
    }

    public void alternarAtivo() {
        this.ativo = !this.ativo;
        auditoria.marcarAtualizacao();
    }

    /** Cria uma copia independente com novo id e versao inicial. */
    public ModeloContrato duplicar() {
        return criar(
                nome + " (copia)",
                descricao,
                this.clausulas,
                false);
    }

    private void aplicar(String nome, String descricao, List<ContratoClausula> clausulas,
                         boolean ativo, int versao) {
        this.nome = nome;
        this.descricao = descricao;
        this.clausulas = clausulas == null ? List.of() : List.copyOf(clausulas);
        this.ativo = ativo;
        this.versao = versao;
    }
}
