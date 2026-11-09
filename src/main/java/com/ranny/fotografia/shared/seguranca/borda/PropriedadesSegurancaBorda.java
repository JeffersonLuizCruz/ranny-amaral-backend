package com.ranny.fotografia.shared.seguranca.borda;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuracao da camada de borda.
 *
 * <p>PATTERN: Configuration Properties Object.
 *
 * <p>Concentra os limites e as politicas que podem variar por ambiente sem
 * alterar codigo. Os defaults sao conservadores para producao; o perfil de
 * teste desliga as regras que os specs legados nao conhecem.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.seguranca.borda")
public class PropriedadesSegurancaBorda {

    private boolean habilitado = true;

    /** Limite de bytes do corpo. 2 MiB cobre os JSONs atuais da API. */
    private long maxPayloadBytes = 2L * 1024 * 1024;

    private boolean exigirUserAgent = true;
    private int maxUserAgentLength = 512;

    private boolean exigirContentTypeEmMutacoes = true;
    private boolean bloquearPadroesScanning = true;

    /**
     * Quando desligado, a avaliacao de IP e ignorada. O ponto de extensao
     * continua no bean {@code AvaliadorAcessoIp}, mas sem custo por request.
     */
    private boolean ipCheckHabilitado = false;

    /**
     * Nao confie em X-Forwarded-For por padrao. Habilite apenas quando a
     * aplicacao estiver atras de um proxy reverso confiavel.
     */
    private boolean confiarEmProxy = false;

    private List<String> caminhosIgnorados = new ArrayList<>(List.of(
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/actuator/health",
            "/actuator/health/**",
            "/error",
            "/favicon.ico"
    ));

    /**
     * Assinaturas de ferramentas de scanning. A comparacao e feita em
     * lower-case contra o User-Agent. A lista e configuravel para evitar
     * bloquear clientes legitimos de um ambiente especifico.
     */
    private List<String> userAgentsScanners = new ArrayList<>(List.of(
            "sqlmap",
            "nikto",
            "nmap",
            "masscan",
            "nuclei",
            "zgrab",
            "dirbuster",
            "gobuster",
            "wpscan",
            "acunetix",
            "nessus",
            "openvas",
            "burp suite",
            "owasp zap"
    ));

    /**
     * Padroes obvios de path traversal e arquivos sensiveis. A checagem e
     * feita no path normalizado (URL-decoded e lower-case).
     */
    private List<String> padroesCaminhoSuspeito = new ArrayList<>(List.of(
            "../",
            "..\\",
            "/.env",
            "/.git",
            "/.svn",
            "/wp-admin",
            "/wp-login",
            "/phpmyadmin",
            "/cgi-bin",
            "/server-status"
    ));

    /**
     * Padroes checados apenas na query string. Mantidos curtos para reduzir
     * falso-positivo em buscas textuais.
     */
    private List<String> padroesQuerySuspeitos = new ArrayList<>(List.of(
            "../",
            "..\\",
            "%2e%2e",
            "%00",
            "<script"
    ));
}
