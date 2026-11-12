package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.EventoGaleria;
import com.ranny.fotografia.galeria.model.EventoGaleriaTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoGaleriaRepository extends JpaRepository<EventoGaleria, String> {

    List<EventoGaleria> findByGaleriaIdOrderByCriadoEmAsc(String galeriaId);

    List<EventoGaleria> findByGaleriaIdInAndTipoOrderByCriadoEmAsc(
            java.util.Collection<String> galeriaIds, EventoGaleriaTipo tipo);
}
