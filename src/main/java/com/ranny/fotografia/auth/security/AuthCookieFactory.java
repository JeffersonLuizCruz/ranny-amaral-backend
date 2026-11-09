package com.ranny.fotografia.auth.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Monta e limpa o cookie HttpOnly do refresh token. */
@Component
public class AuthCookieFactory {

    private final AuthProperties properties;

    public AuthCookieFactory(AuthProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie criar(String valor) {
        return base(valor)
                .maxAge(Duration.ofDays(properties.getRefreshTokenDays()))
                .build();
    }

    public ResponseCookie limpar() {
        return base("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.getCookieName(), valor)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path("/api/v1/auth");
        if (properties.getCookieDomain() != null && !properties.getCookieDomain().isBlank()) {
            builder.domain(properties.getCookieDomain());
        }
        return builder;
    }
}
