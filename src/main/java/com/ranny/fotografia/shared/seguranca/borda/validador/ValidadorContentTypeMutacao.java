package com.ranny.fotografia.shared.seguranca.borda.validador;

import com.ranny.fotografia.shared.seguranca.borda.ProblemaBorda;
import com.ranny.fotografia.shared.seguranca.borda.PropriedadesSegurancaBorda;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Valida o Content-Type de operacoes mutantes com corpo.
 *
 * <p>A API aceita JSON e variantes {@code +json} (JSON Patch, Problem Details,
 * etc.). Requisicoes sem corpo (ex.: confirmar pagamento) passam sem
 * Content-Type. O filtro nao faz parsing do body; isso continua no MVC.
 */
@Component
@Order(40)
public class ValidadorContentTypeMutacao implements ValidadorRequisicaoBorda {

    private static final Set<String> METODOS_COM_CORPO = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final PropriedadesSegurancaBorda propriedades;

    public ValidadorContentTypeMutacao(PropriedadesSegurancaBorda propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public Optional<ProblemaBorda> validar(HttpServletRequest request) {
        if (!propriedades.isExigirContentTypeEmMutacoes()) {
            return Optional.empty();
        }
        if (!METODOS_COM_CORPO.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            return Optional.empty();
        }
        if (!temCorpo(request)) {
            return Optional.empty();
        }

        String contentType = request.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return Optional.of(ProblemaBorda.contentObrigatorio());
        }
        try {
            MediaType tipo = MediaType.parseMediaType(contentType);
            if (MediaType.APPLICATION_JSON.isCompatibleWith(tipo)
                    || tipo.getSubtype() != null && tipo.getSubtype().endsWith("+json")) {
                return Optional.empty();
            }
        } catch (InvalidMediaTypeException ex) {
            return Optional.of(ProblemaBorda.contentInvalido());
        }
        return Optional.of(ProblemaBorda.contentInvalido());
    }

    private boolean temCorpo(HttpServletRequest request) {
        if (request.getContentLengthLong() > 0) {
            return true;
        }
        String transferEncoding = request.getHeader(HttpHeaders.TRANSFER_ENCODING);
        return transferEncoding != null
                && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked");
    }
}
