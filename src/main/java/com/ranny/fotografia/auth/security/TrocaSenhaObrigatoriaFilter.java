package com.ranny.fotografia.auth.security;

import com.ranny.fotografia.shared.seguranca.borda.RespostaProblemaSeguranca;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Bloqueia o acesso ao CRM enquanto o usuario precisa trocar a senha.
 *
 * <p>Permite apenas perfil, troca de senha e logout. O frontend tambem
 * redireciona, mas a regra vale para chamadas diretas a API.
 */
@Component
public class TrocaSenhaObrigatoriaFilter extends OncePerRequestFilter {

    private static final Set<String> PERMITIDOS = Set.of(
            "/api/v1/auth/me",
            "/api/v1/auth/change-password",
            "/api/v1/auth/logout"
    );

    private final RespostaProblemaSeguranca respostaProblema;

    public TrocaSenhaObrigatoriaFilter(RespostaProblemaSeguranca respostaProblema) {
        this.respostaProblema = respostaProblema;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken jwt
                && Boolean.TRUE.equals(jwt.getToken().getClaim("pwd"))
                && !PERMITIDOS.contains(request.getRequestURI())
                && !"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            respostaProblema.escrever(response, request, HttpStatus.FORBIDDEN,
                    "TROCA_SENHA_OBRIGATORIA",
                    "Troque a senha antes de continuar.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
