package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.Galeria;
import com.ranny.fotografia.galeria.model.GaleriaStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** PATTERN: Specification para filtros combinaveis de galerias. */
public final class GaleriaSpecifications {

    private GaleriaSpecifications() {
    }

    public static Specification<Galeria> comStatus(GaleriaStatus status) {
        return status == null ? null
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Galeria> busca(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        String like = "%" + termo.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("clienteNome")), like);
    }
}
