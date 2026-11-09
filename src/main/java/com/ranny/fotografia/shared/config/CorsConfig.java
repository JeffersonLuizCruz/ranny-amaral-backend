package com.ranny.fotografia.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

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
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Fonte unica de CORS para o Spring Security.
     *
     * <p>O {@code CorsFilter} do Security e executado antes do MVC. Sem este
     * bean, respostas de bloqueio do filtro de borda nao teriam os headers
     * CORS, e o navegador nao conseguiria ler o Problem Details.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(List.of(origensPermitidas));
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("*"));
        configuracao.setAllowCredentials(true);
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }
}
