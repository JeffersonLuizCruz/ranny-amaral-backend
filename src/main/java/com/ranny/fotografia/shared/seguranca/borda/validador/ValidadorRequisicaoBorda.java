package com.ranny.fotografia.shared.seguranca.borda.validador;

import com.ranny.fotografia.shared.seguranca.borda.ProblemaBorda;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Contrato de uma regra de borda.
 *
 * <p>PATTERN: Strategy + Chain of Responsibility. Cada implementacao e um elo
 * independente; o filtro executa a lista na ordem definida por {@code @Order}.
 * Novas regras (rate limiting, geo blocking, assinatura HMAC) entram como novos
 * beans, sem alterar o filtro.
 */
public interface ValidadorRequisicaoBorda {

    Optional<ProblemaBorda> validar(HttpServletRequest request);
}
