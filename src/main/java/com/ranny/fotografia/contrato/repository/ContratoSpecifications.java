package com.ranny.fotografia.contrato.repository;

import com.ranny.fotografia.contrato.model.Contrato;
import com.ranny.fotografia.contrato.model.documento.StatusAceite;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * PATTERN: Specification (Spring Data JPA).
 *
 * <p>Cada metodo devolve um predicado independente; o service compoe apenas os
 * filtros informados com {@code where(...).and(...)}. Isso substitui uma
 * combinacao explosiva de queries derivadas e mantem os filtros testaveis.
 */
public final class ContratoSpecifications {

    private ContratoSpecifications() {
    }

    public static Specification<Contrato> comStatus(StatusAceite status) {
        return status == null ? null
                : (root, query, cb) -> cb.equal(root.get("statusAceite"), status);
    }

    public static Specification<Contrato> comStatusPagamento(PagamentoStatus status) {
        return status == null ? null
                : (root, query, cb) -> cb.equal(root.get("statusPagamento"), status);
    }

    public static Specification<Contrato> doCliente(String clienteId) {
        return clienteId == null || clienteId.isBlank() ? null
                : (root, query, cb) -> cb.equal(root.get("clienteId"), clienteId);
    }

    public static Specification<Contrato> doAgendamento(String agendamentoId) {
        return agendamentoId == null || agendamentoId.isBlank() ? null
                : (root, query, cb) -> cb.equal(root.get("agendamentoId"), agendamentoId);
    }

    /**
     * Busca textual por numero, nome do cliente ou nome do pacote.
     * Os nomes vem de colunas desnormalizadas (V2), nao do JSON do snapshot.
     */
    public static Specification<Contrato> busca(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        String like = "%" + termo.trim().toLowerCase(java.util.Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("numero")), like),
                cb.like(cb.lower(root.get("clienteNome")), like),
                cb.like(cb.lower(root.get("pacoteNome")), like));
    }
}
