package com.ranny.fotografia.arquivo.repository;

import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.arquivo.ArquivoStatus;
import com.ranny.fotografia.arquivo.model.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
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

    List<Arquivo> findByReferenciaIdAndStatusNot(String referenciaId, ArquivoStatus status);

    long countByObjectKeyAndStatusNot(String objectKey, ArquivoStatus status);

    long countByStatusNot(ArquivoStatus status);

    @Query("""
            select a.finalidade, sum(a.tamanhoBytes), count(a)
            from Arquivo a
            where a.status <> :excluido
            group by a.finalidade
            """)
    List<Object[]> totaisPorFinalidade(@Param("excluido") ArquivoStatus excluido);

    @Query("""
            select a.referenciaId, sum(a.tamanhoBytes), count(a)
            from Arquivo a
            where a.referenciaId in :referencias and a.status <> :excluido
            group by a.referenciaId
            """)
    List<Object[]> totaisPorReferencia(@Param("referencias") Collection<String> referencias,
                                       @Param("excluido") ArquivoStatus excluido);

    @Query("""
            select a.objectKey
            from Arquivo a
            where a.status <> :excluido
            """)
    List<String> objectKeysAtivas(@Param("excluido") ArquivoStatus excluido);
}
