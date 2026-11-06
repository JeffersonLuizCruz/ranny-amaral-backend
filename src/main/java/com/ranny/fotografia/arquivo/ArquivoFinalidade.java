package com.ranny.fotografia.arquivo;

/**
 * Finalidade de um arquivo no CRM.
 *
 * <p>PATTERN: Value Object / Enum tipado.
 *
 * <p>Distingue o que e um arquivo generico de um ativo semantico. Uma foto de
 * galeria, por exemplo, e representada por uma entidade propria que aponta para
 * um arquivo original e para arquivos derivados.
 */
public enum ArquivoFinalidade {
    COMPROVANTE_PAGAMENTO,
    FOTO_ORIGINAL,
    FOTO_DERIVADO,
    /** Versao final editada entregue ao cliente (separada do derivado tecnico). */
    FOTO_EDITADA,
    VIDEO_ORIGINAL,
    CONTRATO_PDF
}
