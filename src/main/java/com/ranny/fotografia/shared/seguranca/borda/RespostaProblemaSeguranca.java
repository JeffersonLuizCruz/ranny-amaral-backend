package com.ranny.fotografia.shared.seguranca.borda;

import com.ranny.fotografia.shared.exception.Problemas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escreve respostas de seguranca no formato RFC 7807/9457.
 *
 * <p>PATTERN: Adapter entre o mundo de servlet e o contrato Problem Details.
 *
 * <p>O filtro roda antes do MVC, portanto nao pode contar com
 * {@code @RestControllerAdvice} para formatar a resposta. Este componente
 * garante o mesmo contrato para bloqueios de borda e handlers de seguranca.
 */
@Component
public class RespostaProblemaSeguranca {

    private static final Logger log = LoggerFactory.getLogger(RespostaProblemaSeguranca.class);

    private final ObjectMapper objectMapper;

    public RespostaProblemaSeguranca(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void escrever(HttpServletResponse response,
                         HttpServletRequest request,
                         ProblemaBorda problema) {
        escrever(response, request, problema.status(), problema.codigo(), problema.detalhe());
    }

    public void escrever(HttpServletResponse response,
                         HttpServletRequest request,
                         HttpStatus status,
                         String codigo,
                         String detalhe) {
        if (response.isCommitted()) {
            log.warn("Resposta ja commitada; nao foi possivel escrever problema {} em {}",
                    codigo, request.getRequestURI());
            return;
        }

        ProblemDetail problema = Problemas.de(status, codigo, detalhe, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        try {
            objectMapper.writeValue(response.getWriter(), problema);
            response.flushBuffer();
        } catch (IOException | RuntimeException ex) {
            log.error("Falha ao escrever resposta de seguranca em {}", request.getRequestURI(), ex);
            if (!response.isCommitted()) {
                try {
                    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
                } catch (IOException falhaEnvio) {
                    log.error("Falha ao enviar erro de fallback", falhaEnvio);
                }
            }
        }
    }
}
