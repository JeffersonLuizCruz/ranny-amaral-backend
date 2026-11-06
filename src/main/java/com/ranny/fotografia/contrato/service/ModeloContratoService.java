package com.ranny.fotografia.contrato.service;

import com.ranny.fotografia.contrato.api.ModeloContratoRequest;
import com.ranny.fotografia.contrato.model.ModeloContrato;
import com.ranny.fotografia.contrato.repository.ModeloContratoRepository;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PATTERN: Facade + Prototype (duplicacao) para modelos de contrato.
 *
 * <p>O service nao monta o clone: delega ao proprio modelo, que conhece suas
 * invariantes (nova versao, novo id, copia defensiva das clausulas).
 */
@Service
public class ModeloContratoService {

    private final ModeloContratoRepository modeloRepository;

    public ModeloContratoService(ModeloContratoRepository modeloRepository) {
        this.modeloRepository = modeloRepository;
    }

    @Transactional(readOnly = true)
    public List<ModeloContrato> listar() {
        return modeloRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public ModeloContrato obter(String id) {
        return modeloRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Modelo de contrato", id));
    }

    @Transactional
    public ModeloContrato criar(ModeloContratoRequest request) {
        return modeloRepository.save(ModeloContrato.criar(
                request.nome().trim(),
                request.descricao(),
                request.clausulas(),
                request.ativo() == null || request.ativo()));
    }

    @Transactional
    public ModeloContrato atualizar(String id, ModeloContratoRequest request) {
        ModeloContrato modelo = obter(id);
        modelo.atualizar(
                request.nome().trim(),
                request.descricao(),
                request.clausulas(),
                request.ativo() != null && request.ativo());
        return modelo;
    }

    @Transactional
    public ModeloContrato alternarAtivo(String id) {
        ModeloContrato modelo = obter(id);
        modelo.alternarAtivo();
        return modelo;
    }

    @Transactional
    public ModeloContrato duplicar(String id) {
        return modeloRepository.save(obter(id).duplicar());
    }

    @Transactional
    public void excluir(String id) {
        modeloRepository.delete(obter(id));
    }
}
