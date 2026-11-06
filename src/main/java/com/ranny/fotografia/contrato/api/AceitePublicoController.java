package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.service.AceitePublicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas publicas do aceite.
 *
 * <p>PATTERN: Chain of Responsibility (filtros do servlet) para captura de IP e
 * user-agent. O controller apenas repassa os metadados de auditoria; toda a
 * regra fica no service.
 */
@RestController
@RequestMapping("/api/v1/public/contratos")
@Tag(name = "Aceite publico", description = "Consulta e aceite de contratos por token")
public class AceitePublicoController {

    private final AceitePublicoService aceiteService;

    public AceitePublicoController(AceitePublicoService aceiteService) {
        this.aceiteService = aceiteService;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Carrega o contrato e a solicitacao pelo token publico")
    public AceitePublicoDtos.ContratoPublicoResponse consultar(@PathVariable String token) {
        return aceiteService.consultar(token);
    }

    @PostMapping("/{token}/visualizar")
    @Operation(summary = "Registra a visualizacao do link")
    public ResponseEntity<Void> visualizar(@PathVariable String token) {
        aceiteService.visualizar(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{token}/aceite")
    @Operation(summary = "Registra o aceite eletronico e cria/vincula o cliente")
    public AceitePublicoDtos.AceiteResponse aceitar(@PathVariable String token,
                                                    @Valid @RequestBody AceitePublicoDtos.AceiteRequest request,
                                                    HttpServletRequest http) {
        return aceiteService.aceitar(token, request, ip(http), userAgent(http));
    }

    @PostMapping("/{token}/comprovante")
    @Operation(summary = "Registra o envio de comprovante de entrada")
    public AceitePublicoDtos.ComprovanteResponse enviarComprovante(
            @PathVariable String token,
            @Valid @RequestBody AceitePublicoDtos.ComprovanteRequest request) {
        return aceiteService.enviarComprovante(token, request);
    }

    private static String ip(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest request) {
        String agente = request.getHeader("User-Agent");
        return agente == null ? null : agente.substring(0, Math.min(agente.length(), 300));
    }
}
