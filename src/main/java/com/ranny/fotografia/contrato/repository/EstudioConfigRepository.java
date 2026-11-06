package com.ranny.fotografia.contrato.repository;

import com.ranny.fotografia.contrato.model.EstudioConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** PATTERN: Repository do singleton de configuracao do estudio. */
@Repository
public interface EstudioConfigRepository extends JpaRepository<EstudioConfig, String> {
}
