package com.ranny.fotografia.cliente.service;

import com.ranny.fotografia.cliente.ClienteConsulta;
import com.ranny.fotografia.cliente.ClienteResumo;
import com.ranny.fotografia.cliente.ContatoCliente;
import com.ranny.fotografia.cliente.api.ClienteRequest;
import com.ranny.fotografia.cliente.api.ClienteResponse;
import com.ranny.fotografia.cliente.model.Cliente;
import com.ranny.fotografia.cliente.model.ClienteOrigem;
import com.ranny.fotografia.cliente.repository.ClienteRepository;
import com.ranny.fotografia.shared.api.PaginaResponse;
import com.ranny.fotografia.shared.api.Paginacao;
import com.ranny.fotografia.shared.exception.ConflitoException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.ports.VerificadorVinculoCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * PATTERN: Facade + Strategy.
 *
 * <p>A facade oferece uma API de negocio simples ao controller, escondendo
 * repositorio, mapeamento e validacoes. A lista de
 * {@link VerificadorVinculoCliente} e injetada pelo Spring: cada modulo que
 * conhece um vinculo de cliente registra sua estrategia, e este service apenas
 * pergunta "algum vinculo?" sem conhecer os modulos concretos.
 *
 * <p>Beneficios: novas origens de vinculo entram sem alterar cliente; a regra
 * de exclusao fica em um unico ponto; e os testes podem injetar stubs.
 */
@Service
public class ClienteService implements ClienteConsulta {

    private final ClienteRepository clienteRepository;
    private final List<VerificadorVinculoCliente> verificadoresDeVinculo;

    public ClienteService(ClienteRepository clienteRepository,
                          List<VerificadorVinculoCliente> verificadoresDeVinculo) {
        this.clienteRepository = clienteRepository;
        this.verificadoresDeVinculo = List.copyOf(verificadoresDeVinculo);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResponse> listar(String busca, Integer pagina, Integer tamanho) {
        var pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.ASC, "nome"));
        Page<Cliente> clientes = busca == null || busca.isBlank()
                ? clienteRepository.findAll(pageable)
                : clienteRepository.buscar("%" + busca.trim().toLowerCase(Locale.ROOT) + "%", pageable);
        return PaginaResponse.de(clientes, ClienteResponse::de);
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(String id) {
        return ClienteResponse.de(obter(id));
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        Cliente cliente = Cliente.criar(
                request.nome().trim(),
                normalizar(request.whatsapp()),
                normalizar(request.email()),
                normalizar(request.documento()),
                request.tipoDocumento(),
                normalizar(request.cidade()),
                request.origem() == null ? ClienteOrigem.OUTRO : request.origem(),
                normalizar(request.observacoes()));
        return ClienteResponse.de(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse atualizar(String id, ClienteRequest request) {
        Cliente cliente = obter(id);
        cliente.atualizar(
                request.nome().trim(),
                normalizar(request.whatsapp()),
                normalizar(request.email()),
                normalizar(request.documento()),
                request.tipoDocumento(),
                normalizar(request.cidade()),
                request.origem() == null ? cliente.getOrigem() : request.origem(),
                normalizar(request.observacoes()));
        return ClienteResponse.de(cliente);
    }

    @Transactional
    public void excluir(String id) {
        Cliente cliente = obter(id);
        boolean possuiVinculo = verificadoresDeVinculo.stream()
                .anyMatch(verificador -> verificador.possuiVinculo(id));
        if (possuiVinculo) {
            throw new ConflitoException("CLIENTE_COM_VINCULO",
                    "O cliente '%s' possui agendamentos vinculados e nao pode ser excluido.".formatted(cliente.getNome()));
        }
        clienteRepository.delete(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteResumo> buscar(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            return Optional.empty();
        }
        return clienteRepository.findById(clienteId).map(ClienteService::paraResumo);
    }

    @Override
    @Transactional
    public ClienteResumo obterOuCriar(ContatoCliente contato) {
        Optional<Cliente> existente = localizarExistente(contato);
        Cliente cliente = existente
                .map(encontrado -> {
                    encontrado.atualizarDadosPessoais(
                            contato.nome(),
                            contato.whatsapp(),
                            contato.email(),
                            contato.documento(),
                            contato.tipoDocumento(),
                            contato.cidade());
                    return encontrado;
                })
                .orElseGet(() -> Cliente.criar(
                        contato.nome(),
                        contato.whatsapp(),
                        contato.email(),
                        contato.documento(),
                        contato.tipoDocumento(),
                        contato.cidade(),
                        ClienteOrigem.CONTRATO,
                        "Cliente criado automaticamente pelo aceite do contrato."));
        return paraResumo(clienteRepository.save(cliente));
    }

    private Optional<Cliente> localizarExistente(ContatoCliente contato) {
        if (contato.documento() != null && !contato.documento().isBlank()) {
            Optional<Cliente> porDocumento = clienteRepository.findFirstByDocumento(contato.documento());
            if (porDocumento.isPresent()) {
                return porDocumento;
            }
        }
        if (contato.whatsapp() != null && !contato.whatsapp().isBlank()) {
            return clienteRepository.findFirstByWhatsapp(contato.whatsapp());
        }
        return Optional.empty();
    }

    private Cliente obter(String id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    private static ClienteResumo paraResumo(Cliente cliente) {
        return new ClienteResumo(
                cliente.getId(),
                cliente.getNome(),
                cliente.getWhatsapp(),
                cliente.getEmail(),
                cliente.getDocumento(),
                cliente.getTipoDocumento(),
                cliente.getCidade());
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
