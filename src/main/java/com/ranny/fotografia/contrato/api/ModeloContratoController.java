package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.service.ModeloContratoService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/** Thin Controller dos modelos de contrato. */
@RestController
@RequestMapping("/api/v1/modelos-contrato")
@Tag(name = "Modelos de contrato", description = "Templates de clausulas contratuais")
public class ModeloContratoController {

    private final ModeloContratoService modeloService;

    public ModeloContratoController(ModeloContratoService modeloService) {
        this.modeloService = modeloService;
    }

    @GetMapping
    @Operation(summary = "Lista os modelos de contrato")
    public List<ModeloContratoResponse> listar() {
        return modeloService.listar().stream().map(ModeloContratoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um modelo de contrato")
    public ModeloContratoResponse buscar(@PathVariable String id) {
        return ModeloContratoResponse.de(modeloService.obter(id));
    }

    @PostMapping
    @Operation(summary = "Cria um modelo de contrato")
    public ResponseEntity<ModeloContratoResponse> criar(@Valid @RequestBody ModeloContratoRequest request,
                                                        UriComponentsBuilder uriBuilder) {
        ModeloContratoResponse criado = ModeloContratoResponse.de(modeloService.criar(request));
        var location = uriBuilder.path("/api/v1/modelos-contrato/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um modelo de contrato (incrementa a versao)")
    public ModeloContratoResponse atualizar(@PathVariable String id,
                                            @Valid @RequestBody ModeloContratoRequest request) {
        return ModeloContratoResponse.de(modeloService.atualizar(id, request));
    }

    @PatchMapping("/{id}/ativo")
    @Operation(summary = "Alterna a situacao do modelo")
    public ModeloContratoResponse alternarAtivo(@PathVariable String id) {
        return ModeloContratoResponse.de(modeloService.alternarAtivo(id));
    }

    @PostMapping("/{id}/duplicar")
    @Operation(summary = "Duplica um modelo de contrato")
    public ResponseEntity<ModeloContratoResponse> duplicar(@PathVariable String id,
                                                           UriComponentsBuilder uriBuilder) {
        ModeloContratoResponse duplicado = ModeloContratoResponse.de(modeloService.duplicar(id));
        var location = uriBuilder.path("/api/v1/modelos-contrato/{id}")
                .buildAndExpand(duplicado.id()).toUri();
        return ResponseEntity.created(location).body(duplicado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um modelo de contrato")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        modeloService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
