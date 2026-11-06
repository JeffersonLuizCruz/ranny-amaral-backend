package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.DownloadGaleria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DownloadGaleriaRepository extends JpaRepository<DownloadGaleria, String> {
}
