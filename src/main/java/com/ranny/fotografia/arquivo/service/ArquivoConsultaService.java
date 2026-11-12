package com.ranny.fotografia.arquivo.service;

import com.ranny.fotografia.arquivo.ArquivoConsulta;
import com.ranny.fotografia.arquivo.ArquivoFinalidade;
import com.ranny.fotografia.arquivo.ArquivoStatus;
import com.ranny.fotografia.arquivo.model.Arquivo;
import com.ranny.fotografia.arquivo.repository.ArquivoRepository;
import com.ranny.fotografia.arquivo.storage.ObjectStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implementacao da API publica do catalogo de arquivos. */
@Service
public class ArquivoConsultaService implements ArquivoConsulta {

    private final ArquivoRepository repository;
    private final ObjectStorage storage;

    public ArquivoConsultaService(ArquivoRepository repository, ObjectStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public TotaisPorFinalidade totaisPorFinalidade() {
        long originais = 0;
        long editadas = 0;
        long derivados = 0;
        long videos = 0;
        long comprovantes = 0;
        long quantidade = 0;
        for (Object[] linha : repository.totaisPorFinalidade(ArquivoStatus.EXPURGADO)) {
            ArquivoFinalidade finalidade = (ArquivoFinalidade) linha[0];
            long bytes = numero(linha[1]);
            quantidade += numero(linha[2]);
            switch (finalidade) {
                case FOTO_ORIGINAL -> originais += bytes;
                case FOTO_EDITADA -> editadas += bytes;
                case FOTO_DERIVADO -> derivados += bytes;
                case VIDEO_ORIGINAL -> videos += bytes;
                case COMPROVANTE_PAGAMENTO, CONTRATO_PDF -> comprovantes += bytes;
            }
        }
        long total = originais + editadas + derivados + videos + comprovantes;
        return new TotaisPorFinalidade(originais, editadas, derivados, videos,
                comprovantes, total, quantidade);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, TotaisReferencia> totaisPorReferencia(Collection<String> referenciaIds) {
        if (referenciaIds == null || referenciaIds.isEmpty()) {
            return Map.of();
        }
        Map<String, TotaisReferencia> totais = new LinkedHashMap<>();
        for (Object[] linha : repository.totaisPorReferencia(
                referenciaIds, ArquivoStatus.EXPURGADO)) {
            String referencia = (String) linha[0];
            totais.put(referencia, new TotaisReferencia(
                    referencia, numero(linha[1]), numero(linha[2])));
        }
        return totais;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArquivoResumo> arquivosDaReferencia(String referenciaId) {
        List<ArquivoResumo> resumos = new ArrayList<>();
        for (Arquivo arquivo : repository.findByReferenciaIdAndStatusNot(
                referenciaId, ArquivoStatus.EXPURGADO)) {
            resumos.add(new ArquivoResumo(
                    arquivo.getId(),
                    arquivo.getReferenciaId(),
                    arquivo.getObjectKey(),
                    arquivo.getFinalidade().name(),
                    arquivo.getTamanhoBytes()));
        }
        return resumos;
    }

    @Override
    @Transactional(readOnly = true)
    public long contarReferencias(String objectKey) {
        return repository.countByObjectKeyAndStatusNot(objectKey, ArquivoStatus.EXPURGADO);
    }

    @Override
    @Transactional
    public int marcarExpurgados(Collection<String> arquivoIds) {
        if (arquivoIds == null || arquivoIds.isEmpty()) {
            return 0;
        }
        int marcados = 0;
        for (Arquivo arquivo : repository.findAllById(arquivoIds)) {
            if (arquivo.getStatus() != ArquivoStatus.EXPURGADO) {
                arquivo.expurgar();
                marcados++;
            }
        }
        return marcados;
    }

    @Override
    public List<String> listarObjetos(String prefixo) {
        return storage.listar(prefixo).stream()
                .map(ObjectStorage.ObjetoArmazenado::objectKey)
                .toList();
    }

    @Override
    public List<ObjetoStorage> listarObjetosComTamanho(String prefixo) {
        return storage.listar(prefixo).stream()
                .map(objeto -> new ObjetoStorage(objeto.objectKey(), objeto.tamanhoBytes()))
                .toList();
    }

    @Override
    public int removerObjetos(List<String> objectKeys) {
        return storage.removerLote(objectKeys);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> todasAsObjectKeys() {
        return repository.objectKeysAtivas(ArquivoStatus.EXPURGADO);
    }

    private static long numero(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }
}
