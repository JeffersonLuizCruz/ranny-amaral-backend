package com.ranny.fotografia.armazenamento.api;

import com.ranny.fotografia.armazenamento.service.ArmazenamentoService;
import com.ranny.fotografia.armazenamento.service.ColetorArmazenamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Rotas de monitoramento de armazenamento. */
@RestController
@RequestMapping("/api/v1/armazenamento")
@Tag(name = "Armazenamento", description = "Consumo de disco e reconciliacao banco x bucket")
public class ArmazenamentoController {

    private final ArmazenamentoService service;
    private final ColetorArmazenamento coletor;

    public ArmazenamentoController(ArmazenamentoService service,
                                   ColetorArmazenamento coletor) {
        this.service = service;
        this.coletor = coletor;
    }

    @GetMapping
    @Operation(summary = "Resumo do disco, do CRM e da infraestrutura")
    public ArmazenamentoDtos.ResumoResponse resumo() {
        return service.resumo();
    }

    @GetMapping("/galerias")
    @Operation(summary = "Tamanho por galeria e elegibilidade do expurgo")
    public List<ArmazenamentoDtos.GaleriaArmazenamento> galerias() {
        return service.galerias();
    }

    @PostMapping("/coletar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Forca uma coleta imediata")
    public ArmazenamentoDtos.ResumoResponse coletar() {
        coletor.coletar();
        return service.resumo();
    }

    @PostMapping("/reconciliar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Compara os objetos do bucket com o catalogo do banco")
    public ArmazenamentoDtos.ReconciliacaoResponse reconciliar() {
        return service.reconciliar();
    }
}
