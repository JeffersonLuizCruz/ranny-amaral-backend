package com.ranny.fotografia.auth.security;

import com.ranny.fotografia.shared.seguranca.borda.RespostaProblemaSeguranca;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valida o cabecalho Origin nos endpoints que usam cookie de refresh.
 *
 * <p>O CSRF esta desabilitado porque a API e stateless e usa Authorization
 * header. Refresh e logout, porem, dependem do cookie HttpOnly; exigir uma
 * origem conhecida reduz o risco de CSRF sem quebrar chamadas server-to-server
 * (que nao enviam Origin).
 */
@Component
public class AuthOriginFilter extends OncePerRequestFilter {

    private final Set<String> origensPermitidas;
    private final RespostaProblemaSeguranca respostaProblema;

    public AuthOriginFilter(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String origens,
            RespostaProblemaSeguranca respostaProblema) {
        this.origensPermitidas = Arrays.stream(origens.split("\\s*,\\s*"))
                .filter(valor -> !valor.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.respostaProblema = respostaProblema;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return !("/api/v1/auth/refresh".equals(caminho)
                || "/api/v1/auth/logout".equals(caminho));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origem = request.getHeader("Origin");
        if (origem != null && !origem.isBlank() && !origensPermitidas.contains(origem)) {
            respostaProblema.escrever(response, request, HttpStatus.FORBIDDEN,
                    "ORIGEM_NAO_PERMITIDA", "Origem nao permitida.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
