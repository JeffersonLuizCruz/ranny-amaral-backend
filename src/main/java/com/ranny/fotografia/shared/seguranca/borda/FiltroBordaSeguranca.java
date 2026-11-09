package com.ranny.fotografia.shared.seguranca.borda;

import com.ranny.fotografia.shared.seguranca.borda.validador.ValidadorRequisicaoBorda;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro de borda executado no inicio da cadeia do Spring Security.
 *
 * <p>PATTERN: Filter Chain + Chain of Responsibility.
 *
 * <p><b>Motivo:</b> requisicoes maliciosas ou desproporcionais devem ser
 * rejeitadas antes de chegar a Controllers, Services, JPA ou object storage.
 * Cada validacao e um elo independente ({@link ValidadorRequisicaoBorda}),
 * executado em ordem. O filtro apenas orquestra.
 *
 * <p><b>Beneficios:</b>
 * <ul>
 *   <li>Controllers e Services ficam livres de checagens de infraestrutura.</li>
 *   <li>Novas regras (rate limiting, geo blocking, HMAC) entram como novos
 *       beans, sem alterar esta classe (Open/Closed).</li>
 *   <li>O corpo e limitado por decorator, sem bufferizar o payload inteiro.</li>
 *   <li>Respostas seguem Problem Details, inclusive antes do MVC.</li>
 * </ul>
 *
 * <p>Observacao: a heranca de {@link OncePerRequestFilter} e uma exigencia do
 * framework, nao heranca de dominio. Todo comportamento variavel e composto.
 */
public class FiltroBordaSeguranca extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroBordaSeguranca.class);

    private final PropriedadesSegurancaBorda propriedades;
    private final List<ValidadorRequisicaoBorda> validadores;
    private final RespostaProblemaSeguranca respostaProblema;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public FiltroBordaSeguranca(PropriedadesSegurancaBorda propriedades,
                                List<ValidadorRequisicaoBorda> validadores,
                                RespostaProblemaSeguranca respostaProblema) {
        this.propriedades = propriedades;
        this.validadores = List.copyOf(validadores);
        this.respostaProblema = respostaProblema;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!propriedades.isHabilitado()) {
            return true;
        }
        if (DispatcherType.ERROR.equals(request.getDispatcherType())) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String caminho = request.getRequestURI();
        return propriedades.getCaminhosIgnorados().stream()
                .anyMatch(padrao -> pathMatcher.match(padrao, caminho));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        for (ValidadorRequisicaoBorda validador : validadores) {
            Optional<ProblemaBorda> problema = validador.validar(request);
            if (problema.isPresent()) {
                respostaProblema.escrever(response, request, problema.get());
                return;
            }
        }

        HttpServletRequest requisicao = propriedades.getMaxPayloadBytes() > 0
                ? new RequisicaoComLimiteTamanho(request, propriedades.getMaxPayloadBytes())
                : request;

        try {
            filterChain.doFilter(requisicao, response);
        } catch (LimitePayloadExcedidoException ex) {
            if (!response.isCommitted()) {
                respostaProblema.escrever(response, request, ProblemaBorda.payloadExcedido());
            } else {
                log.warn("Limite de payload excedido apos commit da resposta em {}",
                        request.getRequestURI());
                throw ex;
            }
        }
    }
}
