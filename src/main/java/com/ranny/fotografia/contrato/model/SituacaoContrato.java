package com.ranny.fotografia.contrato.model;

/**
 * Fila operacional do contrato usada pelo staff.
 *
 * <p>Cada valor representa o proximo passo de uma pendencia, e nao um status
 * isolado do banco. As filas sao mutuamente exclusivas para que o mesmo
 * contrato nao apareca em duas listas ao mesmo tempo.
 */
public enum SituacaoContrato {

    /** Contrato aguardando o cliente aceitar o link. */
    AGUARDANDO_ACEITE("aguardando_aceite"),

    /** Contrato ja aceito com entrada ainda nao paga. */
    ENTRADA_EM_ABERTO("entrada_em_aberto"),

    /** Comprovante de entrada enviado e aguardando aprovacao do staff. */
    COMPROVANTE_EM_ANALISE("comprovante_em_analise");

    private final String valor;

    SituacaoContrato(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static SituacaoContrato de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (SituacaoContrato situacao : values()) {
            if (situacao.valor.equalsIgnoreCase(valor)) {
                return situacao;
            }
        }
        throw new IllegalArgumentException("Situacao de contrato invalida: " + valor);
    }
}
