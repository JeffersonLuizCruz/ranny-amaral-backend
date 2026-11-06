package com.ranny.fotografia.pacote.service;

import com.ranny.fotografia.pacote.PacoteConsulta;
import com.ranny.fotografia.pacote.PacoteResumo;
import com.ranny.fotografia.pacote.api.PacoteRequest;
import com.ranny.fotografia.pacote.api.PacoteResponse;
import com.ranny.fotografia.pacote.model.Pacote;
import com.ranny.fotografia.pacote.model.PromocaoPacote;
import com.ranny.fotografia.pacote.repository.PacoteRepository;
import com.ranny.fotografia.shared.api.PaginaResponse;
import com.ranny.fotografia.shared.api.Paginacao;
import com.ranny.fotografia.shared.exception.ConflitoException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import com.ranny.fotografia.shared.ports.VerificadorVinculoPacote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PATTERN: Facade + Strategy + Specification (regra de inferencia).
 *
 * <p>A inferencia de quantidades usa a mesma heuristica do frontend ("20 fotos",
 * "1 video") para pacotes legados que nao informam os campos. Ela fica isolada
 * em metodos privados, e o service atua como facade: normaliza o request,
 * aplica defaults, persiste e converte para DTO.
 *
 * <p>A exclusao usa as estrategias de {@link VerificadorVinculoPacote} para
 * bloquear pacotes usados por agendamentos sem depender do modulo agenda.
 */
@Service
public class PacoteService implements PacoteConsulta {

    private static final Pattern PADRAO_FOTOS = Pattern.compile("(\\d+)\\s*fotos?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PADRAO_VIDEOS = Pattern.compile("(\\d+)\\s*v[ií]deos?", Pattern.CASE_INSENSITIVE);

    private final PacoteRepository pacoteRepository;
    private final List<VerificadorVinculoPacote> verificadoresDeVinculo;

    public PacoteService(PacoteRepository pacoteRepository,
                         List<VerificadorVinculoPacote> verificadoresDeVinculo) {
        this.pacoteRepository = pacoteRepository;
        this.verificadoresDeVinculo = List.copyOf(verificadoresDeVinculo);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PacoteResponse> listar(Boolean ativo, String busca,
                                                 Integer pagina, Integer tamanho) {
        var pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.ASC, "nome"));
        String termo = busca == null || busca.isBlank()
                ? "%"
                : "%" + busca.trim().toLowerCase(Locale.ROOT) + "%";
        Page<Pacote> pacotes = pacoteRepository.buscar(termo, ativo, pageable);
        return PaginaResponse.de(pacotes, PacoteResponse::de);
    }

    @Transactional(readOnly = true)
    public PacoteResponse buscarPorId(String id) {
        return PacoteResponse.de(obter(id));
    }

    @Transactional
    public PacoteResponse criar(PacoteRequest request) {
        List<String> itens = normalizarItens(request.itensInclusos());
        Pacote pacote = Pacote.criar(
                request.nome().trim(),
                request.duracaoMinutos(),
                request.valor(),
                normalizarDescricao(request.descricao()),
                request.corHex().trim(),
                itens,
                request.ativo() == null || request.ativo(),
                resolverQuantidade(request.quantidadeFotosInclusas(), itens, PADRAO_FOTOS),
                resolverQuantidade(request.quantidadeVideosInclusos(), itens, PADRAO_VIDEOS),
                resolverValorFotoExtra(request.valorFotoExtra()),
                montarPromocao(request.promocao()));
        return PacoteResponse.de(pacoteRepository.save(pacote));
    }

    @Transactional
    public PacoteResponse atualizar(String id, PacoteRequest request) {
        Pacote pacote = obter(id);
        List<String> itens = normalizarItens(request.itensInclusos());
        pacote.atualizar(
                request.nome().trim(),
                request.duracaoMinutos(),
                request.valor(),
                normalizarDescricao(request.descricao()),
                request.corHex().trim(),
                itens,
                request.ativo() != null && request.ativo(),
                resolverQuantidade(request.quantidadeFotosInclusas(), itens, PADRAO_FOTOS),
                resolverQuantidade(request.quantidadeVideosInclusos(), itens, PADRAO_VIDEOS),
                resolverValorFotoExtra(request.valorFotoExtra()),
                montarPromocao(request.promocao()));
        return PacoteResponse.de(pacote);
    }

    @Transactional
    public PacoteResponse alternarAtivo(String id) {
        Pacote pacote = obter(id);
        pacote.alternarAtivo();
        return PacoteResponse.de(pacote);
    }

    @Transactional
    public void excluir(String id) {
        Pacote pacote = obter(id);
        boolean possuiVinculo = verificadoresDeVinculo.stream()
                .anyMatch(verificador -> verificador.possuiVinculo(id));
        if (possuiVinculo) {
            throw new ConflitoException("PACOTE_COM_VINCULO",
                    "O pacote '%s' possui agendamentos vinculados e nao pode ser excluido.".formatted(pacote.getNome()));
        }
        pacoteRepository.delete(pacote);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PacoteResumo> buscar(String pacoteId) {
        if (pacoteId == null || pacoteId.isBlank()) {
            return Optional.empty();
        }
        return pacoteRepository.findById(pacoteId).map(PacoteService::paraResumo);
    }

    private Pacote obter(String id) {
        return pacoteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pacote", id));
    }

    private static int resolverQuantidade(Integer informada, List<String> itens, Pattern padrao) {
        if (informada != null) {
            return Math.max(0, informada);
        }
        String texto = String.join(" ", itens);
        Matcher matcher = padrao.matcher(texto);
        if (matcher.find()) {
            try {
                return Math.max(0, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static BigDecimal resolverValorFotoExtra(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static String normalizarDescricao(String descricao) {
        return descricao == null || descricao.isBlank() ? "" : descricao.trim();
    }

    private static List<String> normalizarItens(List<String> itens) {
        if (itens == null) {
            return List.of();
        }
        return itens.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
    }

    private static PromocaoPacote montarPromocao(PacoteRequest.PromocaoRequest promocao) {
        if (promocao == null) {
            return null;
        }
        return PromocaoPacote.de(
                promocao.ativa(),
                promocao.valorPromocional(),
                promocao.validaAte(),
                promocao.rotulo());
    }

    private static PacoteResumo paraResumo(Pacote pacote) {
        PacoteResumo.PromocaoResumo promocao = pacote.getPromocao() == null ? null
                : new PacoteResumo.PromocaoResumo(
                        pacote.getPromocao().estaAtiva(),
                        pacote.getPromocao().getValorPromocional(),
                        pacote.getPromocao().getValidaAte(),
                        pacote.getPromocao().getRotulo());
        return new PacoteResumo(
                pacote.getId(),
                pacote.getNome(),
                pacote.getDuracaoMinutos(),
                pacote.getValor(),
                pacote.getItensInclusos(),
                pacote.getQuantidadeFotosInclusas(),
                pacote.getQuantidadeVideosInclusos(),
                pacote.getValorFotoExtra(),
                promocao,
                pacote.isAtivo());
    }
}
