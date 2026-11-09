package com.ranny.fotografia.shared.seguranca.borda.validador;

import com.ranny.fotografia.shared.seguranca.borda.ProblemaBorda;
import com.ranny.fotografia.shared.seguranca.borda.PropriedadesSegurancaBorda;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Bloqueia assinaturas obvias de scanning.
 *
 * <p>A regra e deliberadamente conservadora: nao tenta ser um WAF completo.
 * Ela cobre User-Agents de ferramentas conhecidas, path traversal e arquivos
 * sensiveis comuns. A query string e checada apenas para traversal/null byte
 * para nao quebrar buscas textuais legitimas.
 */
@Component
@Order(50)
public class ValidadorPadroesScanning implements ValidadorRequisicaoBorda {

    private static final Logger log = LoggerFactory.getLogger(ValidadorPadroesScanning.class);

    private final PropriedadesSegurancaBorda propriedades;

    public ValidadorPadroesScanning(PropriedadesSegurancaBorda propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public Optional<ProblemaBorda> validar(HttpServletRequest request) {
        if (!propriedades.isBloquearPadroesScanning()) {
            return Optional.empty();
        }

        Optional<ProblemaBorda> porUserAgent = validarUserAgent(request);
        if (porUserAgent.isPresent()) {
            return porUserAgent;
        }

        String caminho = normalizar(request.getRequestURI());
        for (String padrao : propriedades.getPadroesCaminhoSuspeito()) {
            if (contem(caminho, padrao)) {
                log.warn("Padrao suspeito '{}' no caminho {}", padrao, request.getRequestURI());
                return Optional.of(ProblemaBorda.padraoMalicioso());
            }
        }

        String query = normalizar(request.getQueryString());
        for (String padrao : propriedades.getPadroesQuerySuspeitos()) {
            if (contem(query, padrao)) {
                log.warn("Padrao suspeito '{}' na query de {}", padrao, request.getRequestURI());
                return Optional.of(ProblemaBorda.padraoMalicioso());
            }
        }
        return Optional.empty();
    }

    private Optional<ProblemaBorda> validarUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent == null || userAgent.isBlank()) {
            return Optional.empty();
        }
        String normalizado = userAgent.toLowerCase(Locale.ROOT);
        for (String scanner : propriedades.getUserAgentsScanners()) {
            if (contem(normalizado, scanner)) {
                log.warn("User-Agent de scanner '{}' bloqueado em {}",
                        scanner, request.getRequestURI());
                return Optional.of(ProblemaBorda.padraoMalicioso());
            }
        }
        return Optional.empty();
    }

    private boolean contem(String valor, String padrao) {
        if (valor == null || padrao == null || padrao.isBlank()) {
            return false;
        }
        return valor.contains(padrao.trim().toLowerCase(Locale.ROOT));
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        try {
            return URLDecoder.decode(valor, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return valor.toLowerCase(Locale.ROOT);
        }
    }
}
