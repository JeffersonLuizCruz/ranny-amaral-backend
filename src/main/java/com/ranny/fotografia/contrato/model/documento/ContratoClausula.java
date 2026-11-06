package com.ranny.fotografia.contrato.model.documento;

/**
 * Clausula contratual. Documento imutavel persistido como JSON.
 *
 * <p>PATTERN: Value Object/Document. Clausulas nao sao consultadas
 * individualmente; guarda-las em tabela propria criaria joins sem retorno.
 */
public record ContratoClausula(String id, String titulo, String corpo) {
}
