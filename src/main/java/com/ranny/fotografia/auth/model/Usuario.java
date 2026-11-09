package com.ranny.fotografia.auth.model;

import com.ranny.fotografia.shared.model.Auditoria;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Usuario da aplicacao.
 *
 * <p>O estado so muda por metodos de dominio: login, falha, bloqueio, troca e
 * reset de senha. A senha e sempre persistida como hash BCrypt.
 */
@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "deve_trocar_senha", nullable = false)
    private boolean deveTrocarSenha;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Column(name = "ultimo_login_em")
    private Instant ultimoLoginEm;

    @Column(name = "senha_alterada_em")
    private Instant senhaAlteradaEm;

    @Embedded
    private Auditoria auditoria;

    public static Usuario criar(String nome, String email, String senhaHash, Role role,
                                boolean deveTrocarSenha, Instant senhaAlteradaEm) {
        Usuario usuario = new Usuario();
        usuario.id = UUID.randomUUID().toString();
        usuario.auditoria = Auditoria.iniciar();
        usuario.nome = nome == null ? null : nome.trim();
        usuario.email = normalizarEmail(email);
        usuario.senhaHash = senhaHash;
        usuario.role = role == null ? Role.GESTOR : role;
        usuario.ativo = true;
        usuario.deveTrocarSenha = deveTrocarSenha;
        usuario.tokenVersion = 0;
        usuario.tentativasFalhas = 0;
        usuario.senhaAlteradaEm = senhaAlteradaEm;
        return usuario;
    }

    public boolean podeAutenticar(Instant agora) {
        return ativo && !estaBloqueado(agora);
    }

    public boolean estaBloqueado(Instant agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    public void registrarFalha(int maxTentativas, Duration duracaoBloqueio, Instant agora) {
        this.tentativasFalhas += 1;
        if (this.tentativasFalhas >= maxTentativas) {
            this.bloqueadoAte = agora.plus(duracaoBloqueio);
        }
        this.auditoria.marcarAtualizacaoEm(agora);
    }

    public void registrarLoginSucesso(Instant agora) {
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        this.ultimoLoginEm = agora;
        this.auditoria.marcarAtualizacaoEm(agora);
    }

    public void alterarSenha(String novoHash, Instant agora) {
        this.senhaHash = novoHash;
        this.deveTrocarSenha = false;
        this.senhaAlteradaEm = agora;
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        this.tokenVersion += 1;
        this.auditoria.marcarAtualizacaoEm(agora);
    }

    public void resetarSenha(String senhaTemporariaHash, Instant agora) {
        this.senhaHash = senhaTemporariaHash;
        this.deveTrocarSenha = true;
        this.senhaAlteradaEm = agora;
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        this.tokenVersion += 1;
        this.auditoria.marcarAtualizacaoEm(agora);
    }

    public void atualizarPerfil(String nome, Role role, Instant agora) {
        Role anterior = this.role;
        this.nome = nome == null ? this.nome : nome.trim();
        this.role = role == null ? this.role : role;
        if (anterior != this.role) {
            this.tokenVersion += 1;
        }
        this.auditoria.marcarAtualizacaoEm(agora);
    }

    public void definirAtivo(boolean ativo, Instant agora) {
        this.ativo = ativo;
        if (!ativo) {
            this.tokenVersion += 1;
        }
        this.auditoria.marcarAtualizacaoEm(agora);
    }

    public void marcarTrocaDeSenhaConcluida(Instant agora) {
        this.deveTrocarSenha = false;
        this.auditoria.marcarAtualizacaoEm(agora);
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
