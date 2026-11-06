package com.ranny.fotografia.contrato.model;

import com.ranny.fotografia.contrato.model.converter.DadosAceiteConverter;
import com.ranny.fotografia.contrato.model.documento.DadosAceite;
import com.ranny.fotografia.contrato.model.documento.StatusSolicitacaoAceite;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.model.Auditoria;
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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Link publico de aceite de um contrato.
 *
 * <p>PATTERN: State + Token (capability).
 *
 * <p>O token e a credencial publica (UUID). O estado controla o ciclo de vida:
 * pendente -> visualizada -> aceita, ou expirada/cancelada. As transicoes sao
 * metodos da entidade, impedindo que um link aceito seja reutilizado.
 */
@Entity
@Table(name = "solicitacoes_aceite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolicitacaoAceite {

    public static final int VALIDADE_PADRAO_DIAS = 7;

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "contrato_id", nullable = false, length = 64)
    private String contratoId;

    @Column(nullable = false, unique = true, length = 80)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacaoAceite status;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "visualizada_em")
    private Instant visualizadaEm;

    @Column(unique = true, length = 30)
    private String protocolo;

    @Convert(converter = DadosAceiteConverter.class)
    @Column(columnDefinition = "TEXT")
    private DadosAceite dadosAceite;

    @Embedded
    private Auditoria auditoria;

    public static SolicitacaoAceite criar(String contratoId, int validadeDias) {
        SolicitacaoAceite solicitacao = new SolicitacaoAceite();
        solicitacao.id = UUID.randomUUID().toString();
        solicitacao.auditoria = Auditoria.iniciar();
        solicitacao.contratoId = contratoId;
        solicitacao.token = UUID.randomUUID().toString();
        solicitacao.status = StatusSolicitacaoAceite.PENDENTE;
        solicitacao.criadaEm = Instant.now();
        solicitacao.expiraEm = solicitacao.criadaEm.plus(Duration.ofDays(validadeDias));
        return solicitacao;
    }

    /** Usado pelo seed de desenvolvimento. */
    public static SolicitacaoAceite restaurar(String id, String contratoId, String token,
                                              StatusSolicitacaoAceite status, Instant criadaEm,
                                              Instant expiraEm, Instant visualizadaEm,
                                              DadosAceite dadosAceite, String protocolo) {
        SolicitacaoAceite solicitacao = new SolicitacaoAceite();
        solicitacao.id = id;
        solicitacao.auditoria = Auditoria.iniciar();
        solicitacao.contratoId = contratoId;
        solicitacao.token = token;
        solicitacao.status = status;
        solicitacao.criadaEm = criadaEm;
        solicitacao.expiraEm = expiraEm;
        solicitacao.visualizadaEm = visualizadaEm;
        solicitacao.dadosAceite = dadosAceite;
        solicitacao.protocolo = protocolo;
        return solicitacao;
    }

    public boolean estaExpirada(Instant referencia) {
        return status.estaAberta() && !expiraEm.isAfter(referencia);
    }

    public void visualizar(Instant em) {
        if (status == StatusSolicitacaoAceite.PENDENTE) {
            this.status = StatusSolicitacaoAceite.VISUALIZADA;
            this.visualizadaEm = em;
            auditoria.marcarAtualizacao();
        }
    }

    public void aceitar(DadosAceite dados) {
        if (!status.estaAberta()) {
            throw new RegraDeNegocioException("LINK_NAO_ACEITAVEL",
                    "Este link de aceite nao esta mais disponivel.");
        }
        this.status = StatusSolicitacaoAceite.ACEITA;
        this.dadosAceite = dados;
        this.protocolo = dados.protocolo();
        auditoria.marcarAtualizacao();
    }

    public void expirar() {
        if (status.estaAberta()) {
            this.status = StatusSolicitacaoAceite.EXPIRADA;
            auditoria.marcarAtualizacao();
        }
    }

    public void cancelar() {
        if (status.estaAberta()) {
            this.status = StatusSolicitacaoAceite.CANCELADA;
            auditoria.marcarAtualizacao();
        }
    }
}
