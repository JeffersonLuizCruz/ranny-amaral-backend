package com.ranny.fotografia.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PATTERN: Factory Method aplicado a configuracao.
 *
 * <p>O {@code @Bean} encapsula a construcao do documento OpenAPI. Os controllers
 * nao sabem como a documentacao e montada; apenas declaram suas operacoes.
 *
 * <p>Beneficios: metadados da API em um unico ponto, facilitando versionamento e
 * geracao de clientes.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fotografiaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fotografia CRM API")
                        .version("v1")
                        .description("API de clientes, pacotes, agenda e contratos do estudio fotografico.")
                        .contact(new Contact().name("Estudio Fotografico")))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
