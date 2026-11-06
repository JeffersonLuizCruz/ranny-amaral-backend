package com.ranny.fotografia.contrato.model.documento;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Tipos de evento do historico do contrato (contrato com o frontend). */
public enum ContratoEventoTipo {
    CRIADO("criado"),
    ATUALIZADO("atualizado"),
    LINK_GERADO("link_gerado"),
    LINK_VISUALIZADO("link_visualizado"),
    ACEITE_REGISTRADO("aceite_registrado"),
    CLIENTE_CRIADO("cliente_criado"),
    CLIENTE_VINCULADO("cliente_vinculado"),
    COMPROVANTE_ENVIADO("comprovante_enviado"),
    PAGAMENTO_CONFIRMADO("pagamento_confirmado"),
    PAGAMENTO_REJEITADO("pagamento_rejeitado"),
    SALDO_RECEBIDO("saldo_recebido"),
    SALDO_ISENTO("saldo_isento"),
    LINK_EXPIRADO("link_expirado"),
    LINK_CANCELADO("link_cancelado"),
    CONTRATO_CANCELADO("contrato_cancelado"),
    REABERTO("reaberto"),
    PDF_GERADO("pdf_gerado"),
    DUPLICADO("duplicado");

    private final String valor;

    ContratoEventoTipo(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static ContratoEventoTipo de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        for (ContratoEventoTipo tipo : values()) {
            if (tipo.valor.equalsIgnoreCase(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de evento de contrato invalido: " + valor);
    }
}
