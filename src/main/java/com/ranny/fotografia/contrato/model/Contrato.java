package com.ranny.fotografia.contrato.model;

import com.ranny.fotografia.contrato.model.converter.ContratoClausulasConverter;
import com.ranny.fotografia.contrato.model.converter.DadosAceiteConverter;
import com.ranny.fotografia.contrato.model.converter.HistoricoContratoConverter;
import com.ranny.fotografia.contrato.model.converter.SnapshotContratoConverter;
import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import com.ranny.fotografia.contrato.model.documento.ContratoEvento;
import com.ranny.fotografia.contrato.model.documento.ContratoEventoTipo;
import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot;
import com.ranny.fotografia.contrato.model.documento.DadosAceite;
import com.ranny.fotografia.contrato.model.documento.StatusAceite;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.financeiro.CalculadoraPagamento;
import com.ranny.fotografia.shared.model.Auditoria;
import com.ranny.fotografia.shared.model.ComprovantePagamento;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoEntrada;
import com.ranny.fotografia.shared.model.PagamentoSaldo;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import com.ranny.fotografia.shared.model.RejeicaoComprovante;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agregado raiz do modulo de contratos.
 *
 * <p>PATTERN: Aggregate Root (DDD) + State Machine + Memento (snapshot).
 *
 * <p>O contrato e a fonte de verdade comercial: guarda o snapshot imutavel do
 * que foi vendido, o historico append-only e o ciclo de vida do aceite e dos
 * pagamentos. Todos os metodos publicos expressam transicoes de negocio e
 * validam o estado atual; nao existem setters.
 *
 * <p>Dados consultaveis (numero, status, valores) ficam em colunas relacionais.
 * Documentos grandes (snapshot, clausulas, historico, aceite) ficam em JSON
 * (CLOB) por meio de conversores, reduzindo joins sem perder o conteudo.
 */
