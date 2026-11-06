package com.ranny.fotografia.shared.ports;

/**
 * PATTERN: Dependency Inversion Principle (Port) + Strategy.
 *
 * <p>O modulo {@code cliente} precisa impedir a exclusao de clientes com
 * agendamentos vinculados, mas nao pode depender de {@code agenda} (isso
 * criaria um ciclo, pois {@code agenda} depende de {@code cliente}). A porta
 * fica no Shared Kernel; o {@code agenda} fornece uma implementacao e o Spring
 * injeta todas as implementacoes encontradas.
 *
 * <p>Beneficios: o dominio de cliente nao conhece o modulo de agenda, novos
 * vinculos (galeria, financeiro) podem implementar a porta, e os testes podem
 * substituir a verificacao por um stub.
 */
public interface VerificadorVinculoCliente {

    boolean possuiVinculo(String clienteId);
}
