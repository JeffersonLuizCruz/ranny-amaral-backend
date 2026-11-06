package com.ranny.fotografia.galeria.model;

/**
 * Origem de um derivado de midia.
 *
 * <p>PATTERN: Value Object / Enum tipado.
 *
 * <p>Um derivado pode ser gerado a partir do arquivo original ou da versao
 * editada. A entrega prioriza os derivados editados; o estudio ainda consegue
 * consultar o original.
 */
public enum MidiaDerivadoOrigem {
    ORIGINAL,
    EDITADA
}
