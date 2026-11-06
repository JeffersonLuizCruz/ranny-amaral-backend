package com.ranny.fotografia.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * PATTERN: Template Method (implicito do Spring MVC).
 *
 * <p>{@link WebMvcConfigurer} expoe pontos de extensao que o Spring chama
 * durante a configuracao do {@code DispatcherServlet}. Sobrescrevemos apenas o
 * metodo necessario ({@code addCorsMappings}), sem herdar comportamento.
 *
 * <p>Beneficios: a politica de CORS fica declarativa e isolada, e o frontend de
 * desenvolvimento (Vite em :5173) consegue consumir a API sem proxies
 * temporarios.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origensPermitidas;

    public CorsConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") String origensPermitidas) {
        this.origensPermitidas = origensPermitidas.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
