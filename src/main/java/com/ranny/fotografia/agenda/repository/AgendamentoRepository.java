package com.ranny.fotografia.agenda.repository;

import com.ranny.fotografia.agenda.model.Agendamento;
import com.ranny.fotografia.agenda.model.StatusAgendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * PATTERN: Repository (Spring Data).
 *
 * <p>As consultas cobrem os filtros do calendario (periodo e status) sem
 * {@code Specification} ou SQL manual. Os booleanos de vinculo alimentam as
 * portas de protecao de exclusao de cliente/pacote.
 */
@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, String> {

    List<Agendamento> findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            OffsetDateTime inicio, OffsetDateTime fim);

    List<Agendamento> findByDataHoraInicioBetweenAndStatusOrderByDataHoraInicioAsc(
            OffsetDateTime inicio, OffsetDateTime fim, StatusAgendamento status);

    Page<Agendamento> findByDataHoraInicioBetweenOrderByDataHoraInicioAsc(
            OffsetDateTime inicio, OffsetDateTime fim, Pageable pageable);

    Page<Agendamento> findByDataHoraInicioBetweenAndStatusOrderByDataHoraInicioAsc(
            OffsetDateTime inicio, OffsetDateTime fim, StatusAgendamento status, Pageable pageable);

    Page<Agendamento> findByStatusOrderByDataHoraInicioAsc(
            StatusAgendamento status, Pageable pageable);

    List<Agendamento> findByStatus(StatusAgendamento status);

    boolean existsByClienteId(String clienteId);

    boolean existsByPacoteId(String pacoteId);
}
