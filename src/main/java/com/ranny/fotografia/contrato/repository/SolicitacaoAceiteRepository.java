package com.ranny.fotografia.contrato.repository;

import com.ranny.fotografia.contrato.model.SolicitacaoAceite;
import com.ranny.fotografia.contrato.model.documento.StatusSolicitacaoAceite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** PATTERN: Repository para links publicos de aceite. */
@Repository
public interface SolicitacaoAceiteRepository extends JpaRepository<SolicitacaoAceite, String> {

    Optional<SolicitacaoAceite> findByToken(String token);

    List<SolicitacaoAceite> findByContratoId(String contratoId);

    List<SolicitacaoAceite> findByContratoIdOrderByCriadaEmDesc(String contratoId);

    List<SolicitacaoAceite> findByContratoIdAndStatusIn(
            String contratoId, Collection<StatusSolicitacaoAceite> statuses);

    Optional<SolicitacaoAceite> findTopByProtocoloStartingWithOrderByProtocoloDesc(String prefixo);
}
