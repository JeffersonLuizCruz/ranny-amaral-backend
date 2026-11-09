package com.ranny.fotografia.auth.service;

import com.ranny.fotografia.auth.security.AuthProperties;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;

/** Politica de senha e geracao de senhas temporarias. */
@Service
public class SenhaService {

    private static final char[] ALFABETO =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TAMANHO_TEMPORARIA = 20;

    private final AuthProperties properties;

    public SenhaService(AuthProperties properties) {
        this.properties = properties;
    }

    public void validar(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new RegraDeNegocioException("SENHA_OBRIGATORIA", "Informe a senha.");
        }
        if (senha.length() < properties.getSenhaMinima()) {
            throw new RegraDeNegocioException("SENHA_CURTA",
                    "A senha deve ter pelo menos %d caracteres.".formatted(properties.getSenhaMinima()));
        }
        if (senha.length() > properties.getSenhaMaxima()) {
            throw new RegraDeNegocioException("SENHA_LONGA",
                    "A senha deve ter no maximo %d caracteres.".formatted(properties.getSenhaMaxima()));
        }
        String normalizada = senha.toLowerCase(Locale.ROOT);
        if (normalizada.equals("12345678") || normalizada.equals("password")
                || normalizada.equals("senha123") || normalizada.equals("admin123")) {
            throw new RegraDeNegocioException("SENHA_COMUM", "Escolha uma senha menos previsivel.");
        }
    }

    public String gerarTemporaria() {
        StringBuilder senha = new StringBuilder(TAMANHO_TEMPORARIA);
        for (int i = 0; i < TAMANHO_TEMPORARIA; i++) {
            senha.append(ALFABETO[RANDOM.nextInt(ALFABETO.length)]);
        }
        return senha.toString();
    }
}
