package com.ranny.fotografia.agenda.api;

import com.ranny.fotografia.agenda.model.StatusAgendamento;
import com.ranny.fotografia.agenda.service.AgendaService;
import com.ranny.fotografia.shared.api.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.List;

/** Thin Controller da agenda. */
@RestController
@RequestMapping("/api/v1/agenda")
@Tag(name = "Agenda", description = "Ensaios, reservas, status e pagamentos")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping
    @Operation(summary = "Lista agendamentos de forma paginada por periodo e/ou status")
    public PaginaResponse<AgendamentoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim,
            @RequestParam(required = false) StatusAgendamento status,
            @Parameter(description = "Pagina 0-based")
            @RequestParam(required = false) Integer pagina,
            @Parameter(description = "Tamanho da pagina (1 a 100, default 20)")
            @RequestParam(required = false) Integer tamanho) {
        return agendaService.listarPaginado(inicio, fim, status, pagina, tamanho);
    }

    @GetMapping("/calendario")
    @Operation(summary = "Lista os agendamentos de um periodo para o calendario")
    public List<AgendamentoResponse> calendario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim,
            @RequestParam(required = false) StatusAgendamento status) {
        return agendaService.listar(inicio, fim, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um agendamento")
    public AgendamentoResponse buscar(@PathVariable String id) {
        return agendaService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cria uma reserva e congela as condicoes do pacote")
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody AgendamentoRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        AgendamentoResponse criado = agendaService.criar(request);
        var location = uriBuilder.path("/api/v1/agenda/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um agendamento")
    public AgendamentoResponse atualizar(@PathVariable String id,
                                         @Valid @RequestBody AgendamentoRequest request) {
        return agendaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um agendamento sem contrato vinculado")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        agendaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera o status do agendamento")
    public AgendamentoResponse alterarStatus(@PathVariable String id,
                                             @Valid @RequestBody AgendaRequests.StatusRequest request) {
        return agendaService.alterarStatus(id, request.status());
    }

    @PutMapping("/{id}/pagamento-entrada")
    @Operation(summary = "Atualiza o pagamento de entrada")
    public AgendamentoResponse atualizarPagamentoEntrada(
            @PathVariable String id,
            @Valid @RequestBody AgendaRequests.PagamentoEntradaRequest request) {
        return agendaService.atualizarPagamentoEntrada(id, request);
    }

    @PutMapping("/{id}/pagamento-final")
    @Operation(summary = "Atualiza o pagamento final")
    public AgendamentoResponse atualizarPagamentoFinal(
            @PathVariable String id,
            @Valid @RequestBody AgendaRequests.PagamentoFinalRequest request) {
        return agendaService.atualizarPagamentoFinal(id, request);
    }
}
