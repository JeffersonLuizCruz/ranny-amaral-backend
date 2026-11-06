package com.ranny.fotografia.cliente;

import com.ranny.fotografia.shared.model.TipoDocumento;

import java.util.Optional;

/**
 * PATTERN: Public API / Port do modulo {@code cliente} para o Spring Modulith.
 *
 * <p>Outros modulos (contrato, agenda) precisam consultar ou criar clientes, mas
 * nao podem acessar {@code cliente.model} nem {@code cliente.repository}. O
 * Spring Modulith permite apenas tipos no pacote raiz do modulo; esta interface
 * e o {@link ClienteResumo} formam o contrato publico.
 *
 * <p>Beneficios: o modelo interno pode evoluir sem quebrar consumidores, e a
 * verificacao de fronteiras do Modulith continua verde.
 */
public interface ClienteConsulta {

    Optional<ClienteResumo> buscar(String clienteId);

    /**
     * Reaproveita o cliente existente (mesmo documento ou WhatsApp) ou cria um
     * novo com origem {@code contrato}. Usado no aceite publico, quando o
     * cliente ainda nao existe no CRM.
     */
    ClienteResumo obterOuCriar(ContatoCliente contato);
}
