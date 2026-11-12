package com.ranny.fotografia.notificacao;

/**
 * Porta de saida para WhatsApp.
 *
 * <p>A implementacao real (Meta Cloud API, Twilio, Z-API etc.) entra no futuro
 * sem acoplar o modulo de galeria ao provedor.
 */
public interface NotificacaoWhatsAppPort {

    void enviar(NotificacaoWhatsAppCommand comando);
}
