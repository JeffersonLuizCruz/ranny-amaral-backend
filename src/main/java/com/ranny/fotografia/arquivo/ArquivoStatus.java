package com.ranny.fotografia.arquivo;

/** Estado do ciclo de vida de um arquivo no storage. */
public enum ArquivoStatus {
    PENDENTE,
    ENVIANDO,
    PROCESSANDO,
    DISPONIVEL,
    FALHA,
    CANCELADO
}
