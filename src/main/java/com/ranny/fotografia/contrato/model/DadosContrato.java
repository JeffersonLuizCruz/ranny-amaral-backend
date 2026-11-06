package com.ranny.fotografia.contrato.model;

import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * PATTERN: Parameter Object.
 *
 * <p>Criar/atualizar um contrato envolve muitos dados comerciais e documentos.
 * Agrupar tudo em um record imutavel evita metodos com 18 parametros e deixa o
 * service legivel. O mesmo objeto serve a criacao e a edicao (os campos id e
 * numero so sao usados na criacao/seed).
 */
public record DadosContrato(
        String id,
        String numero,
        String clienteId,
        String agendamentoId,
        String pacoteId,
        String modeloId,
        String titulo,
        List<ContratoClausula> clausulas,
        List<ContratoClausula> clausulasModelo,
        ContratoSnapshot snapshot,
        BigDecimal valorTotal,
        BigDecimal valorEntrada,
        BigDecimal percentualEntrada,
        String formaPagamento,
        String condicoesPagamento,
        Instant dataEmissao,
        Instant dataValidade,
        String observacoes
) {
}
