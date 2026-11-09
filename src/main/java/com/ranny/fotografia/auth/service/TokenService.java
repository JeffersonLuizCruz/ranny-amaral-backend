package com.ranny.fotografia.auth.service;

import com.ranny.fotografia.auth.model.RefreshToken;
import com.ranny.fotografia.auth.model.Usuario;
import com.ranny.fotografia.auth.repository.RefreshTokenRepository;
import com.ranny.fotografia.auth.security.AuthProperties;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Emissao, hash e revogacao de tokens. */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenService(JwtEncoder jwtEncoder, AuthProperties properties,
                        RefreshTokenRepository refreshTokenRepository) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String gerarAccessToken(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(agora)
                .expiresAt(agora.plus(properties.getAccessTokenMinutes(), ChronoUnit.MINUTES))
                .subject(usuario.getId())
                .id(UUID.randomUUID().toString())
                .claim("email", usuario.getEmail())
                .claim("roles", List.of(usuario.getRole().name()))
                .claim("pwd", usuario.isDeveTrocarSenha())
                .claim("ver", usuario.getTokenVersion())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(properties.getJwtKeyId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public RefreshGerado gerarRefresh(Usuario usuario, String ip, String userAgent) {
        String valor = gerarValorOpaco();
        Instant agora = Instant.now();
        RefreshToken token = RefreshToken.criar(
                usuario.getId(), hash(valor),
                agora.plus(properties.getRefreshTokenDays(), ChronoUnit.DAYS),
                ip, userAgent, agora);
        return new RefreshGerado(valor, refreshTokenRepository.save(token));
    }

    public Optional<RefreshToken> buscarPorValor(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        return refreshTokenRepository.findByTokenHash(hash(valor));
    }

    public void revogarTodos(String usuarioId, Instant agora) {
        for (RefreshToken token : refreshTokenRepository
                .findByUsuarioIdAndRevogadoEmIsNull(usuarioId)) {
            token.revogar(agora);
        }
    }

    public long accessTokenExpiresInSeconds() {
        return properties.getAccessTokenMinutes() * 60L;
    }

    public static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 indisponivel.", ex);
        }
    }

    private static String gerarValorOpaco() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RefreshGerado(String valor, RefreshToken token) {
    }
}
