package com.ranny.fotografia.pacote.api;

import com.ranny.fotografia.pacote.service.PacoteService;
import com.ranny.fotografia.shared.api.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** Thin Controller do catalogo de pacotes. */
@RestController
@RequestMapping("/api/v1/pacotes")
@Tag(name = "Pacotes", description = "Catalogo de pacotes fotograficos")
public class PacoteController {

    private final PacoteService pacoteService;

    public PacoteController(PacoteService pacoteService) {
        this.pacoteService = pacoteService;
    }

    @GetMapping
    @Operation(summary = "Lista pacotes de forma paginada, filtrando por situacao e texto")
    public PaginaResponse<PacoteResponse> listar(
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String q,
            @Parameter(description = "Pagina 0-based")
            @RequestParam(required = false) Integer pagina,
            @Parameter(description = "Tamanho da pagina (1 a 100, default 20)")
            @RequestParam(required = false) Integer tamanho) {
        return pacoteService.listar(ativo, q, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um pacote")
    public PacoteResponse buscar(@PathVariable String id) {
        return pacoteService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um pacote")
    public ResponseEntity<PacoteResponse> criar(@Valid @RequestBody PacoteRequest request,
                                                UriComponentsBuilder uriBuilder) {
        PacoteResponse criado = pacoteService.criar(request);
        var location = uriBuilder.path("/api/v1/pacotes/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um pacote")
    public PacoteResponse atualizar(@PathVariable String id,
                                    @Valid @RequestBody PacoteRequest request) {
        return pacoteService.atualizar(id, request);
    }

    @PatchMapping("/{id}/ativo")
    @Operation(summary = "Alterna a situacao ativo/inativo do pacote")
    public PacoteResponse alternarAtivo(@PathVariable String id) {
        return pacoteService.alternarAtivo(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um pacote sem agendamentos vinculados")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        pacoteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
