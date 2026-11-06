package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.Galeria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GaleriaRepository extends JpaRepository<Galeria, String>,
        JpaSpecificationExecutor<Galeria> {

    Optional<Galeria> findByAgendamentoId(String agendamentoId);
}
