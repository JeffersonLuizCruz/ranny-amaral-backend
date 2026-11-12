package com.ranny.fotografia.armazenamento.repository;

import com.ranny.fotografia.armazenamento.model.AmostraArmazenamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmostraArmazenamentoRepository extends JpaRepository<AmostraArmazenamento, String> {

    Optional<AmostraArmazenamento> findFirstByOrderByColetadoEmDesc();

    List<AmostraArmazenamento> findTop30ByOrderByColetadoEmDesc();
}
