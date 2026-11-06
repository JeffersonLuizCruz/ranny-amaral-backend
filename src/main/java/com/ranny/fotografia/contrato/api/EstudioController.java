package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.service.EstudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin Controller da configuracao unica do estudio. */
@RestController
@RequestMapping("/api/v1/estudio")
@Tag(name = "Estudio", description = "Configuracao do estudio para contratos")
public class EstudioController {

    private final EstudioService estudioService;

    public EstudioController(EstudioService estudioService) {
        this.estudioService = estudioService;
    }

    @GetMapping
    @Operation(summary = "Obtem a configuracao do estudio (cria a padrao se necessario)")
    public EstudioResponse obter() {
        return EstudioResponse.de(estudioService.obter());
    }

    @PutMapping
    @Operation(summary = "Atualiza a configuracao do estudio")
    public EstudioResponse atualizar(@Valid @RequestBody EstudioRequest request) {
        return EstudioResponse.de(estudioService.atualizar(request));
    }
}
