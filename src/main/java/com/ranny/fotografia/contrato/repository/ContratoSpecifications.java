package com.ranny.fotografia.contrato.repository;

import com.ranny.fotografia.contrato.model.Contrato;
import com.ranny.fotografia.contrato.model.SituacaoContrato;
import com.ranny.fotografia.contrato.model.documento.StatusAceite;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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

    public static Specification<Contrato> comStatusEntrada(PagamentoStatus status) {
        return status == null ? null
                : (root, query, cb) -> cb.equal(root.get("statusPagamento"), status);
    }

    public static Specification<Contrato> comStatusSaldoFinal(PagamentoStatus status) {
        return status == null ? null
                : (root, query, cb) -> cb.equal(root.get("statusPagamentoFinal"), status);
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

    /**
     * Fila operacional do staff. As tres situacoes sao mutuamente exclusivas:
     * um comprovante em analise tem prioridade sobre o aceite; contratos sem
     * aceite nao entram na fila de entrada.
     */
    public static Specification<Contrato> daSituacao(SituacaoContrato situacao) {
        if (situacao == null) {
            return null;
        }
        return switch (situacao) {
            case AGUARDANDO_ACEITE -> aguardandoAceite();
            case ENTRADA_EM_ABERTO -> entradaEmAberto();
            case COMPROVANTE_EM_ANALISE -> comprovanteEmAnalise();
        };
    }

    public static Specification<Contrato> aguardandoAceite() {
        return (root, query, cb) -> cb.and(
                root.get("statusAceite").in(StatusAceite.PENDENTE, StatusAceite.EXPIRADO),
                cb.not(comprovanteEmAnalisePredicate(root, cb)));
    }

    public static Specification<Contrato> entradaEmAberto() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("statusAceite"), StatusAceite.ACEITO),
                cb.equal(root.get("statusPagamento"), PagamentoStatus.PENDENTE),
                cb.or(
                        cb.isNull(root.get("pagamento").get("comprovante")),
                        cb.isNotNull(root.get("pagamento").get("rejeicao"))));
    }

    public static Specification<Contrato> comprovanteEmAnalise() {
        return (root, query, cb) -> comprovanteEmAnalisePredicate(root, cb);
    }

    private static Predicate comprovanteEmAnalisePredicate(Root<Contrato> root,
                                                           CriteriaBuilder cb) {
        return cb.and(
                cb.equal(root.get("statusPagamento"), PagamentoStatus.PENDENTE),
                cb.isNotNull(root.get("pagamento").get("comprovante")),
                cb.isNull(root.get("pagamento").get("rejeicao")));
    }
}
