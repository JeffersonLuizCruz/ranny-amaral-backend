package com.ranny.fotografia.galeria.api;

import com.ranny.fotografia.galeria.service.GaleriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/** Rotas publicas de selecao e entrega por token. */
@RestController
@RequestMapping("/api/v1/public/galerias")
@Tag(name = "Galerias publicas", description = "Selecao e entrega por link")
public class GaleriaPublicaController {

    private final GaleriaService galeriaService;

    public GaleriaPublicaController(GaleriaService galeriaService) {
        this.galeriaService = galeriaService;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Carrega a galeria publica pelo token")
    public GaleriaDtos.GaleriaPublicaResponse consultar(@PathVariable String token) {
        return galeriaService.consultarPublico(token);
    }

    @PostMapping("/{token}/selecao")
    @Operation(summary = "Envia a selecao de fotos e videos")
    public GaleriaDtos.SelecaoResponse selecionar(
            @PathVariable String token,
            @RequestBody GaleriaDtos.SelecaoPublicaRequest request,
            HttpServletRequest http) {
        return galeriaService.submeterSelecao(token, request.fotoIds(), request.videoIds(),
                ip(http), userAgent(http));
    }

    @PostMapping("/{token}/download")
    @Operation(summary = "Gera a URL assinada de download e registra o evento")
    public GaleriaDtos.DownloadResponse baixar(
            @PathVariable String token,
            @Valid @RequestBody GaleriaDtos.DownloadRequest request,
            HttpServletRequest http) {
        return galeriaService.registrarDownload(token, request.midiaId(), request.versao(),
                ip(http), userAgent(http));
    }

    @GetMapping("/{token}/download-zip/validar")
    @Operation(summary = "Valida o ZIP antes de iniciar o streaming")
    public GaleriaDtos.ZipValidacaoResponse validarZip(
            @PathVariable String token,
            @RequestParam(required = false, defaultValue = "editada") String versao) {
        return new GaleriaDtos.ZipValidacaoResponse(galeriaService.validarZip(token, versao));
    }

    @GetMapping("/{token}/download-zip")
    @Operation(summary = "Baixa os arquivos da entrega em um unico ZIP")
    public void baixarZip(
            @PathVariable String token,
            @RequestParam(required = false, defaultValue = "editada") String versao,
            HttpServletRequest http,
            HttpServletResponse response) throws IOException {
        galeriaService.registrarDownloadsZip(token, versao, ip(http), userAgent(http));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"galeria.zip\"");
        galeriaService.escreverZip(token, versao, response.getOutputStream());
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
