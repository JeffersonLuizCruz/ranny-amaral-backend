package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.TokenDownloadSelecao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TokenDownloadSelecaoRepository
        extends JpaRepository<TokenDownloadSelecao, String> {

    Optional<TokenDownloadSelecao> findByToken(String token);

    long deleteByExpiraEmBefore(Instant instante);
}
