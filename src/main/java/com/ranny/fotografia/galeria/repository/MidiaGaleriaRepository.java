package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.MidiaGaleria;
import com.ranny.fotografia.galeria.model.MidiaStatus;
import com.ranny.fotografia.galeria.model.MidiaTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MidiaGaleriaRepository extends JpaRepository<MidiaGaleria, String> {

    Page<MidiaGaleria> findByGaleriaIdAndStatusNotOrderByOrdemAsc(
            String galeriaId, MidiaStatus status, Pageable pageable);

    List<MidiaGaleria> findByGaleriaIdAndStatusNotOrderByOrdemAsc(
            String galeriaId, MidiaStatus status);

    List<MidiaGaleria> findByGaleriaIdAndIdInAndStatus(String galeriaId,
                                                       Collection<String> ids,
                                                       MidiaStatus status);

    List<MidiaGaleria> findByGaleriaIdAndIdIn(String galeriaId, Collection<String> ids);

    long countByGaleriaIdAndTipoAndStatus(String galeriaId, MidiaTipo tipo, MidiaStatus status);

    long countByGaleriaIdAndTipoAndStatusNot(String galeriaId, MidiaTipo tipo, MidiaStatus status);

    /**
     * Proximo numero inclui midias removidas de proposito: o numero nunca e
     * reutilizado, mesmo depois de uma exclusao.
     */
    @Query("""
            select coalesce(max(midia.ordem), 0)
            from MidiaGaleria midia
            where midia.galeriaId = :galeriaId and midia.tipo = :tipo
            """)
    int maiorOrdem(@Param("galeriaId") String galeriaId, @Param("tipo") MidiaTipo tipo);

    Optional<MidiaGaleria> findByIdAndGaleriaId(String id, String galeriaId);
}
