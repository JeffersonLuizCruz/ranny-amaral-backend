package com.ranny.fotografia.contrato.service;

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
public class EstudioService {

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
                request.prazoPagamentoDias());
        return estudio;
    }
}
