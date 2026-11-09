package com.ranny.fotografia.auth.security;

import com.ranny.fotografia.shared.seguranca.borda.FiltroBordaSeguranca;
import com.ranny.fotografia.shared.seguranca.borda.RespostaProblemaSeguranca;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;

/**
 * Cadeia de seguranca da aplicacao.
 *
 * <p>Rotas publicas continuam abertas (aceite, selecao e entrega por token).
 * O restante da API exige JWT. A gestao de usuarios e exclusiva de ADMIN.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class ConfiguracaoSegurancaWeb {

    private final RespostaProblemaSeguranca respostaProblema;

    public ConfiguracaoSegurancaWeb(RespostaProblemaSeguranca respostaProblema) {
        this.respostaProblema = respostaProblema;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FiltroBordaSeguranca filtroBorda,
            ContaAtivaFilter contaAtivaFilter,
            TrocaSenhaObrigatoriaFilter trocaSenhaFilter,
            AuthOriginFilter authOriginFilter,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            @Value("${app.security.enabled:true}") boolean segurancaHabilitada) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao ->
                        sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterAfter(filtroBorda, CorsFilter.class);

        if (!segurancaHabilitada) {
            http.authorizeHttpRequests(autorizacao -> autorizacao.anyRequest().permitAll());
            return http.build();
        }

        http
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, ex) ->
                                respostaProblema.escrever(response, request,
                                        HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO",
                                        "Autenticacao necessaria.")))
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint((request, response, ex) ->
                                respostaProblema.escrever(response, request,
                                        HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO",
                                        "Autenticacao necessaria."))
                        .accessDeniedHandler((request, response, ex) ->
                                respostaProblema.escrever(response, request,
                                        HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                                        "Acesso negado.")))
                .addFilterAfter(contaAtivaFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(trocaSenhaFilter, ContaAtivaFilter.class)
                .addFilterAfter(authOriginFilter, CorsFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "registroFiltroBorda")
    public FilterRegistrationBean<FiltroBordaSeguranca> registroFiltroBorda(
            FiltroBordaSeguranca filtroBorda) {
        FilterRegistrationBean<FiltroBordaSeguranca> registro =
                new FilterRegistrationBean<>(filtroBorda);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<ContaAtivaFilter> registroContaAtiva(ContaAtivaFilter filtro) {
        FilterRegistrationBean<ContaAtivaFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<TrocaSenhaObrigatoriaFilter> registroTrocaSenha(
            TrocaSenhaObrigatoriaFilter filtro) {
        FilterRegistrationBean<TrocaSenhaObrigatoriaFilter> registro =
                new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<AuthOriginFilter> registroAuthOrigin(AuthOriginFilter filtro) {
        FilterRegistrationBean<AuthOriginFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }
}
