package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Fabrica de respostas RFC 7807/9457.
 *
 * <p>PATTERN: Factory Method. Centraliza o formato do Problem Details para que
 * filtros, handlers de seguranca e o {@link GlobalExceptionHandler} nao
 * repliquem a mesma montagem.
 *
 * <p>Os campos {@code codigo}, {@code mensagem}, {@code caminho} e
 * {@code campos} sao extensoes de compatibilidade com o contrato antigo da API.
 * O formato padrao continua sendo {@code type}, {@code title}, {@code status},
 * {@code detail} e {@code instance}.
 */
public final class Problemas {

    private static final String BASE_TYPE = "urn:ranny:fotografia:problema:";

    private Problemas() {
    }

    public static ProblemDetail de(HttpStatus status, String codigo, String detalhe, String instance) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(status.getReasonPhrase());
        problema.setType(URI.create(BASE_TYPE + slug(codigo)));
        problema.setInstance(uri(instance));
        problema.setProperty("codigo", codigo);
        problema.setProperty("mensagem", detalhe);
        problema.setProperty("caminho", instance);
        problema.setProperty("timestamp", Instant.now());
        return problema;
    }

    public static void adicionarCampos(ProblemDetail problema, List<CampoErro> campos) {
        if (campos != null && !campos.isEmpty()) {
            problema.setProperty("campos", List.copyOf(campos));
        }
    }

    private static String slug(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "erro";
        }
        return codigo.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static URI uri(String valor) {
        if (valor == null || valor.isBlank()) {
            return URI.create("/");
        }
        try {
            return URI.create(valor);
        } catch (IllegalArgumentException ex) {
            return URI.create("/");
        }
    }
}
