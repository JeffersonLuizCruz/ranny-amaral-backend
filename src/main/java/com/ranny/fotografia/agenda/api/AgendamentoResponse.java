package com.ranny.fotografia.agenda.api;

import com.ranny.fotografia.agenda.model.Agendamento;
import com.ranny.fotografia.agenda.model.CondicoesPacote;
import com.ranny.fotografia.agenda.model.ContatoReserva;
import com.ranny.fotografia.agenda.model.PromocaoAplicada;
import com.ranny.fotografia.agenda.model.StatusAgendamento;
import com.ranny.fotografia.shared.model.ComprovantePagamento;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoEntrada;
import com.ranny.fotografia.shared.model.PagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import com.ranny.fotografia.shared.model.RejeicaoComprovante;
import com.ranny.fotografia.shared.financeiro.ResumoPagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO de saida do agendamento.
 *
 * <p>Os nomes e a forma dos pagamentos seguem o contrato do frontend
 * ({@code pagamentoEntrada}, {@code pagamentoFinal}, {@code condicoesPacote}).
 * PATTERN: Factory Method ({@code de}) para manter o mapeamento fora do
 * controller.
 */
public record AgendamentoResponse(
        String id,
        String clienteId,
        ContatoResponse contato,
        String contratoId,
        String pacoteId,
        OffsetDateTime dataHoraInicio,
        OffsetDateTime dataHoraFim,
        String local,
        StatusAgendamento status,
        String observacoes,
        BigDecimal valorAcordado,
        BigDecimal valorEntrada,
        BigDecimal percentualEntrada,
        BigDecimal saldoRestante,
        PagamentoEntradaResponse pagamentoEntrada,
        PagamentoSaldoResponse pagamentoFinal,
        CondicoesPacoteResponse condicoesPacote,
        String galeriaId,
        Instant createdAt,
        Instant updatedAt,
        PagamentoStatus statusEntrada,
        PagamentoStatus statusSaldoFinal,
        boolean entradaQuitada,
        boolean saldoQuitado,
        boolean quitado,
        BigDecimal valorRecebido,
        BigDecimal valorPendente
) {

    public static AgendamentoResponse de(Agendamento agendamento) {
        ResumoPagamento resumo = ResumoPagamento.de(
                agendamento.getPagamentoEntrada(),
                agendamento.getPagamentoFinal(),
                agendamento.getValorEntrada(),
                agendamento.getSaldoRestante());

        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getClienteId(),
                ContatoResponse.de(agendamento.getContato()),
                agendamento.getContratoId(),
                agendamento.getPacoteId(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getLocal(),
                agendamento.getStatus(),
                agendamento.getObservacoes(),
                agendamento.getValorAcordado(),
                agendamento.getValorEntrada(),
                agendamento.getPercentualEntrada(),
                agendamento.getSaldoRestante(),
                PagamentoEntradaResponse.de(agendamento.getPagamentoEntrada()),
                PagamentoSaldoResponse.de(agendamento.getPagamentoFinal()),
                CondicoesPacoteResponse.de(agendamento.getCondicoesPacote()),
                agendamento.getGaleriaId(),
                agendamento.getAuditoria().getCriadoEm(),
                agendamento.getAuditoria().getAtualizadoEm(),
                resumo.statusEntrada(),
                resumo.statusSaldoFinal(),
                resumo.entradaQuitada(),
                resumo.saldoQuitado(),
                resumo.quitado(),
                resumo.valorRecebido(),
                resumo.valorPendente()
        );
    }

    public record ContatoResponse(String nome, String whatsapp) {

        static ContatoResponse de(ContatoReserva contato) {
            return contato == null ? null : new ContatoResponse(contato.getNome(), contato.getWhatsapp());
        }
    }

    public record PagamentoEntradaResponse(
            BigDecimal valor,
            BigDecimal percentual,
            PagamentoStatus status,
            String arquivoId,
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
                    pagamento.getArquivoId(),
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

    public record CondicoesPacoteResponse(
            int quantidadeFotosInclusas,
            int quantidadeVideosInclusos,
            BigDecimal valorFotoExtra,
            BigDecimal valorTabela,
            List<FaixaFotoExtraResponse> faixasFotoExtra,
            PromocaoAplicadaResponse promocaoAplicada
    ) {

        static CondicoesPacoteResponse de(CondicoesPacote condicoes) {
            if (condicoes == null) {
                return null;
            }
            return new CondicoesPacoteResponse(
                    condicoes.getQuantidadeFotosInclusas(),
                    condicoes.getQuantidadeVideosInclusos(),
                    condicoes.getValorFotoExtra(),
                    condicoes.getValorTabela(),
                    condicoes.getFaixasFotoExtra().stream()
                            .map(FaixaFotoExtraResponse::de)
                            .toList(),
                    PromocaoAplicadaResponse.de(condicoes.getPromocaoAplicada()));
        }
    }

    public record FaixaFotoExtraResponse(int quantidadeMinima, BigDecimal valorUnitario) {

        static FaixaFotoExtraResponse de(
                com.ranny.fotografia.shared.model.FaixaFotoExtraSnapshot faixa) {
            return new FaixaFotoExtraResponse(
                    faixa.quantidadeMinima(),
                    faixa.valorUnitario());
        }
    }

    public record PromocaoAplicadaResponse(BigDecimal valorPromocional, String rotulo) {

        static PromocaoAplicadaResponse de(PromocaoAplicada promocao) {
            return promocao == null ? null
                    : new PromocaoAplicadaResponse(promocao.getValorPromocional(), promocao.getRotulo());
        }
    }
}
