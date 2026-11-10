package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.Galeria;
import com.ranny.fotografia.galeria.model.GaleriaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GaleriaRepository extends JpaRepository<Galeria, String>,
        JpaSpecificationExecutor<Galeria> {

    Optional<Galeria> findByAgendamentoId(String agendamentoId);

    List<Galeria> findByAgendamentoIdIn(Collection<String> agendamentoIds);

    long countByStatus(GaleriaStatus status);

    /**
     * Serializa a criacao de midias da mesma galeria para que dois uploads
     * simultaneos nao calculem o mesmo proximo numero.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select galeria from Galeria galeria where galeria.id = :id")
    Optional<Galeria> findByIdComLock(@Param("id") String id);
}
