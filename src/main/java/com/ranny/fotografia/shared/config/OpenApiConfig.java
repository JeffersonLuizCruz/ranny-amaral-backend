package com.ranny.fotografia.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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
                        .contact(new Contact().name("Estudio Fotografico")));
    }
}
