package com.ranny.fotografia.armazenamento.service;

import com.ranny.fotografia.armazenamento.model.OrigemColeta;

/** Porta de medicao do disco. */
public interface MedidorVolume {

    Medicao medir();

    record Medicao(Long totalBytes, Long livreBytes, OrigemColeta origem) {
    }
}
