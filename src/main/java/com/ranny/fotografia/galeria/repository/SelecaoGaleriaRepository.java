package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.SelecaoGaleria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SelecaoGaleriaRepository extends JpaRepository<SelecaoGaleria, String> {

    Optional<SelecaoGaleria> findByProtocolo(String protocolo);

    Optional<SelecaoGaleria> findFirstByGaleriaIdOrderByEnviadaEmDesc(String galeriaId);
}
