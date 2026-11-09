package com.ranny.fotografia.auth.api;

import com.ranny.fotografia.auth.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestao de usuarios, restrita ao perfil ADMIN. */
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<AuthDtos.UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.SenhaTemporariaResponse criar(
            @Valid @RequestBody AuthDtos.CriarUsuarioRequest request,
            @AuthenticationPrincipal Jwt jwt, HttpServletRequest http) {
        return usuarioService.criar(request, jwt.getSubject(), ip(http), userAgent(http));
    }

    @PostMapping("/{id}/reset-password")
    public AuthDtos.SenhaTemporariaResponse resetarSenha(
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest http) {
        return usuarioService.resetarSenha(id, jwt.getSubject(), ip(http), userAgent(http));
    }

    @PutMapping("/{id}")
    public AuthDtos.UsuarioResponse atualizar(
            @PathVariable String id,
            @Valid @RequestBody AuthDtos.AtualizarUsuarioRequest request,
            @AuthenticationPrincipal Jwt jwt, HttpServletRequest http) {
        return usuarioService.atualizar(id, request, jwt.getSubject(), ip(http), userAgent(http));
    }

    @PatchMapping("/{id}/ativo")
    public AuthDtos.UsuarioResponse alterarAtivo(
            @PathVariable String id,
            @Valid @RequestBody AuthDtos.AtivoRequest request,
            @AuthenticationPrincipal Jwt jwt, HttpServletRequest http) {
        return usuarioService.alterarAtivo(id, request.ativo(), jwt.getSubject(),
                ip(http), userAgent(http));
    }

    private static String ip(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest request) {
        String agente = request.getHeader("User-Agent");
        return agente == null ? null : agente.substring(0, Math.min(agente.length(), 300));
    }
}
