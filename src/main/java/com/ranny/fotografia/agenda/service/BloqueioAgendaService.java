package com.ranny.fotografia.agenda.service;

import com.ranny.fotografia.agenda.api.BloqueioAgendaRequest;
import com.ranny.fotografia.agenda.api.BloqueioAgendaResponse;
import com.ranny.fotografia.agenda.model.Agendamento;
import com.ranny.fotografia.agenda.model.BloqueioAgenda;
import com.ranny.fotografia.agenda.model.StatusAgendamento;
import com.ranny.fotografia.agenda.repository.AgendamentoRepository;
import com.ranny.fotografia.agenda.repository.BloqueioAgendaRepository;
import com.ranny.fotografia.shared.exception.RequisicaoInvalidaException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Application Service dos bloqueios pontuais da agenda.
 *
 * <p>Garante a simetria da regra: um ensaio nao pode nascer/entrar em um
 * bloqueio e um bloqueio nao pode ser criado sobre um ensaio ativo. Ensaios
 * cancelados nao ocupam o horario.
 */
@Service
public class BloqueioAgendaService {

    private static final ZoneId FUSO_ESTUDIO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT);
    private static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private final BloqueioAgendaRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;

    public BloqueioAgendaService(BloqueioAgendaRepository bloqueioRepository,
                                 AgendamentoRepository agendamentoRepository) {
        this.bloqueioRepository = bloqueioRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<BloqueioAgendaResponse> listar(OffsetDateTime inicio, OffsetDateTime fim) {
        validarPeriodo(inicio, fim);
        return bloqueioRepository
                .findByInicioLessThanAndFimGreaterThanOrderByInicioAsc(fim, inicio)
                .stream()
                .map(BloqueioAgendaResponse::de)
                .toList();
    }

    @Transactional
    public BloqueioAgendaResponse criar(BloqueioAgendaRequest request) {
        Janela janela = montarJanela(request);
        validarConflitoComAgendamentos(janela);
        validarSobreposicao(janela, null);

        BloqueioAgenda bloqueio = BloqueioAgenda.criar(
                janela.inicio(), janela.fim(), janela.diaInteiro(), request.motivo());
        return BloqueioAgendaResponse.de(bloqueioRepository.save(bloqueio));
    }

    @Transactional
    public BloqueioAgendaResponse atualizar(String id, BloqueioAgendaRequest request) {
        BloqueioAgenda bloqueio = obter(id);
        Janela janela = montarJanela(request);
        validarConflitoComAgendamentos(janela);
        validarSobreposicao(janela, id);

        bloqueio.atualizar(janela.inicio(), janela.fim(), janela.diaInteiro(), request.motivo());
        return BloqueioAgendaResponse.de(bloqueio);
    }

    @Transactional
    public void excluir(String id) {
        bloqueioRepository.delete(obter(id));
    }

    private BloqueioAgenda obter(String id) {
        return bloqueioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bloqueio de agenda", id));
    }

    private static void validarPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {
        if ((inicio == null) != (fim == null)) {
            throw new RequisicaoInvalidaException("PERIODO_INCOMPLETO",
                    "Informe inicio e fim juntos para filtrar bloqueios.");
        }
        if (inicio == null) {
            throw new RequisicaoInvalidaException("PERIODO_OBRIGATORIO",
                    "Informe o periodo para listar os bloqueios.");
        }
    }

    private static Janela montarJanela(BloqueioAgendaRequest request) {
        if (request.diaInteiro()) {
            return new Janela(
                    request.data().atStartOfDay(FUSO_ESTUDIO).toOffsetDateTime(),
                    request.data().plusDays(1).atStartOfDay(FUSO_ESTUDIO).toOffsetDateTime(),
                    true);
        }

        if (request.horaInicio() == null || request.horaFim() == null) {
            throw new RegraDeNegocioException("HORARIO_OBRIGATORIO",
                    "Informe o horario de inicio e de fim do bloqueio.");
        }

        LocalTime horaInicio;
        LocalTime horaFim;
        try {
            horaInicio = LocalTime.parse(request.horaInicio(), HORA);
            horaFim = LocalTime.parse(request.horaFim(), HORA);
        } catch (DateTimeParseException ex) {
            throw new RegraDeNegocioException("HORARIO_INVALIDO",
                    "Informe os horarios no formato HH:mm.");
        }

        if (!horaFim.isAfter(horaInicio)) {
            throw new RegraDeNegocioException("PERIODO_INVALIDO",
                    "O horario de fim deve ser posterior ao horario de inicio.");
        }

        OffsetDateTime inicio = ZonedDateTime.of(request.data(), horaInicio, FUSO_ESTUDIO)
                .toOffsetDateTime();
        OffsetDateTime fim = ZonedDateTime.of(request.data(), horaFim, FUSO_ESTUDIO)
                .toOffsetDateTime();
        return new Janela(inicio, fim, false);
    }

    private void validarConflitoComAgendamentos(Janela janela) {
        List<Agendamento> conflitos = agendamentoRepository
                .findByDataHoraInicioLessThanAndDataHoraFimGreaterThanAndStatusNotOrderByDataHoraInicioAsc(
                        janela.fim(), janela.inicio(), StatusAgendamento.CANCELADO);
        if (conflitos.isEmpty()) {
            return;
        }

        String periodos = conflitos.stream()
                .limit(3)
                .map(BloqueioAgendaService::descrever)
                .collect(Collectors.joining("; "));
        if (conflitos.size() > 3) {
            periodos += "; ...";
        }
        throw new RegraDeNegocioException("BLOQUEIO_CONFLITA_COM_AGENDAMENTO",
                "Ja existe ensaio ativo no periodo: " + periodos
                        + ". Cancele ou remarque o ensaio antes de bloquear.");
    }

    private void validarSobreposicao(Janela janela, String ignorarId) {
        boolean sobreposto = bloqueioRepository
                .findByInicioLessThanAndFimGreaterThanOrderByInicioAsc(janela.fim(), janela.inicio())
                .stream()
                .anyMatch(bloqueio -> !bloqueio.getId().equals(ignorarId)
                        && bloqueio.sobrepoe(janela.inicio(), janela.fim()));
        if (sobreposto) {
            throw new RegraDeNegocioException("BLOQUEIO_SOBREPOSTO",
                    "Ja existe um bloqueio nesse periodo. Edite ou exclua o bloqueio existente.");
        }
    }

    private static String descrever(Agendamento agendamento) {
        var inicio = agendamento.getDataHoraInicio().atZoneSameInstant(FUSO_ESTUDIO);
        var fim = agendamento.getDataHoraFim().atZoneSameInstant(FUSO_ESTUDIO);
        return DATA_HORA.format(inicio) + " a " + HORA.format(fim);
    }

    private record Janela(OffsetDateTime inicio, OffsetDateTime fim, boolean diaInteiro) {
    }
}
