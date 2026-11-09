package com.ranny.fotografia.shared.seguranca.borda.validador;

import com.ranny.fotografia.shared.seguranca.borda.ProblemaBorda;
import com.ranny.fotografia.shared.seguranca.borda.PropriedadesSegurancaBorda;
import com.ranny.fotografia.shared.seguranca.borda.ip.AvaliadorAcessoIp;
import com.ranny.fotografia.shared.seguranca.borda.ip.DecisaoAcessoIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Consulta a estrategia de bloqueio por IP/cache.
 *
 * <p>Executa depois da checagem barata de tamanho para que um payload gigante
 * seja rejeitado antes de qualquer consulta externa.
 */
@Component
@Order(20)
public class ValidadorAcessoIp implements ValidadorRequisicaoBorda {

    private static final Logger log = LoggerFactory.getLogger(ValidadorAcessoIp.class);

    private final PropriedadesSegurancaBorda propriedades;
    private final AvaliadorAcessoIp avaliador;

    public ValidadorAcessoIp(PropriedadesSegurancaBorda propriedades, AvaliadorAcessoIp avaliador) {
        this.propriedades = propriedades;
        this.avaliador = avaliador;
    }

    @Override
    public Optional<ProblemaBorda> validar(HttpServletRequest request) {
        if (!propriedades.isIpCheckHabilitado()) {
            return Optional.empty();
        }
        String ip = resolverIp(request);
        DecisaoAcessoIp decisao = avaliador.avaliar(ip, request);
        if (decisao == null || decisao.permitido()) {
            return Optional.empty();
        }
        log.warn("Acesso bloqueado para IP {} em {}: {}",
                ip, request.getRequestURI(), decisao.motivo());
        return Optional.of(ProblemaBorda.acessoNegado());
    }

    private String resolverIp(HttpServletRequest request) {
        if (propriedades.isConfiarEmProxy()) {
            String encaminhado = request.getHeader("X-Forwarded-For");
            if (encaminhado != null && !encaminhado.isBlank()) {
                String primeiro = encaminhado.split(",")[0].trim();
                if (!primeiro.isEmpty()) {
                    return primeiro;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
