package com.ranny.fotografia.auth.security;

import com.ranny.fotografia.auth.model.Usuario;
import com.ranny.fotografia.auth.repository.UsuarioRepository;
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
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valida que o usuario do JWT continua ativo e que a versao do token confere.
 *
 * <p>Isso permite revogar todas as sessoes imediatamente apos um reset de senha
 * ou desativacao. Um cache curto evita uma consulta ao banco em cada request.
 */
@Component
public class ContaAtivaFilter extends OncePerRequestFilter {

    private static final long CACHE_SEGUNDOS = 30;

    private final UsuarioRepository usuarioRepository;
    private final RespostaProblemaSeguranca respostaProblema;
    private final Map<String, CacheUsuario> cache = new ConcurrentHashMap<>();

    public ContaAtivaFilter(UsuarioRepository usuarioRepository,
                            RespostaProblemaSeguranca respostaProblema) {
        this.usuarioRepository = usuarioRepository;
        this.respostaProblema = respostaProblema;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken jwt) {
            String usuarioId = jwt.getToken().getSubject();
            Number versaoClaim = jwt.getToken().getClaim("ver");
            long versao = versaoClaim == null ? -1 : versaoClaim.longValue();
            CacheUsuario emCache = cache.get(usuarioId);
            Instant agora = Instant.now();

            boolean valido = emCache != null
                    && emCache.expiraEm().isAfter(agora)
                    && emCache.ativo()
                    && emCache.tokenVersion() == versao;

            if (!valido) {
                Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
                if (usuario == null || !usuario.isAtivo()
                        || versao < 0 || usuario.getTokenVersion() != versao) {
                    SecurityContextHolder.clearContext();
                    respostaProblema.escrever(response, request, HttpStatus.UNAUTHORIZED,
                            "SESSAO_INVALIDA",
                            "A sessao expirou ou foi revogada. Entre novamente.");
                    return;
                }
                cache.put(usuarioId, new CacheUsuario(usuario.getTokenVersion(), true,
                        agora.plusSeconds(CACHE_SEGUNDOS)));
            }
        }
        filterChain.doFilter(request, response);
    }

    private record CacheUsuario(int tokenVersion, boolean ativo, Instant expiraEm) {
    }
}
