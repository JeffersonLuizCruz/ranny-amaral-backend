package com.ranny.fotografia.auth.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Chaves e beans de JWT.
 *
 * <p>Em producao as chaves vem de {@code APP_AUTH_JWT_PRIVATE_KEY} e
 * {@code APP_AUTH_JWT_PUBLIC_KEY} (PEM em base64). Em dev/teste, se ausentes,
 * um par efemero e gerado para nao travar o ambiente local; todas as sessoes
 * caem quando a aplicacao reinicia.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    public record JwtKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey, String keyId) {
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtKeyPair jwtKeyPair(AuthProperties properties, Environment environment) {
        if (texto(properties.getJwtPrivateKey()) && texto(properties.getJwtPublicKey())) {
            return new JwtKeyPair(
                    lerPublica(properties.getJwtPublicKey()),
                    lerPrivada(properties.getJwtPrivateKey()),
                    properties.getJwtKeyId());
        }
        boolean producao = environment.acceptsProfiles(Profiles.of("prod"));
        if (producao && !properties.isAllowEphemeralKeys()) {
            throw new IllegalStateException(
                    "APP_AUTH_JWT_PRIVATE_KEY e APP_AUTH_JWT_PUBLIC_KEY sao obrigatorias em producao.");
        }
        log.warn("Chaves JWT ausentes; gerando par efemero. Sessoes serao invalidadas ao reiniciar.");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new JwtKeyPair((RSAPublicKey) pair.getPublic(),
                    (RSAPrivateKey) pair.getPrivate(), properties.getJwtKeyId());
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar as chaves JWT de desenvolvimento.", ex);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtKeyPair keys) {
        RSAKey jwk = new RSAKey.Builder(keys.publicKey())
                .privateKey(keys.privateKey())
                .keyID(keys.keyId())
                .build();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(source);
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtKeyPair keys, AuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("roles");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(autoridades);
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    private static boolean texto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static byte[] decodificar(String valor) {
        String limpo = valor
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(limpo);
    }

    private static RSAPrivateKey lerPrivada(String valor) {
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decodificar(valor)));
        } catch (Exception ex) {
            throw new IllegalStateException("APP_AUTH_JWT_PRIVATE_KEY invalida.", ex);
        }
    }

    private static RSAPublicKey lerPublica(String valor) {
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decodificar(valor)));
        } catch (Exception ex) {
            throw new IllegalStateException("APP_AUTH_JWT_PUBLIC_KEY invalida.", ex);
        }
    }
}
