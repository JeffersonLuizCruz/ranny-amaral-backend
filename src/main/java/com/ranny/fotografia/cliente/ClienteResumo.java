package com.ranny.fotografia.cliente;

import com.ranny.fotografia.shared.model.TipoDocumento;

/** Projecao publica e imutavel de um cliente para outros modulos. */
public record ClienteResumo(
        String id,
        String nome,
        String whatsapp,
        String email,
        String documento,
        TipoDocumento tipoDocumento,
        String cidade
) {
}
