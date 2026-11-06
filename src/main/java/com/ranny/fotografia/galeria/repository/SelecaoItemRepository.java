package com.ranny.fotografia.galeria.repository;

import com.ranny.fotografia.galeria.model.SelecaoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SelecaoItemRepository extends JpaRepository<SelecaoItem, String> {

    List<SelecaoItem> findBySelecaoId(String selecaoId);
}
