package com.ranny.fotografia.shared.events;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Evento publicado quando um lote de midias fica disponivel para download.
 *
 * <p>As chaves sao curtas de proposito: o outbox do Spring Modulith usa
 * {@code serialized_event varchar(255)}. Nome e WhatsApp do cliente sao
 * resolvidos pelo listener via {@code ClienteConsulta}, mantendo o payload
 * compacto.
 */
public record LoteLiberadoEvent(
        @JsonProperty("g") String galeriaId,
        @JsonProperty("c") String clienteId,
        @JsonProperty("t") String tokenLink,
        @JsonProperty("q") int quantidadeLiberada,
        @JsonProperty("l") int totalLiberado,
        @JsonProperty("s") int totalSelecionado
) {
}
