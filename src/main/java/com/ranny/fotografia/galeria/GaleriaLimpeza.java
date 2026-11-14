package com.ranny.fotografia.galeria;

import java.util.List;

/**
 * Porta publica para agendar expurgos em lote.
 *
 * <p>O modulo de armazenamento calcula o espaco a liberar e delega ao modulo de
 * galeria a decisao de elegibilidade e o agendamento dos expurgos.
 */
public interface GaleriaLimpeza {

    /**
     * Agenda o expurgo das galerias informadas, ignorando as que deixaram de ser
     * elegiveis. Devolve quantas foram efetivamente agendadas.
     */
    int agendar(List<String> galeriaIds, String motivo);
}
