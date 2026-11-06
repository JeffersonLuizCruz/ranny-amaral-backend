package com.ranny.fotografia.cliente;

import com.ranny.fotografia.shared.model.TipoDocumento;

/** Dados minimos para localizar ou criar um cliente durante o aceite. */
public record ContatoCliente(
        String nome,
        String whatsapp,
        String email,
        String documento,
        TipoDocumento tipoDocumento,
        String cidade
) {
}
