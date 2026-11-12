package com.ranny.fotografia.armazenamento.service;

import com.ranny.fotografia.armazenamento.ArmazenamentoProperties;
import com.ranny.fotografia.armazenamento.model.OrigemColeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mede total e livre do filesystem onde ficam os volumes.
 *
 * <p>O backend roda em container: ler {@code /} devolveria o overlay, nao o disco
 * do host. Por isso o caminho e configuravel e deve apontar para um volume do
 * Garage montado read-only (mesmo disco do Postgres).
 *
 * <p>A API de administracao do Garage (v2.5.0) nao expoe espaco em disco; essa
 * foi a alternativa descartada.
 */
@Component
public class MedidorVolumeFilesystem implements MedidorVolume {

    private static final Logger log = LoggerFactory.getLogger(MedidorVolumeFilesystem.class);

    private final ArmazenamentoProperties properties;

    public MedidorVolumeFilesystem(ArmazenamentoProperties properties) {
        this.properties = properties;
    }

    @Override
    public Medicao medir() {
        String caminho = properties.getVolumePath();
        if (caminho == null || caminho.isBlank()) {
            return new Medicao(null, null, OrigemColeta.INDISPONIVEL);
        }
        try {
            FileStore store = Files.getFileStore(Path.of(caminho));
            return new Medicao(store.getTotalSpace(), store.getUsableSpace(),
                    OrigemColeta.FILESYSTEM);
        } catch (IOException | RuntimeException ex) {
            log.warn("Nao foi possivel medir o volume {}: {}. Configure app.armazenamento.volume-path.",
                    caminho, ex.getMessage());
            return new Medicao(null, null, OrigemColeta.INDISPONIVEL);
        }
    }
}
