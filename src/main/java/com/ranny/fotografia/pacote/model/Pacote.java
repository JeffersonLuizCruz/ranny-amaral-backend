package com.ranny.fotografia.pacote.model;

import com.ranny.fotografia.shared.model.Auditoria;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PATTERN: Rich Domain Model + Composition.
 *
 * <p>O pacote e um agregado de catalogo. As quantidades e o preco da foto extra
 * sao calculados/inferidos na criacao e depois congelados no agendamento; por
 * isso a entidade guarda os valores explicitamente, em vez de recalcular a
 * partir de textos de marketing.
 *
 * <p>{@code itensInclusos} e um {@code @ElementCollection} carregado sob
 * demanda (LAZY): a lista so e lida quando o DTO e montado, dentro da
 * transacao. Nao ha {@code EAGER}.
 */
@Entity
@Table(name = "pacotes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pacote {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(name = "cor_hex", nullable = false, length = 9)
    private String corHex;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "pacote_itens", joinColumns = @JoinColumn(name = "pacote_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "item", nullable = false, length = 200)
    private List<String> itensInclusos = new ArrayList<>();

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "quantidade_fotos_inclusas", nullable = false)
    private int quantidadeFotosInclusas;

    @Column(name = "quantidade_videos_inclusos", nullable = false)
    private int quantidadeVideosInclusos;

    @Column(name = "valor_foto_extra", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFotoExtra;

    @Embedded
    private PromocaoPacote promocao;

    @Embedded
    private Auditoria auditoria;

    public static Pacote criar(String nome, int duracaoMinutos, BigDecimal valor, String descricao,
                               String corHex, List<String> itensInclusos, boolean ativo,
                               int quantidadeFotosInclusas, int quantidadeVideosInclusos,
                               BigDecimal valorFotoExtra, PromocaoPacote promocao) {
        Pacote pacote = new Pacote();
        pacote.id = UUID.randomUUID().toString();
        pacote.auditoria = Auditoria.iniciar();
        pacote.aplicar(nome, duracaoMinutos, valor, descricao, corHex, itensInclusos, ativo,
                quantidadeFotosInclusas, quantidadeVideosInclusos, valorFotoExtra, promocao);
        return pacote;
    }

    /** Usado pelo seed de desenvolvimento para preservar IDs de demonstracao. */
    public static Pacote restaurar(String id, String nome, int duracaoMinutos, BigDecimal valor,
                                   String descricao, String corHex, List<String> itensInclusos,
                                   boolean ativo, int quantidadeFotosInclusas,
                                   int quantidadeVideosInclusos, BigDecimal valorFotoExtra,
                                   PromocaoPacote promocao) {
        Pacote pacote = new Pacote();
        pacote.id = id;
        pacote.auditoria = Auditoria.iniciar();
        pacote.aplicar(nome, duracaoMinutos, valor, descricao, corHex, itensInclusos, ativo,
                quantidadeFotosInclusas, quantidadeVideosInclusos, valorFotoExtra, promocao);
        return pacote;
    }

    public void atualizar(String nome, int duracaoMinutos, BigDecimal valor, String descricao,
                          String corHex, List<String> itensInclusos, boolean ativo,
                          int quantidadeFotosInclusas, int quantidadeVideosInclusos,
                          BigDecimal valorFotoExtra, PromocaoPacote promocao) {
        aplicar(nome, duracaoMinutos, valor, descricao, corHex, itensInclusos, ativo,
                quantidadeFotosInclusas, quantidadeVideosInclusos, valorFotoExtra, promocao);
        auditoria.marcarAtualizacao();
    }

    public void alternarAtivo() {
        this.ativo = !this.ativo;
        auditoria.marcarAtualizacao();
    }

    /** Copia defensiva: o agregado nao expoe sua lista interna mutavel. */
    public List<String> getItensInclusos() {
        return List.copyOf(itensInclusos);
    }

    private void aplicar(String nome, int duracaoMinutos, BigDecimal valor, String descricao,
                         String corHex, List<String> itensInclusos, boolean ativo,
                         int quantidadeFotosInclusas, int quantidadeVideosInclusos,
                         BigDecimal valorFotoExtra, PromocaoPacote promocao) {
        this.nome = nome;
        this.duracaoMinutos = duracaoMinutos;
        this.valor = valor;
        this.descricao = descricao;
        this.corHex = corHex;
        this.itensInclusos = itensInclusos == null ? new ArrayList<>() : new ArrayList<>(itensInclusos);
        this.ativo = ativo;
        this.quantidadeFotosInclusas = Math.max(0, quantidadeFotosInclusas);
        this.quantidadeVideosInclusos = Math.max(0, quantidadeVideosInclusos);
        this.valorFotoExtra = valorFotoExtra == null ? BigDecimal.ZERO : valorFotoExtra;
        this.promocao = promocao;
    }
}
