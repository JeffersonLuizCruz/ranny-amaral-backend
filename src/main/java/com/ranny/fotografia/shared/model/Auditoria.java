package com.ranny.fotografia.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.time.Instant;

/**
 * PATTERN: Value Object (DDD) + Composition over Inheritance.
 *
 * <p>Em vez de uma superclasse {@code BaseEntity} (heranca usada apenas para
 * reaproveitar tres campos), a auditoria e um objeto de valor embutido. Cada
 * entidade continua livre para escolher o seu proprio ciclo de vida, e a
 * composicao evita o acoplamento rigido que uma hierarquia de entidades cria.
 *
 * <p>Beneficios: o mesmo objeto pode ser testado isoladamente, o mapeamento JPA
 * fica explicito em cada entidade e nao existe "heranca por conveniencia".
 */
@Embeddable
@Getter
public class Auditoria {

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Auditoria() {
        // Exigido pelo JPA.
    }

    private Auditoria(Instant criadoEm, Instant atualizadoEm) {
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    /** Cria a auditoria com o instante atual. */
    public static Auditoria iniciar() {
        Instant agora = Instant.now();
        return new Auditoria(agora, agora);
    }

    /** Cria a auditoria em um instante fixo (usado pelo seed de demonstracao). */
    public static Auditoria em(Instant instante) {
        return new Auditoria(instante, instante);
    }

    /** Marca o registro como alterado agora. */
    public void marcarAtualizacao() {
        this.atualizadoEm = Instant.now();
    }

    /** Marca a atualizacao em um instante fixo (usado pelo seed de demonstracao). */
    public void marcarAtualizacaoEm(Instant instante) {
        this.atualizadoEm = instante;
    }
}
