package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.PedidoExtras;
import com.ranny.fotografia.galeria.model.PedidoExtrasStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoExtrasRepository extends JpaRepository<PedidoExtras, String> {

    Optional<PedidoExtras> findFirstByGaleriaIdOrderByAuditoriaCriadoEmDesc(String galeriaId);

    Optional<PedidoExtras> findFirstByGaleriaIdAndStatusOrderByAuditoriaCriadoEmDesc(
            String galeriaId, PedidoExtrasStatus status);

    List<PedidoExtras> findByGaleriaIdOrderByAuditoriaCriadoEmAsc(String galeriaId);

    List<PedidoExtras> findByGaleriaIdInOrderByAuditoriaCriadoEmAsc(Collection<String> galeriaIds);

    boolean existsByGaleriaIdAndStatus(String galeriaId, PedidoExtrasStatus status);

    long countByStatus(PedidoExtrasStatus status);
}
