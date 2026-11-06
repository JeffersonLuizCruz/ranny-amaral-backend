package com.ranny.fotografia.cliente.repository;

import com.ranny.fotografia.cliente.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PATTERN: Repository (Spring Data).
 *
 * <p>A interface declara as consultas em termos de dominio; o Spring gera a
 * implementacao. O service nunca monta SQL/JPA diretamente.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, String> {

    @Query("""
            select c from Cliente c
            where lower(c.nome) like :termo
               or lower(c.email) like :termo
               or c.whatsapp like :termo
            """)
    Page<Cliente> buscar(@Param("termo") String termo, Pageable pageable);

    Optional<Cliente> findFirstByDocumento(String documento);

    Optional<Cliente> findFirstByWhatsapp(String whatsapp);
}
