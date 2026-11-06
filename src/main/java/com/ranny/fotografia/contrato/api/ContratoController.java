package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.model.documento.StatusAceite;
import com.ranny.fotografia.contrato.service.ContratoService;
import com.ranny.fotografia.shared.api.PaginaResponse;
import com.ranny.fotografia.shared.model.PagamentoStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/** Thin Controller dos contratos administrativos. */
@RestController
@RequestMapping("/api/v1/contratos")
@Tag(name = "Contratos", description = "Contratos, links de aceite e pagamentos")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @GetMapping
    @Operation(summary = "Lista contratos de forma paginada por status, cliente, agendamento ou texto")
    public PaginaResponse<ContratoResponse> listar(
            @RequestParam(required = false) StatusAceite statusAceite,
            @RequestParam(required = false) PagamentoStatus statusPagamento,
            @RequestParam(required = false) String clienteId,
            @RequestParam(required = false) String agendamentoId,
            @RequestParam(required = false) String q,
            @Parameter(description = "Pagina 0-based")
            @RequestParam(required = false) Integer pagina,
            @Parameter(description = "Tamanho da pagina (1 a 100, default 20)")
            @RequestParam(required = false) Integer tamanho) {
        return contratoService.listar(statusAceite, statusPagamento, clienteId, agendamentoId, q, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um contrato")
    public ContratoResponse buscar(@PathVariable String id) {
        return contratoService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cria um contrato em rascunho com snapshot e clausulas resolvidas")
    public ResponseEntity<ContratoResponse> criar(@Valid @RequestBody ContratoRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        ContratoResponse criado = contratoService.criar(request);
        var location = uriBuilder.path("/api/v1/contratos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um contrato em rascunho")
    public ContratoResponse atualizar(@PathVariable String id,
                                      @Valid @RequestBody ContratoRequest request) {
        return contratoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um contrato em rascunho")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        contratoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicar")
    @Operation(summary = "Duplica um contrato como novo rascunho")
    public ResponseEntity<ContratoResponse> duplicar(@PathVariable String id,
                                                     UriComponentsBuilder uriBuilder) {
        ContratoResponse duplicado = contratoService.duplicar(id);
        var location = uriBuilder.path("/api/v1/contratos/{id}").buildAndExpand(duplicado.id()).toUri();
        return ResponseEntity.created(location).body(duplicado);
    }

    @PostMapping("/{id}/links-aceite")
    @Operation(summary = "Gera um novo link publico de aceite")
    public ResponseEntity<SolicitacaoAceiteResponse> gerarLink(
            @PathVariable String id,
            @RequestBody(required = false) @Valid LinkAceiteRequest request) {
        return ResponseEntity.ok(contratoService.gerarLink(id, request));
    }

    @GetMapping("/{id}/links-aceite")
    @Operation(summary = "Lista os links de aceite do contrato, do mais recente para o mais antigo")
    public List<SolicitacaoAceiteResponse> listarLinks(@PathVariable String id) {
        return contratoService.listarLinks(id);
    }

    @PostMapping("/{id}/links-aceite/{linkId}/cancelar")
    @Operation(summary = "Cancela um link de aceite")
    public ResponseEntity<Void> cancelarLink(@PathVariable String id, @PathVariable String linkId) {
        contratoService.cancelarLink(id, linkId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reabrir")
    @Operation(summary = "Reabre um contrato pendente/expirado como rascunho")
    public ContratoResponse reabrir(@PathVariable String id) {
        return contratoService.reabrir(id);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela um contrato ainda nao aceito")
    public ContratoResponse cancelar(@PathVariable String id) {
        return contratoService.cancelar(id);
    }

    @PostMapping("/{id}/pagamento/confirmar")
    @Operation(summary = "Confirma a entrada do contrato")
    public ContratoResponse confirmarPagamento(
            @PathVariable String id,
            @RequestBody(required = false) @Valid PagamentoContratoRequests.ConfirmacaoRequest request) {
        return contratoService.confirmarPagamento(id, request);
    }

    @PostMapping("/{id}/pagamento/rejeitar")
    @Operation(summary = "Rejeita o comprovante de entrada")
    public ContratoResponse rejeitarPagamento(
            @PathVariable String id,
            @Valid @RequestBody PagamentoContratoRequests.RejeicaoRequest request) {
        return contratoService.rejeitarComprovante(id, request);
    }

    @PostMapping("/{id}/pagamento/isentar")
    @Operation(summary = "Isenta a entrada")
    public ContratoResponse isentarPagamento(@PathVariable String id) {
        return contratoService.isentarEntrada(id);
    }

    @PutMapping("/{id}/pagamento-final")
    @Operation(summary = "Registra o recebimento do saldo final")
    public ContratoResponse registrarPagamentoFinal(
            @PathVariable String id,
            @Valid @RequestBody PagamentoContratoRequests.PagamentoFinalRequest request) {
        return contratoService.registrarPagamentoFinal(id, request);
    }

    @PostMapping("/{id}/pagamento-final/isentar")
    @Operation(summary = "Isenta o saldo final")
    public ContratoResponse isentarPagamentoFinal(@PathVariable String id) {
        return contratoService.isentarPagamentoFinal(id);
    }

    @PostMapping("/{id}/pagamento-final/reabrir")
    @Operation(summary = "Reabre o saldo final para novo recebimento")
    public ContratoResponse reabrirPagamentoFinal(@PathVariable String id) {
        return contratoService.reabrirPagamentoFinal(id);
    }
}
