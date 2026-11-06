package com.ranny.fotografia.financeiro.service;

import com.ranny.fotografia.agenda.AgendamentoConsulta;
import com.ranny.fotografia.agenda.AgendamentoConsulta.ResumoAgendamento;
import com.ranny.fotografia.cliente.ClienteConsulta;
import com.ranny.fotografia.contrato.ContratoConsulta;
import com.ranny.fotografia.contrato.ContratoConsulta.ResumoFinanceiro;
import com.ranny.fotografia.financeiro.api.FinanceiroDtos;
import com.ranny.fotografia.galeria.GaleriaConsulta;
import com.ranny.fotografia.galeria.GaleriaConsulta.ResumoExtras;
import com.ranny.fotografia.galeria.GaleriaConsulta.StatusExtras;
import com.ranny.fotografia.pacote.PacoteConsulta;
import com.ranny.fotografia.shared.exception.RequisicaoInvalidaException;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Read model financeiro mensal.
 *
 * <p>PATTERN: Application Service + Read Model.
 *
 * <p>Agenda define a competencia; contrato tem prioridade sobre os valores do
 * agendamento; extras vem da galeria. Nenhum valor e recalculado no frontend.
 */
@Service
public class FinanceiroService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final AgendamentoConsulta agendamentoConsulta;
    private final ContratoConsulta contratoConsulta;
    private final GaleriaConsulta galeriaConsulta;
    private final ClienteConsulta clienteConsulta;
    private final PacoteConsulta pacoteConsulta;

    public FinanceiroService(AgendamentoConsulta agendamentoConsulta,
                             ContratoConsulta contratoConsulta,
                             GaleriaConsulta galeriaConsulta,
                             ClienteConsulta clienteConsulta,
                             PacoteConsulta pacoteConsulta) {
        this.agendamentoConsulta = agendamentoConsulta;
        this.contratoConsulta = contratoConsulta;
        this.galeriaConsulta = galeriaConsulta;
        this.clienteConsulta = clienteConsulta;
        this.pacoteConsulta = pacoteConsulta;
    }

    @Transactional(readOnly = true)
    public FinanceiroDtos.ResumoMensalResponse resumoMensal(String mes) {
        YearMonth competencia = competencia(mes);
        OffsetDateTime inicio = competencia.atDay(1).atStartOfDay(FUSO).toOffsetDateTime();
        OffsetDateTime fim = competencia.atEndOfMonth()
                .atTime(23, 59, 59).atZone(FUSO).toOffsetDateTime();

        List<ResumoAgendamento> agendamentos = agendamentoConsulta
                .listarPorPeriodo(inicio, fim)
                .stream()
                .filter(agendamento -> !agendamento.cancelado())
                .toList();

        List<String> ids = agendamentos.stream().map(ResumoAgendamento::id).toList();
        Map<String, ResumoFinanceiro> contratos = contratoConsulta.buscarPorAgendamentos(ids);
        Map<String, ResumoExtras> extras = galeriaConsulta.buscarExtrasPorAgendamentos(ids);
        Map<String, String> clientes = new HashMap<>();
        Map<String, String> pacotes = new HashMap<>();

        List<FinanceiroDtos.EntradaResponse> entradas = new ArrayList<>();
        for (ResumoAgendamento agendamento : agendamentos) {
            entradas.add(entrada(agendamento, contratos.get(agendamento.id()),
                    extras.get(agendamento.id()), clientes, pacotes));
        }

        BigDecimal totalPrevisto = soma(entradas, FinanceiroDtos.EntradaResponse::valorTotal)
                .add(soma(entradas, FinanceiroDtos.EntradaResponse::valorExtras));
        BigDecimal totalRecebido = soma(entradas, FinanceiroDtos.EntradaResponse::recebido);
        BigDecimal totalAReceber = totalPrevisto.subtract(totalRecebido).max(BigDecimal.ZERO);
        BigDecimal totalEntradasRecebidas = soma(entradas,
                FinanceiroDtos.EntradaResponse::recebidoEntrada);
        BigDecimal totalEntradasPendentes = entradas.stream()
                .filter(entrada -> "pendente".equals(entrada.statusEntrada()))
                .map(FinanceiroDtos.EntradaResponse::valorEntrada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaldos = entradas.stream()
                .filter(entrada -> "pendente".equals(entrada.statusFinal()))
                .map(FinanceiroDtos.EntradaResponse::saldoRestante)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaldosRecebidos = soma(entradas,
                FinanceiroDtos.EntradaResponse::recebidoFinal);
        BigDecimal totalExtras = soma(entradas, FinanceiroDtos.EntradaResponse::valorExtras);
        BigDecimal totalExtrasRecebidos = soma(entradas,
                FinanceiroDtos.EntradaResponse::recebidoExtras);
        BigDecimal totalExtrasPendentes = entradas.stream()
                .filter(entrada -> "pendente".equals(entrada.statusExtras()))
                .map(FinanceiroDtos.EntradaResponse::valorExtras)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int quantidade = entradas.size();
        int quantidadeRecebidas = (int) entradas.stream()
                .filter(entrada -> "confirmado".equals(entrada.statusEntrada()))
                .count();
        int quantidadePendentes = (int) entradas.stream()
                .filter(entrada -> "pendente".equals(entrada.statusEntrada()))
                .count();
        int quantidadeSaldosPendentes = (int) entradas.stream()
                .filter(entrada -> "pendente".equals(entrada.statusFinal()))
                .count();
        int quantidadeExtrasPendentes = (int) entradas.stream()
                .filter(entrada -> "pendente".equals(entrada.statusExtras()))
                .count();

        BigDecimal ticketMedio = quantidade == 0 ? BigDecimal.ZERO
                : totalPrevisto.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
        BigDecimal percentualRecebido = totalPrevisto.signum() == 0 ? BigDecimal.ZERO
                : totalRecebido.multiply(CEM).divide(totalPrevisto, 2, RoundingMode.HALF_UP);

        return new FinanceiroDtos.ResumoMensalResponse(
                competencia.toString(),
                quantidade,
                quantidadeRecebidas,
                quantidadePendentes,
                escala(totalPrevisto),
                escala(totalRecebido),
                escala(totalAReceber),
                escala(totalEntradasRecebidas),
                escala(totalEntradasPendentes),
                escala(totalSaldos),
                escala(totalSaldosRecebidos),
                escala(totalExtras),
                escala(totalExtrasRecebidos),
                escala(totalExtrasPendentes),
                quantidadeSaldosPendentes,
                quantidadeExtrasPendentes,
                ticketMedio,
                percentualRecebido,
                entradas);
    }

    private FinanceiroDtos.EntradaResponse entrada(
            ResumoAgendamento agendamento,
            ResumoFinanceiro contrato,
            ResumoExtras extras,
            Map<String, String> clientes,
            Map<String, String> pacotes) {
        BigDecimal valorTotal = contrato == null
                ? nuloParaZero(agendamento.valorAcordado())
                : nuloParaZero(contrato.valorTotal());
        BigDecimal valorEntrada = contrato == null
                ? nuloParaZero(agendamento.valorEntrada())
                : nuloParaZero(contrato.valorEntrada());
        BigDecimal percentual = contrato == null
                ? nuloParaZero(agendamento.percentualEntrada())
                : nuloParaZero(contrato.percentualEntrada());
        BigDecimal saldo = contrato == null
                ? nuloParaZero(agendamento.saldoRestante())
                : nuloParaZero(contrato.saldoRestante());
        PagamentoStatus statusEntrada = contrato == null
                ? agendamento.entradaStatus() : contrato.statusEntrada();
        PagamentoStatus statusFinal = contrato == null
                ? agendamento.finalStatus() : contrato.statusFinal();

        BigDecimal valorExtras = extras == null
                || extras.status() == StatusExtras.CANCELADO
                || extras.status() == StatusExtras.ISENTO
                ? BigDecimal.ZERO
                : nuloParaZero(extras.valorTotal());
        String statusExtras = extras == null ? null : extras.status().name().toLowerCase(Locale.ROOT);

        BigDecimal recebidoEntrada = statusEntrada == PagamentoStatus.CONFIRMADO
                ? valorEntrada : BigDecimal.ZERO;
        BigDecimal recebidoFinal = statusFinal == PagamentoStatus.CONFIRMADO
                ? saldo : BigDecimal.ZERO;
        BigDecimal recebidoExtras = extras != null && extras.status() == StatusExtras.PAGO
                ? valorExtras : BigDecimal.ZERO;
        BigDecimal recebido = recebidoEntrada.add(recebidoFinal).add(recebidoExtras);
        BigDecimal aReceber = valorTotal.add(valorExtras).subtract(recebido).max(BigDecimal.ZERO);

        return new FinanceiroDtos.EntradaResponse(
                agendamento.id(),
                contrato == null ? agendamento.contratoId() : contrato.contratoId(),
                agendamento.clienteId(),
                clienteNome(agendamento, clientes),
                agendamento.pacoteId(),
                pacoteNome(agendamento.pacoteId(), pacotes),
                agendamento.dataHoraInicio(),
                agendamento.dataHoraFim(),
                agendamento.statusValor(),
                escala(valorTotal),
                escala(valorEntrada),
                percentual,
                escala(saldo),
                valorStatus(statusEntrada),
                valorStatus(statusFinal),
                escala(valorExtras),
                statusExtras,
                escala(recebidoEntrada),
                escala(recebidoFinal),
                escala(recebidoExtras),
                escala(recebido),
                escala(aReceber));
    }

    private String clienteNome(ResumoAgendamento agendamento, Map<String, String> cache) {
        if (agendamento.clienteId() == null) {
            return agendamento.contato() == null ? null : agendamento.contato().nome();
        }
        return cache.computeIfAbsent(agendamento.clienteId(), id ->
                clienteConsulta.buscar(id)
                        .map(cliente -> cliente.nome())
                        .orElseGet(() -> agendamento.contato() == null
                                ? null : agendamento.contato().nome()));
    }

    private String pacoteNome(String pacoteId, Map<String, String> cache) {
        if (pacoteId == null) {
            return null;
        }
        return cache.computeIfAbsent(pacoteId, id ->
                pacoteConsulta.buscar(id).map(pacote -> pacote.nome()).orElse(null));
    }

    private static YearMonth competencia(String mes) {
        try {
            return mes == null || mes.isBlank()
                    ? YearMonth.now(FUSO)
                    : YearMonth.parse(mes.trim());
        } catch (RuntimeException ex) {
            throw new RequisicaoInvalidaException("MES_INVALIDO",
                    "Informe o mes no formato YYYY-MM.");
        }
    }

    private static BigDecimal soma(
            List<FinanceiroDtos.EntradaResponse> entradas,
            java.util.function.Function<FinanceiroDtos.EntradaResponse, BigDecimal> campo) {
        return entradas.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String valorStatus(PagamentoStatus status) {
        return status == null ? null : status.getValor();
    }

    private static BigDecimal nuloParaZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
