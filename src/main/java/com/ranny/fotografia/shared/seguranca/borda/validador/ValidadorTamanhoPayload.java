package com.ranny.fotografia.shared.seguranca.borda.validador;

import com.ranny.fotografia.shared.seguranca.borda.ProblemaBorda;
import com.ranny.fotografia.shared.seguranca.borda.PropriedadesSegurancaBorda;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Primeira barreira contra DoS por corpo grande.
 *
 * <p>Valida o {@code Content-Length} antes de qualquer leitura. Quando o
 * tamanho e desconhecido (chunked), o {@code RequisicaoComLimiteTamanho}
 * assume a protecao byte a byte.
 */
@Component
@Order(10)
public class ValidadorTamanhoPayload implements ValidadorRequisicaoBorda {

    private final PropriedadesSegurancaBorda propriedades;

    public ValidadorTamanhoPayload(PropriedadesSegurancaBorda propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public Optional<ProblemaBorda> validar(HttpServletRequest request) {
        if (propriedades.getMaxPayloadBytes() <= 0) {
            return Optional.empty();
        }
        String header = request.getHeader(HttpHeaders.CONTENT_LENGTH);
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        long tamanho;
        try {
            tamanho = Long.parseLong(header.trim());
        } catch (NumberFormatException ex) {
            return Optional.of(ProblemaBorda.contentLengthInvalido());
        }
        if (tamanho < 0) {
            return Optional.of(ProblemaBorda.contentLengthInvalido());
        }
        if (tamanho > propriedades.getMaxPayloadBytes()) {
            return Optional.of(ProblemaBorda.payloadExcedido());
        }
        return Optional.empty();
    }
}
