package com.ranny.fotografia.cliente.api;

import com.ranny.fotografia.cliente.service.ClienteService;
import com.ranny.fotografia.shared.api.PaginaResponse;
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

/**
 * PATTERN: Thin Controller / Front Controller do Spring MVC.
 *
 * <p>O controller apenas traduz HTTP para chamadas de service: nao contem regra
 * de negocio, nao acessa repositorio e nao monta entidades. Os status HTTP
 * expressam o resultado (201 + Location na criacao, 204 na exclusao, 200 nas
 * demais operacoes).
 */
@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Cadastro e consulta de clientes do estudio")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    @Operation(summary = "Lista clientes de forma paginada, filtrando por nome, WhatsApp ou e-mail")
    public PaginaResponse<ClienteResponse> listar(
            @RequestParam(required = false) String q,
            @Parameter(description = "Pagina 0-based")
            @RequestParam(required = false) Integer pagina,
            @Parameter(description = "Tamanho da pagina (1 a 100, default 20)")
            @RequestParam(required = false) Integer tamanho) {
        return clienteService.listar(q, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um cliente")
    public ClienteResponse buscar(@PathVariable String id) {
        return clienteService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um cliente")
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        ClienteResponse criado = clienteService.criar(request);
        var location = uriBuilder.path("/api/v1/clientes/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um cliente")
    public ClienteResponse atualizar(@PathVariable String id,
                                     @Valid @RequestBody ClienteRequest request) {
        return clienteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um cliente sem vinculos")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
