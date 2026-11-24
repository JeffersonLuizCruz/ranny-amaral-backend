package com.ranny.fotografia.agenda.model;

import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.financeiro.CalculadoraPagamento;
import com.ranny.fotografia.shared.model.Auditoria;
import com.ranny.fotografia.shared.model.PagamentoEntrada;
import com.ranny.fotografia.shared.model.PagamentoSaldo;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Agregado raiz do modulo de agenda.
 *
 * <p>PATTERN: Aggregate Root (DDD) + Rich Domain Model + Composition.
 *
 * <p>O agendamento guarda apenas os IDs de cliente, pacote e contrato — nunca
 * referencias JPA entre modulos. Isso preserva as fronteiras do Spring Modulith,
 * evita {@code EAGER} acidental e permite evoluir cada modulo de forma
 * independente. As informacoes comerciais que precisam sobreviver a mudancas de
 * catalogo sao congeladas em {@link CondicoesPacote}.
 *
 * <p>Os pagamentos sao value objects embutidos; o estado so muda por metodos de
 * dominio, nunca por setters publicos.
 */
@Entity
@Table(name = "agendamentos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agendamento {

    private static final ZoneId FUSO_ESTUDIO = ZoneId.of("America/Sao_Paulo");

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "cliente_id", length = 64)
    private String clienteId;

    @Column(name = "pacote_id", nullable = false, length = 64)
    private String pacoteId;

    @Column(name = "contrato_id", length = 64)
    private String contratoId;

    @Embedded
    private ContatoReserva contato;

    @Column(name = "data_hora_inicio", nullable = false)
    private OffsetDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private OffsetDateTime dataHoraFim;

    @Column(nullable = false, length = 180)
    private String local;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "cancelamento_motivo", length = 1000)
    private String cancelamentoMotivo;

    @Column(name = "cancelado_em")
    private Instant canceladoEm;

    @Column(name = "cancelado_por", length = 120)
    private String canceladoPor;

    @Column(name = "valor_acordado", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorAcordado;

    @Column(name = "valor_entrada", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorEntrada;

    @Column(name = "percentual_entrada", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualEntrada;

    @Column(name = "saldo_restante", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoRestante;

    @Embedded
    private PagamentoEntrada pagamentoEntrada;

    @Embedded
    private PagamentoSaldo pagamentoFinal;

    @Embedded
    private CondicoesPacote condicoesPacote;

    @Embedded
    private TaxaDeslocamento taxaDeslocamento;

    @Column(name = "galeria_id", length = 64)
    private String galeriaId;

    @Embedded
    private Auditoria auditoria;

    public static Agendamento criar(String clienteId, ContatoReserva contato, String pacoteId,
                                    OffsetDateTime inicio, OffsetDateTime fim, String local,
                                    StatusAgendamento status, String observacoes,
                                    BigDecimal valorAcordado, BigDecimal valorEntrada,
                                    BigDecimal percentualEntrada, CondicoesPacote condicoes,
                                    TaxaDeslocamento taxaDeslocamento) {
        Agendamento agendamento = new Agendamento();
        agendamento.id = UUID.randomUUID().toString();
        agendamento.auditoria = Auditoria.iniciar();
        agendamento.aplicar(clienteId, contato, pacoteId, inicio, fim, local, status, observacoes,
                valorAcordado, valorEntrada, percentualEntrada, condicoes, taxaDeslocamento);
        agendamento.recalcularPagamentos();
        return agendamento;
    }

    /** Usado pelo seed de desenvolvimento para preservar IDs de demonstracao. */
    public static Agendamento restaurar(String id, String clienteId, ContatoReserva contato,
                                        String pacoteId, String contratoId, OffsetDateTime inicio,
                                        OffsetDateTime fim, String local, StatusAgendamento status,
                                        String observacoes, BigDecimal valorAcordado,
                                        BigDecimal valorEntrada, BigDecimal percentualEntrada,
                                        CondicoesPacote condicoes, TaxaDeslocamento taxaDeslocamento) {
        Agendamento agendamento = new Agendamento();
        agendamento.id = id;
        agendamento.auditoria = Auditoria.iniciar();
        agendamento.aplicar(clienteId, contato, pacoteId, inicio, fim, local, status, observacoes,
                valorAcordado, valorEntrada, percentualEntrada, condicoes, taxaDeslocamento);
        agendamento.contratoId = contratoId;
        agendamento.recalcularPagamentos();
        return agendamento;
    }

    public void atualizar(String clienteId, ContatoReserva contato, String pacoteId,
                          OffsetDateTime inicio, OffsetDateTime fim, String local,
                          StatusAgendamento status, String observacoes,
                          BigDecimal valorAcordado, BigDecimal valorEntrada,
                          BigDecimal percentualEntrada, CondicoesPacote condicoes,
                          TaxaDeslocamento taxaDeslocamento) {
        boolean pacoteMudou = !this.pacoteId.equals(pacoteId);
        aplicar(clienteId, contato, pacoteId, inicio, fim, local, status, observacoes,
                valorAcordado, valorEntrada, percentualEntrada,
                pacoteMudou || this.condicoesPacote == null ? condicoes : this.condicoesPacote,
                taxaDeslocamento);
        recalcularPagamentos();
        auditoria.marcarAtualizacao();
    }

    public void alterarStatus(StatusAgendamento novoStatus) {
        if (!status.podeTransicionarPara(novoStatus)) {
            throw new RegraDeNegocioException("TRANSICAO_INVALIDA",
                    "Nao e permitido alterar o agendamento de '%s' para '%s'."
                            .formatted(status.getValor(), novoStatus.getValor()));
        }
        this.status = novoStatus;
        auditoria.marcarAtualizacao();
    }

    /**
     * Cancela o ensaio exigindo justificativa.
     *
     * <p>O motivo fica preservado mesmo que o ensaio seja reaberto como
     * reserva; assim o historico do cancelamento nao se perde. Um ensaio ja
     * cancelado pode receber a justificativa depois (ex.: cancelamento vindo
     * do contrato), mas um ensaio realizado nunca pode ser cancelado.
     */
    public void cancelar(String motivo, String por, Instant quando) {
        if (!status.podeTransicionarPara(StatusAgendamento.CANCELADO)) {
            throw new RegraDeNegocioException("TRANSICAO_INVALIDA",
                    "Nao e permitido alterar o agendamento de '%s' para '%s'."
                            .formatted(status.getValor(), StatusAgendamento.CANCELADO.getValor()));
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioException("MOTIVO_CANCELAMENTO_OBRIGATORIO",
                    "Informe a justificativa do cancelamento do ensaio.");
        }
        String motivoNormalizado = motivo.trim();
        if (motivoNormalizado.length() > 1000) {
            throw new RegraDeNegocioException("MOTIVO_CANCELAMENTO_LONGO",
                    "A justificativa do cancelamento deve ter no maximo 1000 caracteres.");
        }
        this.status = StatusAgendamento.CANCELADO;
        this.cancelamentoMotivo = motivoNormalizado;
        this.canceladoEm = quando == null ? Instant.now() : quando;
        this.canceladoPor = por == null || por.isBlank() ? "Equipe do estudio" : por.trim();
        auditoria.marcarAtualizacao();
    }

    /** Nunca nulo: agendamentos anteriores a coluna sao lidos como taxa zero. */
    public TaxaDeslocamento getTaxaDeslocamento() {
        return taxaDeslocamento == null ? TaxaDeslocamento.nenhuma() : taxaDeslocamento;
    }

    public boolean entradaQuitada() {
        return pagamentoEntrada != null && pagamentoEntrada.estaQuitado();
    }

    public boolean saldoQuitado() {
        return pagamentoFinal != null && pagamentoFinal.estaQuitado();
    }

    /**
     * Promove o atendimento para REALIZADO quando a data do ensaio chegou e
     * entrada e saldo final estao quitados.
     *
     * <p>A data e avaliada no fuso do estudio, nao no offset do banco. Estados
     * CANCELADO, RESERVADO e REALIZADO nao sao alterados por este metodo.
     */
    public boolean promoverParaRealizado(LocalDate hoje) {
        if (status != StatusAgendamento.CONFIRMADO
                || dataHoraInicio == null
                || !entradaQuitada()
                || !saldoQuitado()) {
            return false;
        }
        LocalDate dataEnsaio = dataHoraInicio
                .atZoneSameInstant(FUSO_ESTUDIO)
                .toLocalDate();
        if (dataEnsaio.isAfter(hoje)) {
            return false;
        }
        this.status = StatusAgendamento.REALIZADO;
        auditoria.marcarAtualizacao();
        return true;
    }

    public void atualizarPagamentoEntrada(BigDecimal valor, BigDecimal percentual,
                                          PagamentoEntrada novoPagamento) {
        this.pagamentoEntrada = novoPagamento;
        this.valorEntrada = CalculadoraPagamento.normalizar(valor);
        this.percentualEntrada = CalculadoraPagamento.normalizar(percentual);
        this.saldoRestante = CalculadoraPagamento.saldoRestante(valorAcordado, this.valorEntrada);
        auditoria.marcarAtualizacao();
    }

    public void atualizarPagamentoFinal(PagamentoSaldo novoPagamento) {
        this.pagamentoFinal = novoPagamento;
        auditoria.marcarAtualizacao();
    }

    public void vincularContrato(String contratoId) {
        this.contratoId = contratoId;
        auditoria.marcarAtualizacao();
    }

    /**
     * Liga a reserva ao cliente cadastrado depois, no aceite do contrato.
     * Nao sobrescreve um cliente que ja estava vinculado.
     */
    public void vincularCliente(String clienteId) {
        if (clienteId == null || clienteId.isBlank() || this.clienteId != null) {
            return;
        }
        this.clienteId = clienteId;
        auditoria.marcarAtualizacao();
    }

    /** Remove o vínculo quando o contrato é excluído. */
    public void desvincularContrato() {
        this.contratoId = null;
        auditoria.marcarAtualizacao();
    }

    public void vincularGaleria(String galeriaId) {
        this.galeriaId = galeriaId;
        auditoria.marcarAtualizacao();
    }

    /**
     * Sincroniza o agendamento a partir do contrato.
     *
     * <p>Ensaio ja realizado nao tem status reescrito (o fato aconteceu), mas os
     * pagamentos continuam espelhados. O mapeamento contrato -> agenda e
     * proposital: o contrato e a fonte de verdade comercial.
     */
    public void sincronizarComContrato(StatusAgendamento statusContrato,
                                       PagamentoEntrada pagamento,
                                       PagamentoSaldo pagamentoFinal) {
        if (this.status != StatusAgendamento.REALIZADO && statusContrato != null
                && this.status.podeTransicionarPara(statusContrato)) {
            this.status = statusContrato;
        }
        if (pagamento != null) {
            this.pagamentoEntrada = pagamento;
            this.valorEntrada = pagamento.getValor() == null ? BigDecimal.ZERO : pagamento.getValor();
            this.percentualEntrada = pagamento.getPercentual() == null
                    ? CalculadoraPagamento.percentualEntrada(valorAcordado, valorEntrada)
                    : pagamento.getPercentual();
            this.saldoRestante = CalculadoraPagamento.saldoRestante(valorAcordado, valorEntrada);
        }
        if (pagamentoFinal != null) {
            this.pagamentoFinal = pagamentoFinal;
        }
        auditoria.marcarAtualizacao();
    }

    public void validarExclusao() {
        if (contratoId != null) {
            throw new com.ranny.fotografia.shared.exception.ConflitoException(
                    "AGENDAMENTO_COM_CONTRATO",
                    "O agendamento possui contrato vinculado e nao pode ser excluido.");
        }
    }

    private void aplicar(String clienteId, ContatoReserva contato, String pacoteId,
                         OffsetDateTime inicio, OffsetDateTime fim, String local,
                         StatusAgendamento status, String observacoes, BigDecimal valorAcordado,
                         BigDecimal valorEntrada, BigDecimal percentualEntrada,
                         CondicoesPacote condicoes, TaxaDeslocamento taxaDeslocamento) {
        this.clienteId = clienteId;
        this.contato = contato;
        this.pacoteId = pacoteId;
        this.dataHoraInicio = inicio;
        this.dataHoraFim = fim;
        this.local = local;
        this.status = status == null ? StatusAgendamento.RESERVADO : status;
        this.observacoes = observacoes;
        this.valorAcordado = CalculadoraPagamento.normalizar(valorAcordado);
        this.valorEntrada = CalculadoraPagamento.normalizar(valorEntrada);
        this.percentualEntrada = CalculadoraPagamento.normalizar(percentualEntrada);
        this.condicoesPacote = condicoes;
        this.taxaDeslocamento = taxaDeslocamento == null
                ? TaxaDeslocamento.nenhuma()
                : taxaDeslocamento;
        if (this.taxaDeslocamento.isRepassada()
                && this.valorAcordado.compareTo(this.taxaDeslocamento.getValor()) < 0) {
            throw new RegraDeNegocioException("DESLOCAMENTO_MAIOR_QUE_TOTAL",
                    "O valor total nao pode ser menor que a taxa de deslocamento repassada.");
        }
    }

    /**
     * Mantem a coerencia entre valores e pagamentos embutidos. E chamado na
     * criacao/edicao, quando o pagamento pode nao ter sido informado.
     */
    private void recalcularPagamentos() {
        if (condicoesPacote == null) {
            throw new RegraDeNegocioException("CONDICOES_AUSENTES",
                    "As condicoes comerciais do pacote sao obrigatorias.");
        }
        if (dataHoraFim == null || !dataHoraFim.isAfter(dataHoraInicio)) {
            throw new RegraDeNegocioException("PERIODO_INVALIDO",
                    "O horario de fim deve ser posterior ao horario de inicio.");
        }
        if (valorAcordado == null || valorAcordado.signum() < 0) {
            throw new RegraDeNegocioException("VALOR_INVALIDO",
                    "O valor acordado nao pode ser negativo.");
        }
        if (pagamentoEntrada == null) {
            pagamentoEntrada = PagamentoEntrada.pendente(valorEntrada, percentualEntrada);
        }
        if (pagamentoFinal == null) {
            saldoRestante = CalculadoraPagamento.saldoRestante(valorAcordado, valorEntrada);
            pagamentoFinal = PagamentoSaldo.pendente(saldoRestante);
        } else {
            saldoRestante = CalculadoraPagamento.saldoRestante(valorAcordado, valorEntrada);
            pagamentoFinal.redefinirValor(saldoRestante);
        }
    }
}
