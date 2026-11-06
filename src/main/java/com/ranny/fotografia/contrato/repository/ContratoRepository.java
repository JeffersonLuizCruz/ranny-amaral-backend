package com.ranny.fotografia.contrato.repository;

import com.ranny.fotografia.contrato.model.Contrato;
import com.ranny.fotografia.contrato.model.documento.StatusAceite;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** PATTERN: Repository + Specification (filtros combinaveis). */
@Repository
public interface ContratoRepository extends JpaRepository<Contrato, String>,
        JpaSpecificationExecutor<Contrato> {

    Optional<Contrato> findTopByNumeroStartingWithOrderByNumeroDesc(String prefixo);

    Optional<Contrato> findFirstByAgendamentoIdOrderByDataEmissaoDesc(String agendamentoId);

    boolean existsByAgendamentoId(String agendamentoId);

    List<Contrato> findByAgendamentoIdInOrderByDataEmissaoDesc(Collection<String> agendamentoIds);

    long countByStatusAceite(StatusAceite statusAceite);

    long countByStatusAceiteNotAndStatusPagamento(StatusAceite statusAceite,
                                                  PagamentoStatus statusPagamento);

    long countByStatusAceiteNotAndStatusPagamentoFinal(StatusAceite statusAceite,
                                                       PagamentoStatus statusPagamentoFinal);
}
