package com.ranny.fotografia.auth.service;

import com.ranny.fotografia.auth.api.AuthDtos;
import com.ranny.fotografia.auth.model.AuthEvento;
import com.ranny.fotografia.auth.model.AuthEventoTipo;
import com.ranny.fotografia.auth.model.RefreshToken;
import com.ranny.fotografia.auth.model.Usuario;
import com.ranny.fotografia.auth.repository.AuthEventoRepository;
import com.ranny.fotografia.auth.repository.UsuarioRepository;
import com.ranny.fotografia.auth.security.AuthProperties;
import com.ranny.fotografia.shared.exception.ContaBloqueadaException;
import com.ranny.fotografia.shared.exception.CredenciaisInvalidasException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.exception.SessaoInvalidaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Casos de uso de login, refresh, logout e troca de senha. */
@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final AuthEventoRepository eventoRepository;
    private final TokenService tokenService;
    private final SenhaService senhaService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;
    private final String hashFicticio;

    public AutenticacaoService(UsuarioRepository usuarioRepository,
                               AuthEventoRepository eventoRepository,
                               TokenService tokenService,
                               SenhaService senhaService,
                               PasswordEncoder passwordEncoder,
                               AuthProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.tokenService = tokenService;
        this.senhaService = senhaService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-para-tempo-constante");
    }

    @Transactional
    public SessaoCriada login(String email, String senha, String ip, String userAgent) {
        Instant agora = Instant.now();
        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCase(Usuario.normalizarEmail(email))
                .orElse(null);

        if (usuario == null) {
            passwordEncoder.matches(senha == null ? "" : senha, hashFicticio);
            registrar(null, AuthEventoTipo.LOGIN_FALHA, ip, userAgent, "E-mail inexistente.", agora);
            throw new CredenciaisInvalidasException();
        }

        if (usuario.estaBloqueado(agora)) {
            registrar(usuario.getId(), AuthEventoTipo.LOGIN_BLOQUEADO, ip, userAgent,
                    "Conta bloqueada por tentativas.", agora);
            throw new ContaBloqueadaException(
                    "Muitas tentativas. Tente novamente em alguns minutos.");
        }

        if (!usuario.isAtivo() || !passwordEncoder.matches(senha == null ? "" : senha,
                usuario.getSenhaHash())) {
            usuario.registrarFalha(properties.getMaxTentativas(),
                    Duration.ofMinutes(properties.getBloqueioMinutos()), agora);
            usuarioRepository.save(usuario);
            registrar(usuario.getId(), AuthEventoTipo.LOGIN_FALHA, ip, userAgent,
                    "Senha invalida ou usuario inativo.", agora);
            throw new CredenciaisInvalidasException();
        }

        usuario.registrarLoginSucesso(agora);
        usuarioRepository.save(usuario);
        registrar(usuario.getId(), AuthEventoTipo.LOGIN_SUCESSO, ip, userAgent, null, agora);
        return criarSessao(usuario, ip, userAgent);
    }

    @Transactional
    public SessaoCriada refresh(String valor, String ip, String userAgent) {
        Instant agora = Instant.now();
        RefreshToken atual = tokenService.buscarPorValor(valor)
                .orElseThrow(SessaoInvalidaException::new);

        if (!atual.estaAtivo(agora)) {
            if (atual.getRevogadoEm() != null && atual.getSubstituidoPor() != null) {
                tokenService.revogarTodos(atual.getUsuarioId(), agora);
                registrar(atual.getUsuarioId(), AuthEventoTipo.REFRESH_REUTILIZADO,
                        ip, userAgent, "Refresh token reutilizado.", agora);
            }
            throw new SessaoInvalidaException();
        }

        Usuario usuario = usuarioRepository.findById(atual.getUsuarioId())
                .orElseThrow(SessaoInvalidaException::new);
        if (!usuario.isAtivo()) {
            atual.revogar(agora);
            throw new SessaoInvalidaException();
        }

        TokenService.RefreshGerado novo = tokenService.gerarRefresh(usuario, ip, userAgent);
        atual.marcarSubstituido(novo.token().getId(), agora);
        registrar(usuario.getId(), AuthEventoTipo.REFRESH_ROTACIONADO, ip, userAgent,
                null, agora);
        return new SessaoCriada(tokenService.gerarAccessToken(usuario),
                tokenService.accessTokenExpiresInSeconds(), novo.valor(), usuario);
    }

    @Transactional
    public void logout(String valor, String ip, String userAgent) {
        Instant agora = Instant.now();
        tokenService.buscarPorValor(valor).ifPresent(token -> {
            token.revogar(agora);
            registrar(token.getUsuarioId(), AuthEventoTipo.LOGOUT, ip, userAgent, null, agora);
        });
    }

    @Transactional
    public SessaoCriada alterarSenha(String usuarioId, String senhaAtual, String novaSenha,
                                     String ip, String userAgent) {
        Instant agora = Instant.now();
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));

        if (!passwordEncoder.matches(senhaAtual == null ? "" : senhaAtual,
                usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("SENHA_ATUAL_INVALIDA",
                    "A senha atual informada esta incorreta.");
        }
        if (senhaAtual != null && senhaAtual.equals(novaSenha)) {
            throw new RegraDeNegocioException("SENHA_REPETIDA",
                    "A nova senha deve ser diferente da atual.");
        }
        senhaService.validar(novaSenha);

        usuario.alterarSenha(passwordEncoder.encode(novaSenha), agora);
        usuarioRepository.save(usuario);
        tokenService.revogarTodos(usuario.getId(), agora);
        registrar(usuario.getId(), AuthEventoTipo.SENHA_ALTERADA, ip, userAgent, null, agora);
        return criarSessao(usuario, ip, userAgent);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UsuarioResponse perfil(String usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));
        return AuthDtos.UsuarioResponse.de(usuario);
    }

    private SessaoCriada criarSessao(Usuario usuario, String ip, String userAgent) {
        TokenService.RefreshGerado refresh = tokenService.gerarRefresh(usuario, ip, userAgent);
        return new SessaoCriada(tokenService.gerarAccessToken(usuario),
                tokenService.accessTokenExpiresInSeconds(), refresh.valor(), usuario);
    }

    private void registrar(String usuarioId, AuthEventoTipo tipo, String ip, String userAgent,
                           String detalhe, Instant agora) {
        eventoRepository.save(AuthEvento.criar(usuarioId, tipo, ip, userAgent, detalhe, agora));
    }

    public record SessaoCriada(String accessToken, long expiresInSeconds,
                               String refreshToken, Usuario usuario) {
    }
}
