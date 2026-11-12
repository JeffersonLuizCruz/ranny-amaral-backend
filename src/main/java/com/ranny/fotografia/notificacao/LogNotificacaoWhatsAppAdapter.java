package com.ranny.fotografia.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter padrao enquanto o provedor de WhatsApp nao esta configurado.
 *
 * <p>Nao envia nada: registra a mensagem para validar o fluxo e a operacao.
 * Para ativar um provedor real, implemente {@link NotificacaoWhatsAppPort}
 * com {@code app.notificacao.whatsapp.habilitado=true}.
 */
@Component
@ConditionalOnProperty(name = "app.notificacao.whatsapp.habilitado",
        havingValue = "false", matchIfMissing = true)
public class LogNotificacaoWhatsAppAdapter implements NotificacaoWhatsAppPort {

    private static final Logger log = LoggerFactory.getLogger(LogNotificacaoWhatsAppAdapter.class);

    @Override
    public void enviar(NotificacaoWhatsAppCommand comando) {
        log.info("[WhatsApp:noop] galeria={} cliente={} telefone={} link={} lote={}/{}",
                comando.galeriaId(),
                comando.clienteNome(),
                mascarar(comando.whatsapp()),
                comando.link(),
                comando.quantidadeLiberada(),
                comando.totalLiberado());
    }

    private static String mascarar(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return "sem-telefone";
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() <= 4) {
            return "****";
        }
        return "****" + digitos.substring(digitos.length() - 4);
    }
}
