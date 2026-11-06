package com.ranny.fotografia.contrato.repository;

import com.ranny.fotografia.contrato.model.ModeloContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** PATTERN: Repository para os modelos (templates) de contrato. */
@Repository
public interface ModeloContratoRepository extends JpaRepository<ModeloContrato, String> {

    List<ModeloContrato> findAllByOrderByNomeAsc();
}
