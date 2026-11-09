package com.ranny.fotografia.shared.seguranca.borda

import com.ranny.fotografia.shared.seguranca.borda.ip.AvaliadorAcessoIp
import com.ranny.fotografia.shared.seguranca.borda.ip.DecisaoAcessoIp
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Test double do ponto de extensao de IP.
 *
 * <p>PATTERN: Test Double (Stub) + Dependency Injection. O filtro real e
 * exercitado; apenas a estrategia de IP e substituida para simular bloqueio.
 */
@TestConfiguration
class ConfiguracaoTesteBorda {

    @Bean
    @Primary
    AvaliadorAcessoIp avaliadorAcessoIpTeste() {
        return new AvaliadorAcessoIp() {
            @Override
            DecisaoAcessoIp avaliar(String ip, HttpServletRequest request) {
                return "10.9.9.9" == ip
                        ? DecisaoAcessoIp.bloqueado("IP de teste")
                        : DecisaoAcessoIp.liberado()
            }
        }
    }
}
