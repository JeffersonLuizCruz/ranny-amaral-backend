package com.ranny.fotografia.shared.ports;

/**
 * PATTERN: Dependency Inversion Principle (Port) + Strategy.
 *
 * <p>Mesma motivacao de {@link VerificadorVinculoCliente}: o modulo
 * {@code pacote} consulta vinculos sem importar o modulo {@code agenda}. O
 * {@code agenda} registra a implementacao e o Spring injeta a lista de
 * verificadores.
 */
public interface VerificadorVinculoPacote {

    boolean possuiVinculo(String pacoteId);
}
