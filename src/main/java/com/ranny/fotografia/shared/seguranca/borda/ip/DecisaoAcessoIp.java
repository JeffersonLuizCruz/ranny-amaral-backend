package com.ranny.fotografia.shared.seguranca.borda.ip;

/**
 * Decisao imutavel de acesso.
 *
 * <p>PATTERN: Value Object. O motivo fica disponivel para log interno, mas
 * nunca e devolvido ao cliente.
 */
public record DecisaoAcessoIp(boolean permitido, String motivo) {

    public static DecisaoAcessoIp liberado() {
        return new DecisaoAcessoIp(true, null);
    }

    public static DecisaoAcessoIp bloqueado(String motivo) {
        return new DecisaoAcessoIp(false, motivo);
    }
}
