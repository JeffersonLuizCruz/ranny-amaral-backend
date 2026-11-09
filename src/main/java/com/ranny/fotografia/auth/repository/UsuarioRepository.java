package com.ranny.fotografia.auth.repository;

import com.ranny.fotografia.auth.model.Role;
import com.ranny.fotografia.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    List<Usuario> findAllByOrderByNomeAsc();
}
