package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.PedidoExtras;
import com.ranny.fotografia.galeria.model.PedidoExtrasStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PedidoExtrasRepository extends JpaRepository<PedidoExtras, String> {

    Optional<PedidoExtras> findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(String galeriaId);

    long countByStatus(PedidoExtrasStatus status);
}
