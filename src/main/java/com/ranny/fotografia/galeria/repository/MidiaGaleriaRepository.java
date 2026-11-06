package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.MidiaGaleria;
import com.ranny.fotografia.galeria.model.MidiaStatus;
import com.ranny.fotografia.galeria.model.MidiaTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    long countByGaleriaIdAndTipoAndStatus(String galeriaId, MidiaTipo tipo, MidiaStatus status);

    long countByGaleriaIdAndTipoAndStatusNot(String galeriaId, MidiaTipo tipo, MidiaStatus status);

    Optional<MidiaGaleria> findByIdAndGaleriaId(String id, String galeriaId);
}
