package com.ranny.fotografia.auth.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracao de autenticacao, sessao e bootstrap do admin. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private int accessTokenMinutes = 15;
    private int refreshTokenDays = 7;
    private String issuer = "http://localhost:8080";

    private String cookieName = "refresh_token";
    private String cookieDomain = "";
    private boolean cookieSecure = false;
    private String cookieSameSite = "Lax";

    private int maxTentativas = 5;
    private int bloqueioMinutos = 15;

    private int senhaMinima = 8;
    private int senhaMaxima = 128;

    private boolean bootstrapEnabled = false;
    private String bootstrapAdminEmail;
    private String bootstrapAdminPassword;

    private String jwtPrivateKey;
    private String jwtPublicKey;
    private String jwtKeyId = "ranny-1";
    private boolean allowEphemeralKeys = false;
}
