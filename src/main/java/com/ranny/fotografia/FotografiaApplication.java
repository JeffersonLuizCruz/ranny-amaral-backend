package com.ranny.fotografia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * A aplicacao usa JWT e gestao de usuarios no modulo {@code auth}. O
 * {@link UserDetailsServiceAutoConfiguration} e excluido para nao criar um
 * usuario default com senha gerada em log; a cadeia de seguranca e declarada
 * explicitamente em {@code auth.security.ConfiguracaoSegurancaWeb}.
 */
@EnableAsync
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FotografiaApplication {

	public static void main(String[] args) {
		SpringApplication.run(FotografiaApplication.class, args);
	}

}
