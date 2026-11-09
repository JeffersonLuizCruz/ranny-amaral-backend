package com.ranny.fotografia.shared.seguranca.borda.ip;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Ponto de extensao para bloqueio/rate limiting por IP.
 *
 * <p>PATTERN: Strategy. O filtro nao conhece Redis, cache ou banco: apenas
 * consulta esta porta. A implementacao default e permissiva; um adaptador
 * Redis pode substitui-la sem alterar a cadeia de filtros.
 */
public interface AvaliadorAcessoIp {

    DecisaoAcessoIp avaliar(String ip, HttpServletRequest request);
}
