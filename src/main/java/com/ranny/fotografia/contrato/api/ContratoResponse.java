package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.model.Contrato;
import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import com.ranny.fotografia.contrato.model.documento.ContratoEvento;
import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot;
import com.ranny.fotografia.contrato.model.documento.DadosAceite;
import com.ranny.fotografia.contrato.model.documento.StatusAceite;
import com.ranny.fotografia.shared.model.ComprovantePagamento;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoEntrada;
import com.ranny.fotografia.shared.model.PagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import com.ranny.fotografia.shared.model.RejeicaoComprovante;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** DTO de saida do contrato, compativel com os tipos do frontend. */
public record ContratoResponse(
        String id,
        String numero,
        String clienteId,
        String agendamentoId,
        String pacoteId,
        String modeloId,
        StatusAceite statusAceite,
        PagamentoStatus statusPagamento,
        String titulo,
        List<ContratoClausula> clausulas,
        List<ContratoClausula> clausulasModelo,
        ContratoSnapshot snapshot,
        BigDecimal valorTotal,
        BigDecimal valorEntrada,
        BigDecimal percentualEntrada,
        BigDecimal saldoRestante,
        PagamentoEntradaResponse pagamento,
        PagamentoSaldoResponse pagamentoFinal,
        PagamentoStatus statusPagamentoFinal,
        String formaPagamento,
        String condicoesPagamento,
        Instant dataEmissao,
        Instant dataValidade,
        String solicitacaoAtualId,
        DadosAceite aceite,
        List<ContratoEvento> historico,
        String observacoes,
        Instant createdAt,
        Instant updatedAt
) {

    public static ContratoResponse de(Contrato contrato) {
        return new ContratoResponse(
                contrato.getId(),
                contrato.getNumero(),
                contrato.getClienteId(),
                contrato.getAgendamentoId(),
                contrato.getPacoteId(),
                contrato.getModeloId(),
                contrato.getStatusAceite(),
                contrato.getStatusPagamento(),
                contrato.getTitulo(),
                contrato.getClausulas(),
                contrato.getClausulasModelo(),
                contrato.getSnapshot(),
                contrato.getValorTotal(),
                contrato.getValorEntrada(),
                contrato.getPercentualEntrada(),
                contrato.getSaldoRestante(),
                PagamentoEntradaResponse.de(contrato.getPagamento()),
                PagamentoSaldoResponse.de(contrato.getPagamentoFinal()),
                contrato.getStatusPagamentoFinal(),
                contrato.getFormaPagamento(),
                contrato.getCondicoesPagamento(),
                contrato.getDataEmissao(),
                contrato.getDataValidade(),
                contrato.getSolicitacaoAtualId(),
                contrato.getAceite(),
                contrato.getHistorico(),
                contrato.getObservacoes(),
                contrato.getAuditoria().getCriadoEm(),
                contrato.getAuditoria().getAtualizadoEm()
        );
    }

    public record PagamentoEntradaResponse(
            BigDecimal valor,
            BigDecimal percentual,
            PagamentoStatus status,
            ComprovantePagamento comprovante,
            RejeicaoComprovante rejeicao,
            Instant confirmadoEm,
            String confirmadoPor,
            String observacoes
    ) {

        static PagamentoEntradaResponse de(PagamentoEntrada pagamento) {
            if (pagamento == null) {
                return null;
            }
            return new PagamentoEntradaResponse(
                    pagamento.getValor(),
                    pagamento.getPercentual(),
                    pagamento.getStatus(),
                    pagamento.getComprovante(),
                    pagamento.getRejeicao(),
                    pagamento.getConfirmadoEm(),
                    pagamento.getConfirmadoPor(),
                    pagamento.getObservacoes());
        }
    }

    public record PagamentoSaldoResponse(
            BigDecimal valor,
            PagamentoStatus status,
            FormaPagamentoSaldo formaPagamento,
            Instant recebidoEm,
            String recebidoPor,
            String observacoes
    ) {

        static PagamentoSaldoResponse de(PagamentoSaldo pagamento) {
            if (pagamento == null) {
                return null;
            }
            return new PagamentoSaldoResponse(
                    pagamento.getValor(),
                    pagamento.getStatus(),
                    pagamento.getFormaPagamento(),
                    pagamento.getRecebidoEm(),
                    pagamento.getRecebidoPor(),
                    pagamento.getObservacoes());
        }
    }
}
