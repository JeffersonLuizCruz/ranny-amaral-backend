package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.MidiaDerivado;
import com.ranny.fotografia.galeria.model.MidiaDerivadoOrigem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MidiaDerivadoRepository extends JpaRepository<MidiaDerivado, String> {

    List<MidiaDerivado> findByMidiaId(String midiaId);

    void deleteByMidiaIdAndOrigem(String midiaId, MidiaDerivadoOrigem origem);
}
