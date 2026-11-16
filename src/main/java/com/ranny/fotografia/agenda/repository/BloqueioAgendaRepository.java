package com.ranny.fotografia.agenda.repository;

import com.ranny.fotografia.agenda.model.BloqueioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/** Consultas de indisponibilidade da agenda. */
@Repository
public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, String> {

    List<BloqueioAgenda> findByInicioLessThanAndFimGreaterThanOrderByInicioAsc(
            OffsetDateTime fim, OffsetDateTime inicio);

    boolean existsByInicioLessThanAndFimGreaterThan(OffsetDateTime fim, OffsetDateTime inicio);
}
