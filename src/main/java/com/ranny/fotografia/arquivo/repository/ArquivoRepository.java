package com.ranny.fotografia.arquivo.repository;

import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.arquivo.ArquivoStatus;
import com.ranny.fotografia.arquivo.model.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** PATTERN: Repository (Spring Data) do catalogo generico de arquivos. */
@Repository
public interface ArquivoRepository extends JpaRepository<Arquivo, String> {

    Optional<Arquivo> findByIdAndFinalidadeAndReferenciaId(
            String id, ArquivoFinalidade finalidade, String referenciaId);

    Optional<Arquivo> findFirstByFinalidadeAndReferenciaIdAndChecksumSha256AndStatus(
            ArquivoFinalidade finalidade, String referenciaId, String checksumSha256, ArquivoStatus status);

    List<Arquivo> findTop100ByStatusInAndExpiraEmBefore(
            List<ArquivoStatus> statuses, Instant limite);
}
