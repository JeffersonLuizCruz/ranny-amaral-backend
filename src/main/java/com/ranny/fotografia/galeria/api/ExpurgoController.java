package com.ranny.fotografia.galeria.api;

import com.ranny.fotografia.galeria.service.ExpurgoGaleriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Armazenamento e expurgo de uma galeria.
 *
 * <p>Leitura liberada para ADMIN e GESTOR; o expurgo e exclusivo de ADMIN.
 */
@RestController
@RequestMapping("/api/v1/galerias/{galeriaId}")
@Tag(name = "Armazenamento", description = "Tamanho por galeria e expurgo de binarios")
public class ExpurgoController {

    private final ExpurgoGaleriaService expurgoService;

    public ExpurgoController(ExpurgoGaleriaService expurgoService) {
        this.expurgoService = expurgoService;
    }

    @GetMapping("/armazenamento")
    @Operation(summary = "Detalha o armazenamento da galeria e a elegibilidade do expurgo")
    public ExpurgoDtos.ArmazenamentoGaleria detalhar(@PathVariable String galeriaId) {
        return expurgoService.detalhe(galeriaId);
    }

    @PostMapping("/expurgo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Agenda o expurgo dos binarios da galeria")
    public ExpurgoDtos.ExpurgoResponse expurgar(
            @PathVariable String galeriaId,
            @Valid @RequestBody ExpurgoDtos.ExpurgoRequest request,
            Authentication authentication) {
        return expurgoService.solicitar(galeriaId, request.motivo(), request.confirmacao(),
                ator(authentication));
    }

    @DeleteMapping("/expurgo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancela um expurgo agendado")
    public ResponseEntity<Void> cancelar(@PathVariable String galeriaId) {
        expurgoService.cancelar(galeriaId);
        return ResponseEntity.noContent().build();
    }

    private static String ator(Authentication authentication) {
        return authentication == null || authentication.getName() == null
                ? "Administrador" : authentication.getName();
    }
}
