package com.ranny.fotografia.shared.seguranca.borda;

import org.springframework.http.HttpStatus;

/**
 * Resultado de uma validacao de borda.
 *
 * <p>PATTERN: Value Object. O validador nao conhece HTTP response; ele apenas
 * devolve o status, o codigo de erro e uma mensagem segura. O
 * {@link RespostaProblemaSeguranca} converte para RFC 7807/9457.
 */
public record ProblemaBorda(HttpStatus status, String codigo, String detalhe) {

    public static ProblemaBorda de(HttpStatus status, String codigo, String detalhe) {
        return new ProblemaBorda(status, codigo, detalhe);
    }

    public static ProblemaBorda payloadExcedido() {
        return de(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_EXCEDIDO",
                "O corpo da requisicao excede o limite permitido.");
    }

    public static ProblemaBorda headerObrigatorio(String header) {
        return de(HttpStatus.BAD_REQUEST, "HEADER_OBRIGATORIO",
                "O header %s e obrigatorio.".formatted(header));
    }

    public static ProblemaBorda headerInvalido(String header) {
        return de(HttpStatus.BAD_REQUEST, "HEADER_INVALIDO",
                "O header %s e invalido.".formatted(header));
    }

    public static ProblemaBorda contentLengthInvalido() {
        return de(HttpStatus.BAD_REQUEST, "CONTENT_LENGTH_INVALIDO",
                "O header Content-Length e invalido.");
    }

    public static ProblemaBorda contentObrigatorio() {
        return de(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "CONTENT_TYPE_OBRIGATORIO",
                "O header Content-Type e obrigatorio para requisicoes com corpo.");
    }

    public static ProblemaBorda contentInvalido() {
        return de(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "CONTENT_TYPE_INVALIDO",
                "O Content-Type deve ser application/json ou um tipo +json.");
    }

    public static ProblemaBorda padraoMalicioso() {
        return de(HttpStatus.FORBIDDEN, "PADRAO_MALICIOSO",
                "A requisicao foi bloqueada pela politica de seguranca.");
    }

    public static ProblemaBorda acessoNegado() {
        return de(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Acesso negado.");
    }
}
