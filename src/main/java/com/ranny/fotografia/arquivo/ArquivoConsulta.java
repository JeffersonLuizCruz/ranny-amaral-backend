package com.ranny.fotografia.arquivo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * API publica de leitura e expurgo do catalogo de arquivos.
 *
 * <p>O modulo de armazenamento consulta totais e o modulo de galeria executa o
 * expurgo sem acessar a entidade {@code Arquivo} nem o storage diretamente.
 */
public interface ArquivoConsulta {

    TotaisPorFinalidade totaisPorFinalidade();

    Map<String, TotaisReferencia> totaisPorReferencia(Collection<String> referenciaIds);

    List<ArquivoResumo> arquivosDaReferencia(String referenciaId);

    long contarReferencias(String objectKey);

    int marcarExpurgados(Collection<String> arquivoIds);

    List<String> listarObjetos(String prefixo);

    List<ObjetoStorage> listarObjetosComTamanho(String prefixo);

    int removerObjetos(List<String> objectKeys);

    List<String> todasAsObjectKeys();

    record TotaisPorFinalidade(
            long originaisBytes,
            long editadasBytes,
            long derivadosBytes,
            long videosBytes,
            long comprovantesBytes,
            long totalBytes,
            long quantidade
    ) {
    }

    record TotaisReferencia(String referenciaId, long totalBytes, long quantidade) {
    }

    record ArquivoResumo(
            String id,
            String referenciaId,
            String objectKey,
            String finalidade,
            long tamanhoBytes
    ) {
    }

    record ObjetoStorage(String objectKey, long tamanhoBytes) {
    }
}
