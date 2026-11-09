package com.ranny.fotografia.shared.seguranca.borda;

/**
 * Sinaliza que o corpo lido ultrapassou o limite configurado.
 *
 * <p>E uma excecao de runtime para atravessar a API de {@code InputStream} sem
 * obrigar todos os controllers a declarar {@code throws IOException}.
 */
public class LimitePayloadExcedidoException extends RuntimeException {

    private final long limiteBytes;

    public LimitePayloadExcedidoException(long limiteBytes) {
        super("O corpo da requisicao excedeu o limite de %d bytes.".formatted(limiteBytes));
        this.limiteBytes = limiteBytes;
    }

    public long getLimiteBytes() {
        return limiteBytes;
    }
}
