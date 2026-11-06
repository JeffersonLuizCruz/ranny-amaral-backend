package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.LinkGaleria;
import com.ranny.fotografia.galeria.model.LinkGaleriaStatus;
import com.ranny.fotografia.galeria.model.LinkGaleriaTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface LinkGaleriaRepository extends JpaRepository<LinkGaleria, String> {

    Optional<LinkGaleria> findByToken(String token);

    List<LinkGaleria> findByGaleriaIdOrderByExpiraEmDesc(String galeriaId);

    Optional<LinkGaleria> findFirstByGaleriaIdAndTipoAndStatusOrderByExpiraEmDesc(
            String galeriaId, LinkGaleriaTipo tipo, LinkGaleriaStatus status);

    Optional<LinkGaleria> findFirstByGaleriaIdAndTipoOrderByExpiraEmDesc(
            String galeriaId, LinkGaleriaTipo tipo);
}
