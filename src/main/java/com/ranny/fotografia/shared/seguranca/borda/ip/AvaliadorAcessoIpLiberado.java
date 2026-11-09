package com.ranny.fotografia.shared.seguranca.borda.ip;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Implementacao default que libera todo trafego.
 *
 * <p>PATTERN: Null Object. Evita {@code null} e {@code if} espalhados no
 * filtro. Quando um adaptador Redis for criado, ele sera injetado no lugar
 * desta estrategia.
 */
public class AvaliadorAcessoIpLiberado implements AvaliadorAcessoIp {

    @Override
    public DecisaoAcessoIp avaliar(String ip, HttpServletRequest request) {
        return DecisaoAcessoIp.liberado();
    }
}
