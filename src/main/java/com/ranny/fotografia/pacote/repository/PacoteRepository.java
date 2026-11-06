package com.ranny.fotografia.pacote.repository;

import com.ranny.fotografia.pacote.model.Pacote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** PATTERN: Repository (Spring Data) para o catalogo de pacotes. */
@Repository
public interface PacoteRepository extends JpaRepository<Pacote, String> {

    @Query("""
            select p from Pacote p
            where (:ativo is null or p.ativo = :ativo)
              and (lower(p.nome) like :termo
                   or lower(p.descricao) like :termo)
            """)
    Page<Pacote> buscar(@Param("termo") String termo,
                        @Param("ativo") Boolean ativo,
                        Pageable pageable);
}
