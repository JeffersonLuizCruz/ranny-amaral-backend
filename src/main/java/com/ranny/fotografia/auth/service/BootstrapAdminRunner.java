package com.ranny.fotografia.auth.service;

import com.ranny.fotografia.auth.model.Role;
import com.ranny.fotografia.auth.model.Usuario;
import com.ranny.fotografia.auth.repository.UsuarioRepository;
import com.ranny.fotografia.auth.security.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Cria o primeiro administrador quando o banco ainda nao possui usuarios.
 *
 * <p>Em producao a ausencia de usuarios e de credenciais de bootstrap falha o
 * startup: sem isso a aplicacao subiria sem nenhuma forma de login.
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final AuthProperties properties;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SenhaService senhaService;
    private final Environment environment;

    public BootstrapAdminRunner(AuthProperties properties, UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder, SenhaService senhaService,
                                Environment environment) {
        this.properties = properties;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.senhaService = senhaService;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        boolean producao = environment.acceptsProfiles(Profiles.of("prod"));
        if (!properties.isBootstrapEnabled()) {
            if (producao) {
                throw new IllegalStateException(
                        "Nenhum usuario cadastrado. Defina APP_AUTH_BOOTSTRAP_ADMIN_EMAIL e "
                                + "APP_AUTH_BOOTSTRAP_ADMIN_PASSWORD no primeiro deploy.");
            }
            log.warn("Nenhum usuario cadastrado e bootstrap desabilitado.");
            return;
        }

        String email = properties.getBootstrapAdminEmail();
        String senha = properties.getBootstrapAdminPassword();
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            throw new IllegalStateException(
                    "APP_AUTH_BOOTSTRAP_ADMIN_EMAIL e APP_AUTH_BOOTSTRAP_ADMIN_PASSWORD "
                            + "sao obrigatorias quando o bootstrap esta habilitado.");
        }
        senhaService.validar(senha);

        Usuario admin = Usuario.criar("Administrador", email, passwordEncoder.encode(senha),
                Role.ADMIN, true, Instant.now());
        usuarioRepository.save(admin);
        log.warn("Administrador inicial criado para {}. Troque a senha no primeiro acesso e "
                + "desabilite o bootstrap.", admin.getEmail());
    }
}
