package com.ranny.fotografia.pacote;

import java.util.Optional;

/**
 * PATTERN: Public API / Port do modulo {@code pacote}.
 *
 * <p>O modulo {@code agenda} precisa conhecer preco, duracao, quantidades e
 * promocao para congelar as condicoes comerciais no momento da venda. Ele faz
 * isso por meio deste contrato publico, sem importar entidades ou repositorios
 * de pacote.
 */
public interface PacoteConsulta {

    Optional<PacoteResumo> buscar(String pacoteId);
}
