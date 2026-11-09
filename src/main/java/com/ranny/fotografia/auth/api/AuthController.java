package com.ranny.fotografia.auth.api;

import com.ranny.fotografia.auth.security.AuthCookieFactory;
import com.ranny.fotografia.auth.security.AuthProperties;
import com.ranny.fotografia.auth.service.AutenticacaoService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login, refresh, logout e troca de senha. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AutenticacaoService autenticacaoService;
    private final AuthCookieFactory cookieFactory;
    private final AuthProperties properties;

    public AuthController(AutenticacaoService autenticacaoService,
                          AuthCookieFactory cookieFactory, AuthProperties properties) {
        this.autenticacaoService = autenticacaoService;
        this.cookieFactory = cookieFactory;
        this.properties = properties;
    }

    @PostMapping("/login")
    public AuthDtos.SessaoResponse login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                         HttpServletRequest http, HttpServletResponse response) {
        var sessao = autenticacaoService.login(request.email(), request.senha(),
                ip(http), userAgent(http));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.criar(sessao.refreshToken()).toString());
        return resposta(sessao);
    }

    @PostMapping("/refresh")
    public AuthDtos.SessaoResponse refresh(HttpServletRequest http, HttpServletResponse response) {
        var sessao = autenticacaoService.refresh(cookie(http), ip(http), userAgent(http));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.criar(sessao.refreshToken()).toString());
        return resposta(sessao);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest http, HttpServletResponse response) {
        autenticacaoService.logout(cookie(http), ip(http), userAgent(http));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.limpar().toString());
    }

    @GetMapping("/me")
    public AuthDtos.UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return autenticacaoService.perfil(jwt.getSubject());
    }

    @PostMapping("/change-password")
    public AuthDtos.SessaoResponse alterarSenha(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AuthDtos.AlterarSenhaRequest request,
            HttpServletRequest http, HttpServletResponse response) {
        var sessao = autenticacaoService.alterarSenha(jwt.getSubject(),
                request.senhaAtual(), request.novaSenha(), ip(http), userAgent(http));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.criar(sessao.refreshToken()).toString());
        return resposta(sessao);
    }

    private static AuthDtos.SessaoResponse resposta(AutenticacaoService.SessaoCriada sessao) {
        return new AuthDtos.SessaoResponse(
                sessao.accessToken(),
                sessao.expiresInSeconds(),
                AuthDtos.UsuarioResponse.de(sessao.usuario()),
                sessao.usuario().isDeveTrocarSenha());
    }

    private String cookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
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
