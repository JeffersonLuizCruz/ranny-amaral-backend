package com.ranny.fotografia.contrato.service;

import com.ranny.fotografia.contrato.EstudioConsulta;
import com.ranny.fotografia.contrato.api.EstudioRequest;
import com.ranny.fotografia.contrato.model.EstudioConfig;
import com.ranny.fotografia.contrato.repository.EstudioConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PATTERN: Facade + Singleton (acesso ao registro unico de configuracao).
 *
 * <p>Garante a existencia da configuracao padrao e centraliza a atualizacao do
 * estudio. Os demais services recebem a entidade pronta, sem se preocupar com a
 * inicializacao.
 */
@Service
public class EstudioService implements EstudioConsulta {

    private final EstudioConfigRepository estudioRepository;

    public EstudioService(EstudioConfigRepository estudioRepository) {
        this.estudioRepository = estudioRepository;
    }

    @Transactional
    public EstudioConfig obter() {
        return estudioRepository.findById(EstudioConfig.ID)
                .orElseGet(() -> estudioRepository.save(EstudioConfig.padrao()));
    }

    @Transactional
    public EstudioConfig atualizar(EstudioRequest request) {
        EstudioConfig estudio = obter();
        estudio.atualizar(
                request.nome(),
                request.razaoSocial(),
                request.cnpj(),
                request.endereco(),
                request.cidade(),
                request.estado(),
                request.email(),
                request.telefone(),
                request.responsavel(),
                request.site(),
                request.foro(),
                request.percentualEntradaPadrao(),
                request.chavePix(),
                request.instrucoesPagamento(),
                request.chavePixExtras(),
                request.instrucoesPagamentoExtras(),
                request.prazoPagamentoDias());
        return estudio;
    }

    @Override
    @Transactional(readOnly = true)
    public String nome() {
        return estudioRepository.findById(EstudioConfig.ID)
                .map(EstudioConfig::getNome)
                .filter(nome -> nome != null && !nome.isBlank())
                .orElse("Estudio");
    }

    @Override
    @Transactional(readOnly = true)
    public DadosPagamentoExtras dadosPagamentoExtras() {
        return estudioRepository.findById(EstudioConfig.ID)
                .map(estudio -> new DadosPagamentoExtras(
                        estudio.getChavePixExtras(),
                        estudio.getInstrucoesPagamentoExtras()))
                .orElseGet(() -> new DadosPagamentoExtras(null, null));
    }
}
