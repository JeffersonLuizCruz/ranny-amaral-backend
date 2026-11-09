package com.ranny.fotografia.auth.repository;

import com.ranny.fotografia.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUsuarioIdAndRevogadoEmIsNull(String usuarioId);

    long countByUsuarioIdAndRevogadoEmIsNullAndExpiraEmAfter(String usuarioId, Instant agora);

    void deleteByExpiraEmBefore(Instant limite);
}
