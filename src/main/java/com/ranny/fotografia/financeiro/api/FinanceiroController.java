package com.ranny.fotografia.financeiro.api;

import com.ranny.fotografia.financeiro.service.FinanceiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta financeira consolidada por competencia mensal. */
@RestController
@RequestMapping("/api/v1/financeiro")
@Tag(name = "Financeiro", description = "Resumo mensal de entradas, saldos e extras")
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    public FinanceiroController(FinanceiroService financeiroService) {
        this.financeiroService = financeiroService;
    }

    @GetMapping("/mensal")
    @Operation(summary = "Resumo financeiro de um mes no formato YYYY-MM")
    public FinanceiroDtos.ResumoMensalResponse mensal(
            @RequestParam(required = false) String mes) {
        return financeiroService.resumoMensal(mes);
    }
}
