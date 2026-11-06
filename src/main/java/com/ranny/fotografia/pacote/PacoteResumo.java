package com.ranny.fotografia.pacote;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Projecao publica e imutavel de um pacote.
 *
 * <p>PATTERN: Value Object com comportamento ({@link #promocaoAplicada(Instant)}
 * e {@link #valorEfetivo(Instant)}).
 *
 * <p>Concentrar a regra de promocao no value object garante que agenda,
 * contrato e futuras galerias apliquem exatamente o mesmo criterio (promocao
 * ativa e dentro da validade), evitando duplicacao de {@code if}s.
 */
public record PacoteResumo(
        String id,
        String nome,
        int duracaoMinutos,
        BigDecimal valor,
        List<String> itensInclusos,
        int quantidadeFotosInclusas,
        int quantidadeVideosInclusos,
        BigDecimal valorFotoExtra,
        PromocaoResumo promocao,
        boolean ativo
) {

    public PacoteResumo {
        itensInclusos = itensInclusos == null ? List.of() : List.copyOf(itensInclusos);
    }

    public Optional<PromocaoResumo> promocaoAplicada(Instant referencia) {
        if (promocao == null || !promocao.ativa()) {
            return Optional.empty();
        }
        LocalDate hoje = LocalDate.ofInstant(referencia, ZoneId.of("America/Sao_Paulo"));
        if (promocao.validaAte() != null && promocao.validaAte().isBefore(hoje)) {
            return Optional.empty();
        }
        return Optional.of(promocao);
    }

    public BigDecimal valorEfetivo(Instant referencia) {
        return promocaoAplicada(referencia)
                .map(PromocaoResumo::valorPromocional)
                .orElse(valor);
    }

    /** Promocao comercial congelada no snapshot do contrato/agendamento. */
    public record PromocaoResumo(
            boolean ativa,
            BigDecimal valorPromocional,
            LocalDate validaAte,
            String rotulo
    ) {
    }
}
