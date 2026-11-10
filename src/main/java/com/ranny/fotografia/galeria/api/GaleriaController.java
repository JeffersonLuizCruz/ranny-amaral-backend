package com.ranny.fotografia.galeria.api;

import com.ranny.fotografia.galeria.service.GaleriaService;
import com.ranny.fotografia.shared.api.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rotas administrativas de galerias. */
@RestController
@RequestMapping("/api/v1/galerias")
@Tag(name = "Galerias", description = "Galerias, midias, selecao, extras e entrega")
public class GaleriaController {

    private final GaleriaService galeriaService;

    public GaleriaController(GaleriaService galeriaService) {
        this.galeriaService = galeriaService;
    }

    @PostMapping
    @Operation(summary = "Cria ou retorna a galeria de um ensaio")
    public GaleriaDtos.GaleriaResponse criar(@Valid @RequestBody GaleriaDtos.CriarGaleriaRequest request) {
        return galeriaService.criar(request);
    }

    @GetMapping
    @Operation(summary = "Lista galerias de forma paginada")
    public PaginaResponse<GaleriaDtos.GaleriaResponse> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return galeriaService.listar(status, q, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma galeria")
    public GaleriaDtos.GaleriaDetalheResponse detalhar(@PathVariable String id) {
        return galeriaService.detalhar(id);
    }

    @GetMapping("/{id}/midias")
    @Operation(summary = "Lista as midias da galeria")
    public PaginaResponse<GaleriaDtos.MidiaResponse> listarMidias(
            @PathVariable String id,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        return galeriaService.listarMidias(id, pagina, tamanho);
    }

    @PostMapping("/{id}/midias/upload")
    @Operation(summary = "Inicia o upload direto de uma midia")
    public GaleriaDtos.UploadSessaoResponse iniciarUpload(
            @PathVariable String id,
            @Valid @RequestBody GaleriaDtos.IniciarMidiaUploadRequest request) {
        return galeriaService.iniciarUpload(id, request);
    }

    @PostMapping("/{id}/midias/upload/{arquivoId}/complete")
    @Operation(summary = "Conclui o upload, processa derivados e cria a midia")
    public GaleriaDtos.MidiaResponse concluirUpload(
            @PathVariable String id,
            @PathVariable String arquivoId,
            @Valid @RequestBody GaleriaDtos.ConcluirMidiaUploadRequest request) {
        return galeriaService.concluirUpload(id, arquivoId, request);
    }

    @PostMapping("/{id}/midias/upload/{arquivoId}/abort")
    @Operation(summary = "Aborta um upload de midia")
    public void abortarUpload(@PathVariable String id, @PathVariable String arquivoId,
                              @RequestParam String tipo) {
        galeriaService.abortarUpload(id, arquivoId, tipo);
    }

    @DeleteMapping("/{id}/midias/{midiaId}")
    @Operation(summary = "Remove uma midia da galeria")
    public void removerMidia(@PathVariable String id, @PathVariable String midiaId) {
        galeriaService.removerMidia(id, midiaId);
    }

    @PostMapping("/{id}/midias/{midiaId}/editada/upload")
    @Operation(summary = "Inicia o upload da versao final editada de uma foto")
    public GaleriaDtos.UploadSessaoResponse iniciarUploadEditada(
            @PathVariable String id,
            @PathVariable String midiaId,
            @Valid @RequestBody GaleriaDtos.IniciarMidiaEditadaUploadRequest request) {
        return galeriaService.iniciarUploadEditada(id, midiaId, request);
    }

    @PostMapping("/{id}/midias/{midiaId}/editada/upload/{arquivoId}/complete")
    @Operation(summary = "Conclui o upload da versao editada e gera os derivados")
    public GaleriaDtos.MidiaResponse concluirUploadEditada(
            @PathVariable String id,
            @PathVariable String midiaId,
            @PathVariable String arquivoId,
            @Valid @RequestBody GaleriaDtos.ConcluirMidiaEditadaUploadRequest request) {
        return galeriaService.concluirUploadEditada(id, midiaId, arquivoId, request);
    }

    @PostMapping("/{id}/midias/{midiaId}/editada/upload/{arquivoId}/abort")
    @Operation(summary = "Aborta o upload da versao editada")
    public void abortarUploadEditada(@PathVariable String id, @PathVariable String midiaId,
                                     @PathVariable String arquivoId) {
        galeriaService.abortarUploadEditada(id, midiaId, arquivoId);
    }

    @DeleteMapping("/{id}/midias/{midiaId}/editada")
    @Operation(summary = "Remove a versao editada vinculada a uma foto")
    public void removerVersaoEditada(@PathVariable String id, @PathVariable String midiaId) {
        galeriaService.removerVersaoEditada(id, midiaId);
    }

    @PostMapping("/{id}/selecao/publicar")
    @Operation(summary = "Publica ou renova o link de selecao")
    public GaleriaDtos.LinkResponse publicarSelecao(@PathVariable String id) {
        return galeriaService.publicarSelecao(id);
    }

    @PostMapping("/{id}/selecao/reabrir")
    @Operation(summary = "Reabre a selecao existente")
    public GaleriaDtos.LinkResponse reabrirSelecao(@PathVariable String id) {
        return galeriaService.reabrirSelecao(id);
    }

    @PostMapping("/{id}/selecao/download")
    @Operation(summary = "Prepara o download das fotos selecionadas para edicao")
    public GaleriaDtos.DownloadSelecaoResponse prepararDownloadSelecao(
            @PathVariable String id) {
        return galeriaService.prepararDownloadSelecao(id);
    }

    @PostMapping("/{id}/extras/confirmar")
    @Operation(summary = "Confirma o pagamento dos extras")
    public GaleriaDtos.PedidoExtrasResponse confirmarExtras(
            @PathVariable String id,
            @Valid @RequestBody GaleriaDtos.ExtrasConfirmacaoRequest request) {
        return galeriaService.confirmarExtras(id, request);
    }

    @PostMapping("/{id}/extras/isentar")
    @Operation(summary = "Isenta os extras")
    public GaleriaDtos.PedidoExtrasResponse isentarExtras(
            @PathVariable String id, @RequestParam(required = false) String por) {
        return galeriaService.isentarExtras(id, por);
    }

    @PostMapping("/{id}/edicao/iniciar")
    @Operation(summary = "Inicia a edicao")
    public void iniciarEdicao(@PathVariable String id) {
        galeriaService.iniciarEdicao(id);
    }

    @PostMapping("/{id}/edicao/liberar")
    @Operation(summary = "Libera a edicao manualmente com motivo")
    public GaleriaDtos.PedidoExtrasResponse liberarEdicao(
            @PathVariable String id,
            @Valid @RequestBody GaleriaDtos.LiberacaoEdicaoRequest request) {
        return galeriaService.liberarEdicaoManual(id, request);
    }

    @PostMapping("/{id}/pronta-entrega")
    @Operation(summary = "Marca a galeria como pronta para entrega")
    public void marcarProntaEntrega(@PathVariable String id) {
        galeriaService.marcarProntaEntrega(id);
    }

    @PostMapping("/{id}/entrega/publicar")
    @Operation(summary = "Publica ou renova o link de entrega")
    public GaleriaDtos.LinkResponse publicarEntrega(@PathVariable String id) {
        return galeriaService.publicarEntrega(id);
    }

    @PostMapping("/{id}/entrega/revogar")
    @Operation(summary = "Revoga o link de entrega")
    public void revogarEntrega(@PathVariable String id) {
        galeriaService.revogarEntrega(id);
    }

    @PostMapping("/{id}/entrega/liberar-raw")
    @Operation(summary = "Libera os arquivos originais para download")
    public GaleriaDtos.GaleriaResponse liberarRaw(
            @PathVariable String id,
            @RequestBody(required = false) GaleriaDtos.LiberacaoRawRequest request) {
        return galeriaService.liberarRaw(id, request == null ? null : request.por());
    }

    @PostMapping("/{id}/entrega/revogar-raw")
    @Operation(summary = "Revoga a liberacao dos arquivos originais")
    public GaleriaDtos.GaleriaResponse revogarRaw(@PathVariable String id) {
        return galeriaService.revogarRaw(id);
    }
}
