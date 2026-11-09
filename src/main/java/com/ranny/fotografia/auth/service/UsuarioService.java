package com.ranny.fotografia.auth.service;

import com.ranny.fotografia.auth.api.AuthDtos;
import com.ranny.fotografia.auth.model.AuthEvento;
import com.ranny.fotografia.auth.model.AuthEventoTipo;
import com.ranny.fotografia.auth.model.Role;
import com.ranny.fotografia.auth.model.Usuario;
import com.ranny.fotografia.auth.repository.AuthEventoRepository;
import com.ranny.fotografia.auth.repository.UsuarioRepository;
import com.ranny.fotografia.shared.exception.ConflitoException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Gestao administrativa de usuarios. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AuthEventoRepository eventoRepository;
    private final PasswordEncoder passwordEncoder;
    private final SenhaService senhaService;
    private final TokenService tokenService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          AuthEventoRepository eventoRepository,
                          PasswordEncoder passwordEncoder,
                          SenhaService senhaService,
                          TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.passwordEncoder = passwordEncoder;
        this.senhaService = senhaService;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public List<AuthDtos.UsuarioResponse> listar() {
        return AuthDtos.UsuarioResponse.de(usuarioRepository.findAllByOrderByNomeAsc());
    }

    @Transactional
    public AuthDtos.SenhaTemporariaResponse criar(AuthDtos.CriarUsuarioRequest request,
                                                  String adminId, String ip, String userAgent) {
        String email = Usuario.normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("EMAIL_EM_USO", "Ja existe um usuario com este e-mail.");
        }

        Instant agora = Instant.now();
        String temporaria = senhaService.gerarTemporaria();
        Usuario usuario = Usuario.criar(request.nome(), email,
                passwordEncoder.encode(temporaria), request.role(), true, agora);
        usuarioRepository.save(usuario);
        registrar(usuario.getId(), AuthEventoTipo.USUARIO_CRIADO, adminId, ip, userAgent,
                "Usuario criado por administrador.", agora);
        return new AuthDtos.SenhaTemporariaResponse(
                AuthDtos.UsuarioResponse.de(usuario), temporaria);
    }

    @Transactional
    public AuthDtos.SenhaTemporariaResponse resetarSenha(String usuarioId, String adminId,
                                                         String ip, String userAgent) {
        Usuario usuario = obter(usuarioId);
        if (usuario.getId().equals(adminId)) {
            throw new RegraDeNegocioException("RESET_PROPRIO",
                    "Use a troca de senha do seu proprio perfil para alterar sua senha.");
        }

        Instant agora = Instant.now();
        String temporaria = senhaService.gerarTemporaria();
        usuario.resetarSenha(passwordEncoder.encode(temporaria), agora);
        usuarioRepository.save(usuario);
        tokenService.revogarTodos(usuario.getId(), agora);
        registrar(usuario.getId(), AuthEventoTipo.SENHA_RESETADA_POR_ADMIN,
                adminId, ip, userAgent, "Senha resetada por administrador.", agora);
        return new AuthDtos.SenhaTemporariaResponse(
                AuthDtos.UsuarioResponse.de(usuario), temporaria);
    }

    @Transactional
    public AuthDtos.UsuarioResponse atualizar(String usuarioId,
                                              AuthDtos.AtualizarUsuarioRequest request,
                                              String adminId, String ip, String userAgent) {
        Usuario usuario = obter(usuarioId);
        if (usuario.getRole() == Role.ADMIN && request.role() != Role.ADMIN
                && usuarioRepository.countByRole(Role.ADMIN) <= 1) {
            throw new RegraDeNegocioException("ULTIMO_ADMIN",
                    "Nao e possivel remover o perfil do ultimo administrador.");
        }
        Instant agora = Instant.now();
        usuario.atualizarPerfil(request.nome(), request.role(), agora);
        usuarioRepository.save(usuario);
        registrar(usuario.getId(), AuthEventoTipo.USUARIO_ATUALIZADO, adminId, ip, userAgent,
                "Usuario atualizado por administrador.", agora);
        return AuthDtos.UsuarioResponse.de(usuario);
    }

    @Transactional
    public AuthDtos.UsuarioResponse alterarAtivo(String usuarioId, boolean ativo, String adminId,
                                                 String ip, String userAgent) {
        Usuario usuario = obter(usuarioId);
        if (!ativo && usuario.getId().equals(adminId)) {
            throw new RegraDeNegocioException("DESATIVAR_PROPRIO",
                    "Nao e possivel desativar o proprio usuario.");
        }
        if (!ativo && usuario.getRole() == Role.ADMIN
                && usuarioRepository.countByRole(Role.ADMIN) <= 1) {
            throw new RegraDeNegocioException("ULTIMO_ADMIN",
                    "Nao e possivel desativar o ultimo administrador.");
        }
        Instant agora = Instant.now();
        usuario.definirAtivo(ativo, agora);
        usuarioRepository.save(usuario);
        if (!ativo) {
            tokenService.revogarTodos(usuario.getId(), agora);
        }
        registrar(usuario.getId(),
                ativo ? AuthEventoTipo.USUARIO_ATIVADO : AuthEventoTipo.USUARIO_DESATIVADO,
                adminId, ip, userAgent,
                ativo ? "Usuario ativado." : "Usuario desativado.", agora);
        return AuthDtos.UsuarioResponse.de(usuario);
    }

    private Usuario obter(String usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));
    }

    private void registrar(String usuarioId, AuthEventoTipo tipo, String adminId,
                           String ip, String userAgent, String detalhe, Instant agora) {
        eventoRepository.save(AuthEvento.criar(usuarioId, tipo, ip, userAgent,
                detalhe == null ? null : detalhe + " (admin: " + adminId + ")", agora));
    }
}