@Entity
@Table(name = "contratos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contrato {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "cliente_id", length = 64)
    private String clienteId;

    @Column(name = "agendamento_id", length = 64)
    private String agendamentoId;

    @Column(name = "pacote_id", length = 64)
    private String pacoteId;

    /**
     * Nomes desnormalizados do snapshot para busca e ordenacao sem varrer o
     * JSON. Sao mantidos pelo agregado em criar/atualizar/duplicar.
     */
    @Column(name = "cliente_nome", length = 120)
    private String clienteNome;

    @Column(name = "pacote_nome", length = 120)
    private String pacoteNome;

    @Column(name = "modelo_id", nullable = false, length = 64)
    private String modeloId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_aceite", nullable = false, length = 20)
    private StatusAceite statusAceite;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private PagamentoStatus statusPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento_final", nullable = false, length = 20)
    private PagamentoStatus statusPagamentoFinal;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Convert(converter = ContratoClausulasConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<ContratoClausula> clausulas;

    @Convert(converter = ContratoClausulasConverter.class)
    @Column(name = "clausulas_modelo", columnDefinition = "TEXT")
    private List<ContratoClausula> clausulasModelo;

    @Convert(converter = SnapshotContratoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private ContratoSnapshot snapshot;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "valor_entrada", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorEntrada;

    @Column(name = "percentual_entrada", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualEntrada;

    @Column(name = "saldo_restante", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoRestante;

    @Embedded
    private PagamentoEntrada pagamento;

    @Embedded
    private PagamentoSaldo pagamentoFinal;

    @Column(name = "forma_pagamento", length = 40)
    private String formaPagamento;

    @Column(name = "condicoes_pagamento", length = 1000)
    private String condicoesPagamento;

    @Column(name = "data_emissao", nullable = false)
    private Instant dataEmissao;

    @Column(name = "data_validade")
    private Instant dataValidade;

    @Column(name = "solicitacao_atual_id", length = 64)
    private String solicitacaoAtualId;

    @Convert(converter = DadosAceiteConverter.class)
    @Column(columnDefinition = "TEXT")
    private DadosAceite aceite;

    @Convert(converter = HistoricoContratoConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<ContratoEvento> historico;

    @Column(length = 1000)
    private String observacoes;

    @Embedded
    private Auditoria auditoria;

    public static Contrato criar(DadosContrato dados) {
        Contrato contrato = new Contrato();
        contrato.id = dados.id() == null || dados.id().isBlank()
                ? UUID.randomUUID().toString()
                : dados.id();
        contrato.numero = dados.numero();
        contrato.auditoria = Auditoria.iniciar();
        contrato.historico = new ArrayList<>();
        contrato.aplicarDados(dados);
        contrato.statusAceite = StatusAceite.RASCUNHO;
        contrato.pagamento = PagamentoEntrada.pendente(dados.valorEntrada(), dados.percentualEntrada());
        contrato.statusPagamento = contrato.pagamento.getStatus();
        contrato.saldoRestante = CalculadoraPagamento.saldoRestante(dados.valorTotal(), dados.valorEntrada());
        contrato.pagamentoFinal = PagamentoSaldo.pendente(contrato.saldoRestante);
        contrato.statusPagamentoFinal = contrato.pagamentoFinal.getStatus();
        contrato.adicionarHistorico(ContratoEventoTipo.CRIADO, "Contrato criado.", contrato.dataEmissao);
        return contrato;
    }

    public void atualizar(DadosContrato dados) {
        validarRascunho();
        aplicarDados(dados);
        if (pagamento == null) {
            pagamento = PagamentoEntrada.pendente(dados.valorEntrada(), dados.percentualEntrada());
        } else {
            pagamento.redefinir(valorEntrada, percentualEntrada);
        }
        statusPagamento = pagamento.getStatus();
        saldoRestante = CalculadoraPagamento.saldoRestante(valorTotal, valorEntrada);
        if (pagamentoFinal == null) {
            pagamentoFinal = PagamentoSaldo.pendente(saldoRestante);
        } else {
            pagamentoFinal.redefinirValor(saldoRestante);
        }
        statusPagamentoFinal = pagamentoFinal.getStatus();
        adicionarHistorico(ContratoEventoTipo.ATUALIZADO, "Contrato atualizado.", Instant.now());
        auditoria.marcarAtualizacao();
    }

    /** Duplica o contrato como novo rascunho, sem aceite, link ou comprovantes. */
    public Contrato duplicar(String novoNumero) {
        Contrato duplicado = new Contrato();
        duplicado.id = UUID.randomUUID().toString();
        duplicado.numero = novoNumero;
        duplicado.clienteId = clienteId;
        duplicado.agendamentoId = agendamentoId;
        duplicado.pacoteId = pacoteId;
        duplicado.clienteNome = clienteNome != null
                ? clienteNome
                : snapshot == null || snapshot.cliente() == null ? null : snapshot.cliente().nome();
        duplicado.pacoteNome = pacoteNome != null
                ? pacoteNome
                : snapshot == null || snapshot.pacote() == null ? null : snapshot.pacote().nome();
        duplicado.modeloId = modeloId;
        duplicado.titulo = titulo;
        duplicado.clausulas = List.copyOf(clausulas == null ? List.of() : clausulas);
        duplicado.clausulasModelo = List.copyOf(clausulasModelo == null ? List.of() : clausulasModelo);
        duplicado.snapshot = snapshot;
        duplicado.valorTotal = valorTotal;
        duplicado.valorEntrada = valorEntrada;
        duplicado.percentualEntrada = percentualEntrada;
        duplicado.saldoRestante = saldoRestante;
        duplicado.formaPagamento = formaPagamento;
        duplicado.condicoesPagamento = condicoesPagamento;
        duplicado.observacoes = observacoes;
        duplicado.dataEmissao = Instant.now();
        duplicado.statusAceite = StatusAceite.RASCUNHO;
        duplicado.pagamento = PagamentoEntrada.pendente(valorEntrada, percentualEntrada);
        duplicado.statusPagamento = duplicado.pagamento.getStatus();
        duplicado.pagamentoFinal = PagamentoSaldo.pendente(saldoRestante);
        duplicado.statusPagamentoFinal = duplicado.pagamentoFinal.getStatus();
        duplicado.historico = new ArrayList<>();
        duplicado.auditoria = Auditoria.iniciar();
        duplicado.adicionarHistorico(ContratoEventoTipo.DUPLICADO,
                "Duplicado de " + numero + ".", duplicado.dataEmissao);
        return duplicado;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida do aceite
    // ------------------------------------------------------------------

    public void registrarLink(SolicitacaoAceite solicitacao) {
        if (statusAceite == StatusAceite.ACEITO || statusAceite == StatusAceite.CANCELADO) {
            throw new RegraDeNegocioException("CONTRATO_NAO_RECEBE_LINK",
                    "Contratos aceitos ou cancelados nao podem gerar novos links de aceite.");
        }
        this.statusAceite = StatusAceite.PENDENTE;
        this.solicitacaoAtualId = solicitacao.getId();
        this.dataValidade = solicitacao.getExpiraEm();
        adicionarHistorico(ContratoEventoTipo.LINK_GERADO,
                "Link de aceite gerado com validade de %d dias.".formatted(
                        java.time.Duration.between(solicitacao.getCriadaEm(), solicitacao.getExpiraEm()).toDays()),
                Instant.now());
        auditoria.marcarAtualizacao();
    }

    public void registrarVisualizacao() {
        adicionarHistorico(ContratoEventoTipo.LINK_VISUALIZADO,
                "Link de aceite visualizado pelo cliente.", Instant.now());
        auditoria.marcarAtualizacao();
    }

    public void registrarAceite(DadosAceite dados, String clienteId, String solicitacaoId) {
        if (statusAceite == StatusAceite.CANCELADO) {
            throw new RegraDeNegocioException("CONTRATO_CANCELADO",
                    "Contrato cancelado nao pode receber aceite.");
        }
        boolean clienteNovo = clienteId != null && !clienteId.equals(this.clienteId);
        this.statusAceite = StatusAceite.ACEITO;
        this.aceite = dados;
        this.solicitacaoAtualId = solicitacaoId;
        if (clienteId != null) {
            this.clienteId = clienteId;
        }
        adicionarHistorico(ContratoEventoTipo.ACEITE_REGISTRADO,
                "Aceite registrado com protocolo %s.".formatted(dados.protocolo()), dados.aceitoEm());
        if (clienteNovo) {
            adicionarHistorico(ContratoEventoTipo.CLIENTE_VINCULADO,
                    "Cliente vinculado ao contrato.", Instant.now());
        }
        auditoria.marcarAtualizacao();
    }

    public void expirar() {
        if (statusAceite != StatusAceite.PENDENTE) {
            return;
        }
        this.statusAceite = StatusAceite.EXPIRADO;
        adicionarHistorico(ContratoEventoTipo.LINK_EXPIRADO, "O link de aceite expirou.", Instant.now());
        auditoria.marcarAtualizacao();
    }

    public void reabrir() {
        if (statusAceite != StatusAceite.PENDENTE && statusAceite != StatusAceite.EXPIRADO) {
            throw new RegraDeNegocioException("CONTRATO_NAO_REABRIVEL",
                    "Somente contratos pendentes ou expirados podem ser reabertos.");
        }
        this.statusAceite = StatusAceite.RASCUNHO;
        this.solicitacaoAtualId = null;
        this.dataValidade = null;
        adicionarHistorico(ContratoEventoTipo.REABERTO, "Contrato reaberto como rascunho.", Instant.now());
        auditoria.marcarAtualizacao();
    }

    public void cancelarLink() {
        this.solicitacaoAtualId = null;
        this.dataValidade = null;
        if (statusAceite == StatusAceite.PENDENTE) {
            this.statusAceite = StatusAceite.RASCUNHO;
        }
        adicionarHistorico(ContratoEventoTipo.LINK_CANCELADO, "Link de aceite cancelado.", Instant.now());
        auditoria.marcarAtualizacao();
    }

    public void cancelar() {
        if (statusAceite == StatusAceite.ACEITO) {
            throw new RegraDeNegocioException("CONTRATO_ACEITO",
                    "Contrato ja aceito pelo cliente nao pode ser cancelado por esta operacao.");
        }
        this.statusAceite = StatusAceite.CANCELADO;
        pagamento.cancelar();
        statusPagamento = pagamento.getStatus();
        pagamentoFinal.cancelar();
        statusPagamentoFinal = pagamentoFinal.getStatus();
        adicionarHistorico(ContratoEventoTipo.CONTRATO_CANCELADO, "Contrato cancelado pelo estudio.", Instant.now());
        auditoria.marcarAtualizacao();
    }

    // ------------------------------------------------------------------
    // Pagamentos
    // ------------------------------------------------------------------

    public void registrarComprovante(ComprovantePagamento comprovante) {
        if (statusAceite == StatusAceite.CANCELADO) {
            throw new RegraDeNegocioException("CONTRATO_CANCELADO",
                    "Contrato cancelado nao aceita comprovante.");
        }
        pagamento.registrarComprovante(comprovante);
        statusPagamento = pagamento.getStatus();
        adicionarHistorico(ContratoEventoTipo.COMPROVANTE_ENVIADO,
                "Comprovante de pagamento enviado.", comprovante.enviadoEm());
        auditoria.marcarAtualizacao();
    }

    public void confirmarPagamento(Instant em, String por) {
        pagamento.confirmar(em, por);
        statusPagamento = pagamento.getStatus();
        adicionarHistorico(ContratoEventoTipo.PAGAMENTO_CONFIRMADO,
                "Entrada confirmada por %s.".formatted(por), em);
        auditoria.marcarAtualizacao();
    }

    public void rejeitarComprovante(String motivo, Instant em) {
        pagamento.rejeitar(new RejeicaoComprovante(motivo, em));
        statusPagamento = pagamento.getStatus();
        adicionarHistorico(ContratoEventoTipo.PAGAMENTO_REJEITADO,
                "Comprovante rejeitado: " + motivo, em);
        auditoria.marcarAtualizacao();
    }

    public void isentarEntrada() {
        pagamento.isentar();
        statusPagamento = pagamento.getStatus();
        adicionarHistorico(ContratoEventoTipo.PAGAMENTO_CONFIRMADO,
                "Entrada marcada como isenta.", Instant.now());
        auditoria.marcarAtualizacao();
    }

    public void registrarPagamentoFinal(BigDecimal valor, FormaPagamentoSaldo forma,
                                        Instant em, String por, String observacoes) {
        pagamentoFinal.receber(valor, forma, em, por, observacoes);
        statusPagamentoFinal = pagamentoFinal.getStatus();
        adicionarHistorico(ContratoEventoTipo.SALDO_RECEBIDO,
                "Saldo final de %s recebido via %s.".formatted(
                        valor == null ? "0,00" : valor.toPlainString(),
                        forma == null ? "outro" : forma.getValor()),
                em);
        auditoria.marcarAtualizacao();
    }

    public void isentarPagamentoFinal() {
        pagamentoFinal.isentar();
        statusPagamentoFinal = pagamentoFinal.getStatus();
        adicionarHistorico(ContratoEventoTipo.SALDO_ISENTO, "Saldo final marcado como isento.", Instant.now());
        auditoria.marcarAtualizacao();
    }

    public void reabrirPagamentoFinal() {
        if (statusPagamentoFinal == PagamentoStatus.CONFIRMADO) {
            throw new RegraDeNegocioException("SALDO_CONFIRMADO",
                    "Saldo final confirmado nao pode ser reaberto.");
        }
        pagamentoFinal.reabrir();
        statusPagamentoFinal = pagamentoFinal.getStatus();
        adicionarHistorico(ContratoEventoTipo.REABERTO, "Saldo final reaberto.", Instant.now());
        auditoria.marcarAtualizacao();
    }

    public void validarExclusao() {
        validarRascunho();
    }

    public void adicionarHistorico(ContratoEventoTipo tipo, String detalhe) {
        adicionarHistorico(tipo, detalhe, Instant.now());
    }

    private void adicionarHistorico(ContratoEventoTipo tipo, String detalhe, Instant em) {
        if (historico == null) {
            historico = new ArrayList<>();
        }
        historico = new ArrayList<>(historico);
        historico.add(new ContratoEvento(UUID.randomUUID().toString(), tipo, em, detalhe));
    }

    private void validarRascunho() {
        if (statusAceite != StatusAceite.RASCUNHO) {
            throw new RegraDeNegocioException("CONTRATO_NAO_EDITAVEL",
                    "Somente contratos em rascunho podem ser alterados ou excluidos.");
        }
    }

    private void aplicarDados(DadosContrato dados) {
        this.clienteId = dados.clienteId();
        this.agendamentoId = dados.agendamentoId();
        this.pacoteId = dados.pacoteId();
        this.clienteNome = dados.snapshot() == null || dados.snapshot().cliente() == null
                ? null
                : dados.snapshot().cliente().nome();
        this.pacoteNome = dados.snapshot() == null || dados.snapshot().pacote() == null
                ? null
                : dados.snapshot().pacote().nome();
        this.modeloId = dados.modeloId();
        this.titulo = dados.titulo();
        this.clausulas = dados.clausulas() == null ? List.of() : List.copyOf(dados.clausulas());
        this.clausulasModelo = dados.clausulasModelo() == null ? List.of() : List.copyOf(dados.clausulasModelo());
        this.snapshot = dados.snapshot();
        this.valorTotal = CalculadoraPagamento.normalizar(dados.valorTotal());
        this.valorEntrada = CalculadoraPagamento.normalizar(dados.valorEntrada());
        this.percentualEntrada = CalculadoraPagamento.normalizar(dados.percentualEntrada());
        this.formaPagamento = dados.formaPagamento();
        this.condicoesPagamento = dados.condicoesPagamento();
        this.dataEmissao = dados.dataEmissao() == null ? Instant.now() : dados.dataEmissao();
        this.dataValidade = dados.dataValidade();
        this.observacoes = dados.observacoes();
    }
}
