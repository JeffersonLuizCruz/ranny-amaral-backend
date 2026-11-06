package com.ranny.fotografia.shared.exception;

import org.springframework.http.HttpStatus;

/** Arquivo acima do limite permitido — responde 413. */
public class ArquivoMuitoGrandeException extends ApiException {

    public ArquivoMuitoGrandeException(String mensagem) {
        super(HttpStatus.PAYLOAD_TOO_LARGE, "ARQUIVO_MUITO_GRANDE", mensagem);
    }
}
