package com.ranny.fotografia.dashboard.api;

import com.ranny.fotografia.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rotas do dashboard operacional. */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Visao operacional do estudio")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Resume agenda, contratos, galerias e financeiro do dia")
    public DashboardDtos.DashboardResponse resumo(
            @RequestParam(required = false) String data) {
        return dashboardService.resumo(data);
    }
}
