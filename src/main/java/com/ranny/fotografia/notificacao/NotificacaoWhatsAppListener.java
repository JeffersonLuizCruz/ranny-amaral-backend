package com.ranny.fotografia.notificacao;

import com.ranny.fotografia.cliente.ClienteConsulta;
import com.ranny.fotografia.shared.events.LoteLiberadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reage a liberacao de um lote e aciona a porta de WhatsApp.
 *
 * <p>O evento e durable (outbox do Spring Modulith). O adapter padrao e no-op;
 * quando o provedor for configurado, apenas a implementacao da porta muda.
 */
@Component
public class NotificacaoWhatsAppListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoWhatsAppListener.class);

    private final NotificacaoWhatsAppPort porta;
    private final NotificacaoWhatsAppProperties propriedades;
    private final ClienteConsulta clienteConsulta;

    public NotificacaoWhatsAppListener(NotificacaoWhatsAppPort porta,
                                       NotificacaoWhatsAppProperties propriedades,
                                       ClienteConsulta clienteConsulta) {
        this.porta = porta;
        this.propriedades = propriedades;
        this.clienteConsulta = clienteConsulta;
    }

    @ApplicationModuleListener
    public void aoLiberarLote(LoteLiberadoEvent evento) {
        if (evento.clienteId() == null || evento.clienteId().isBlank()) {
            log.debug("Galeria {} sem cliente; notificacao ignorada.", evento.galeriaId());
            return;
        }
        if (evento.tokenLink() == null || evento.tokenLink().isBlank()) {
            log.debug("Galeria {} sem link publico; notificacao ignorada.",
                    evento.galeriaId());
            return;
        }
        var cliente = clienteConsulta.buscar(evento.clienteId()).orElse(null);
        if (cliente == null || cliente.whatsapp() == null || cliente.whatsapp().isBlank()) {
            log.debug("Cliente {} sem WhatsApp; notificacao ignorada.", evento.clienteId());
            return;
        }
        String link = propriedades.publicBaseUrl() == null
                ? null
                : propriedades.publicBaseUrl().replaceAll("/+$", "") + "/galeria/"
                        + evento.tokenLink();
        porta.enviar(new NotificacaoWhatsAppCommand(
                evento.galeriaId(),
                evento.clienteId(),
                cliente.nome(),
                cliente.whatsapp(),
                link,
                evento.quantidadeLiberada(),
                evento.totalLiberado(),
                evento.totalSelecionado()));
    }
}
