package com.ranny.fotografia.shared.seguranca.borda.validador;

import com.ranny.fotografia.shared.seguranca.borda.ProblemaBorda;
import com.ranny.fotografia.shared.seguranca.borda.PropriedadesSegurancaBorda;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Exige User-Agent em requisicoes de API.
 *
 * <p>Clientes automatizados muito simples e scanners costumam omitir o header.
 * A ausencia nao e prova de ataque, por isso o status e 400 e a regra e
 * configuravel por ambiente.
 */
@Component
@Order(30)
public class ValidadorHeadersObrigatorios implements ValidadorRequisicaoBorda {

    private final PropriedadesSegurancaBorda propriedades;

    public ValidadorHeadersObrigatorios(PropriedadesSegurancaBorda propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public Optional<ProblemaBorda> validar(HttpServletRequest request) {
        if (!propriedades.isExigirUserAgent()) {
            return Optional.empty();
        }
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent == null || userAgent.isBlank()) {
            return Optional.of(ProblemaBorda.headerObrigatorio(HttpHeaders.USER_AGENT));
        }
        if (userAgent.length() > propriedades.getMaxUserAgentLength()) {
            return Optional.of(ProblemaBorda.headerInvalido(HttpHeaders.USER_AGENT));
        }
        return Optional.empty();
    }
}
