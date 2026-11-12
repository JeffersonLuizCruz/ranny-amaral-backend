package com.ranny.fotografia.notificacao;

/** Comando de envio de WhatsApp; independente do provedor escolhido. */
public record NotificacaoWhatsAppCommand(
        String galeriaId,
        String clienteId,
        String clienteNome,
        String whatsapp,
        String link,
        int quantidadeLiberada,
        int totalLiberado,
        int totalSelecionado
) {
}
