package com.ranny.fotografia.agenda.api;

import com.ranny.fotografia.agenda.service.BloqueioAgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/** CRUD dos bloqueios pontuais da agenda. */
@RestController
@RequestMapping("/api/v1/agenda/bloqueios")
@Tag(name = "Agenda", description = "Ensaios, reservas, status, pagamentos e bloqueios")
public class BloqueioAgendaController {

    private final BloqueioAgendaService bloqueioAgendaService;

    public BloqueioAgendaController(BloqueioAgendaService bloqueioAgendaService) {
        this.bloqueioAgendaService = bloqueioAgendaService;
    }

    @GetMapping
    @Operation(summary = "Lista bloqueios que intersectam o periodo")
    public List<BloqueioAgendaResponse> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim) {
        return bloqueioAgendaService.listar(inicio, fim);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bloqueia um dia inteiro ou um intervalo")
    public BloqueioAgendaResponse criar(@Valid @RequestBody BloqueioAgendaRequest request) {
        return bloqueioAgendaService.criar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um bloqueio")
    public BloqueioAgendaResponse atualizar(@PathVariable String id,
                                            @Valid @RequestBody BloqueioAgendaRequest request) {
        return bloqueioAgendaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um bloqueio")
    public void excluir(@PathVariable String id) {
        bloqueioAgendaService.excluir(id);
    }
}
