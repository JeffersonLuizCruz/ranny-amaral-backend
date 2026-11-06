package com.ranny.fotografia.dashboard.service;

import com.ranny.fotografia.agenda.AgendamentoConsulta;
import com.ranny.fotografia.agenda.AgendamentoConsulta.ResumoAgendamento;
import com.ranny.fotografia.cliente.ClienteConsulta;
import com.ranny.fotografia.cliente.ClienteResumo;
import com.ranny.fotografia.contrato.ContratoConsulta;
import com.ranny.fotografia.dashboard.api.DashboardDtos;
import com.ranny.fotografia.financeiro.FinanceiroConsulta;
import com.ranny.fotografia.galeria.GaleriaConsulta;
import com.ranny.fotografia.shared.exception.RequisicaoInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read model do dashboard operacional.
 *
 * <p>PATTERN: Application Service + Read Model.
 *
 * <p>Agrega agenda, contratos, galerias e financeiro em uma unica chamada. Cada
 * modulo continua dono dos seus dados; o dashboard usa apenas as APIs publicas.
 */
@Service
public class DashboardService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int DIAS_PROXIMOS = 14;
    private static final int LIMITE_PROXIMOS = 8;

    private final AgendamentoConsulta agendamentoConsulta;
    private final ContratoConsulta contratoConsulta;
    private final GaleriaConsulta galeriaConsulta;
    private final FinanceiroConsulta financeiroConsulta;
    private final ClienteConsulta clienteConsulta;

    public DashboardService(AgendamentoConsulta agendamentoConsulta,
                            ContratoConsulta contratoConsulta,
                            GaleriaConsulta galeriaConsulta,
                            FinanceiroConsulta financeiroConsulta,
                            ClienteConsulta clienteConsulta) {
        this.agendamentoConsulta = agendamentoConsulta;
        this.contratoConsulta = contratoConsulta;
        this.galeriaConsulta = galeriaConsulta;
        this.financeiroConsulta = financeiroConsulta;
        this.clienteConsulta = clienteConsulta;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.DashboardResponse resumo(String data) {
        LocalDate dia = data(data);
        OffsetDateTime inicioDia = dia.atStartOfDay(FUSO).toOffsetDateTime();
        OffsetDateTime fimDia = dia.plusDays(1).atStartOfDay(FUSO)
                .toOffsetDateTime().minusNanos(1);
        OffsetDateTime limite = dia.plusDays(DIAS_PROXIMOS)
                .atTime(23, 59, 59).atZone(FUSO).toOffsetDateTime();
        OffsetDateTime agora = OffsetDateTime.now(FUSO);

        List<ResumoAgendamento> periodo = agendamentoConsulta
                .listarPorPeriodo(inicioDia, limite)
                .stream()
                .filter(agendamento -> !agendamento.cancelado())
                .sorted(Comparator.comparing(ResumoAgendamento::dataHoraInicio))
                .toList();

        int ensaiosHoje = (int) periodo.stream()
                .filter(agendamento -> !agendamento.dataHoraInicio().isBefore(inicioDia))
                .filter(agendamento -> !agendamento.dataHoraInicio().isAfter(fimDia))
                .count();
        int ensaiosSemana = (int) periodo.stream()
                .filter(agendamento -> agendamento.dataHoraInicio()
                        .isBefore(dia.plusDays(7).atStartOfDay(FUSO).toOffsetDateTime()))
                .count();

        List<DashboardDtos.ProximoEnsaioResponse> proximos = periodo.stream()
                .filter(agendamento -> agendamento.dataHoraFim() == null
                        || agendamento.dataHoraFim().isAfter(agora))
                .limit(LIMITE_PROXIMOS)
                .map(this::proximoEnsaio)
                .toList();

        ContratoConsulta.ResumoPendentes contratos = contratoConsulta.resumoPendentes();
        GaleriaConsulta.ResumoOperacional galerias = galeriaConsulta.resumoOperacional();
        FinanceiroConsulta.ResumoDashboard financeiro =
                financeiroConsulta.resumoParaDashboard(dia.toString().substring(0, 7));

        DashboardDtos.ResumoResponse resumo = new DashboardDtos.ResumoResponse(
                ensaiosHoje,
                ensaiosSemana,
                contratos.aguardandoAceite(),
                contratos.entradaPendente(),
                galerias.emSelecao(),
                galerias.selecaoEnviada(),
                galerias.emEdicao(),
                galerias.prontaEntrega(),
                galerias.extrasPendentes());

        return new DashboardDtos.DashboardResponse(
                dia.toString(),
                resumo,
                proximos,
                pendencias(resumo),
                new DashboardDtos.FinanceiroResumoResponse(
                        financeiro.mes(),
                        financeiro.totalPrevisto(),
                        financeiro.totalRecebido(),
                        financeiro.totalAReceber(),
                        financeiro.quantidadePendentes(),
                        financeiro.quantidadeExtrasPendentes()));
    }

    private DashboardDtos.ProximoEnsaioResponse proximoEnsaio(ResumoAgendamento agendamento) {
        return new DashboardDtos.ProximoEnsaioResponse(
                agendamento.id(),
                agendamento.clienteId(),
                clienteNome(agendamento),
                agendamento.pacoteId(),
                agendamento.dataHoraInicio(),
                agendamento.dataHoraFim(),
                agendamento.statusValor());
    }

    private String clienteNome(ResumoAgendamento agendamento) {
        if (agendamento.clienteId() != null) {
            return clienteConsulta.buscar(agendamento.clienteId())
                    .map(ClienteResumo::nome)
                    .orElseGet(() -> contatoNome(agendamento));
        }
        return contatoNome(agendamento);
    }

    private static String contatoNome(ResumoAgendamento agendamento) {
        return agendamento.contato() == null ? null : agendamento.contato().nome();
    }

    private static List<DashboardDtos.PendenciaResponse> pendencias(
            DashboardDtos.ResumoResponse resumo) {
        List<DashboardDtos.PendenciaResponse> pendencias = new ArrayList<>();
        if (resumo.contratosAguardandoAceite() > 0) {
            pendencias.add(new DashboardDtos.PendenciaResponse(
                    "CONTRATO", "Contratos aguardando aceite",
                    resumo.contratosAguardandoAceite() + " contrato(s) pendente(s) de aceite.",
                    "ALTA", "/contratos"));
        }
        if (resumo.contratosEntradaPendente() > 0) {
            pendencias.add(new DashboardDtos.PendenciaResponse(
                    "FINANCEIRO", "Entradas pendentes",
                    resumo.contratosEntradaPendente() + " contrato(s) com entrada pendente.",
                    "ALTA", "/financeiro"));
        }
        if (resumo.extrasPendentes() > 0) {
            pendencias.add(new DashboardDtos.PendenciaResponse(
                    "GALERIA", "Extras aguardando confirmação",
                    resumo.extrasPendentes() + " pedido(s) de extras pendente(s).",
                    "ALTA", "/galerias"));
        }
        if (resumo.galeriasAguardandoEdicao() > 0) {
            pendencias.add(new DashboardDtos.PendenciaResponse(
                    "GALERIA", "Seleções prontas para edição",
                    resumo.galeriasAguardandoEdicao() + " galeria(s) com seleção enviada.",
                    "MEDIA", "/galerias"));
        }
        if (resumo.galeriasEmEdicao() > 0) {
            pendencias.add(new DashboardDtos.PendenciaResponse(
                    "GALERIA", "Galerias em edição",
                    resumo.galeriasEmEdicao() + " galeria(s) em edição.",
                    "MEDIA", "/galerias"));
        }
        if (resumo.galeriasProntasEntrega() > 0) {
            pendencias.add(new DashboardDtos.PendenciaResponse(
                    "GALERIA", "Galerias prontas para entrega",
                    resumo.galeriasProntasEntrega() + " galeria(s) aguardando publicação.",
                    "MEDIA", "/galerias"));
        }
        if (resumo.galeriasEmSelecao() > 0) {
            pendencias.add(new DashboardDtos.PendenciaResponse(
                    "GALERIA", "Galerias em seleção",
                    resumo.galeriasEmSelecao() + " galeria(s) aguardando o cliente.",
                    "BAIXA", "/galerias"));
        }
        return pendencias;
    }

    private static LocalDate data(String data) {
        if (data == null || data.isBlank()) {
            return LocalDate.now(FUSO);
        }
        try {
            return LocalDate.parse(data.trim());
        } catch (DateTimeParseException ex) {
            throw new RequisicaoInvalidaException("DATA_INVALIDA",
                    "Informe a data no formato YYYY-MM-DD.");
        }
    }
}
