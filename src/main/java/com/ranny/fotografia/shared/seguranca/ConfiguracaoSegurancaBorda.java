package com.ranny.fotografia.shared.seguranca;

import com.ranny.fotografia.shared.seguranca.borda.FiltroBordaSeguranca;
import com.ranny.fotografia.shared.seguranca.borda.PropriedadesSegurancaBorda;
import com.ranny.fotografia.shared.seguranca.borda.RespostaProblemaSeguranca;
import com.ranny.fotografia.shared.seguranca.borda.ip.AvaliadorAcessoIp;
import com.ranny.fotografia.shared.seguranca.borda.ip.AvaliadorAcessoIpLiberado;
import com.ranny.fotografia.shared.seguranca.borda.validador.ValidadorRequisicaoBorda;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Composition Root da camada de borda.
 *
 * <p>PATTERN: Composition Root + Dependency Injection.
 *
 * <p>O filtro e os validadores sao beans explicitos; a lista de validadores e
 * injetada na ordem definida por {@code @Order}. O avaliador de IP default so e
 * criado se nenhum adaptador (Redis, por exemplo) estiver presente.
 */
@Configuration
@EnableConfigurationProperties(PropriedadesSegurancaBorda.class)
public class ConfiguracaoSegurancaBorda {

    @Bean
    @ConditionalOnMissingBean(AvaliadorAcessoIp.class)
    public AvaliadorAcessoIp avaliadorAcessoIpLiberado() {
        return new AvaliadorAcessoIpLiberado();
    }

    @Bean
    public FiltroBordaSeguranca filtroBordaSeguranca(
            PropriedadesSegurancaBorda propriedades,
            List<ValidadorRequisicaoBorda> validadores,
            RespostaProblemaSeguranca respostaProblema) {
        return new FiltroBordaSeguranca(propriedades, validadores, respostaProblema);
    }
}
