package com.ranny.fotografia.auth.repository;

import com.ranny.fotografia.auth.model.AuthEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthEventoRepository extends JpaRepository<AuthEvento, String> {
}
