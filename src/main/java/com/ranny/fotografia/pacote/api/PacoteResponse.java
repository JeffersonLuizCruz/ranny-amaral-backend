package com.ranny.fotografia.pacote.api;

import com.ranny.fotografia.pacote.model.Pacote;
import com.ranny.fotografia.pacote.model.PromocaoPacote;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** DTO de saida do pacote, com os mesmos nomes consumidos pelo frontend. */
public record PacoteResponse(
        String id,
        String nome,
        int duracaoMinutos,
        BigDecimal valor,
        String descricao,
        String corHex,
        List<String> itensInclusos,
        boolean ativo,
        int quantidadeFotosInclusas,
        int quantidadeVideosInclusos,
        BigDecimal valorFotoExtra,
        List<FaixaFotoExtraResponse> faixasFotoExtra,
        PromocaoResponse promocao,
        Instant createdAt,
        Instant updatedAt
) {

    public static PacoteResponse de(Pacote pacote) {
        return new PacoteResponse(
                pacote.getId(),
                pacote.getNome(),
                pacote.getDuracaoMinutos(),
                pacote.getValor(),
                pacote.getDescricao(),
                pacote.getCorHex(),
                pacote.getItensInclusos(),
                pacote.isAtivo(),
                pacote.getQuantidadeFotosInclusas(),
                pacote.getQuantidadeVideosInclusos(),
                pacote.getValorFotoExtra(),
                pacote.getFaixasFotoExtra().stream()
                        .map(FaixaFotoExtraResponse::de)
                        .toList(),
                PromocaoResponse.de(pacote.getPromocao()),
                pacote.getAuditoria().getCriadoEm(),
                pacote.getAuditoria().getAtualizadoEm()
        );
    }

    public record FaixaFotoExtraResponse(int quantidadeMinima, BigDecimal valorUnitario) {

        static FaixaFotoExtraResponse de(com.ranny.fotografia.pacote.model.FaixaFotoExtra faixa) {
            return new FaixaFotoExtraResponse(
                    faixa.getQuantidadeMinima(),
                    faixa.getValorUnitario());
        }
    }

    public record PromocaoResponse(
            boolean ativa,
            BigDecimal valorPromocional,
            LocalDate validaAte,
            String rotulo
    ) {

        public static PromocaoResponse de(PromocaoPacote promocao) {
            if (promocao == null) {
                return null;
            }
            return new PromocaoResponse(
                    promocao.estaAtiva(),
                    promocao.getValorPromocional(),
                    promocao.getValidaAte(),
                    promocao.getRotulo());
        }
    }
}
