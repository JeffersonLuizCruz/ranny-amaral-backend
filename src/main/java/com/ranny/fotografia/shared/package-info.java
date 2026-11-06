/**
 * {@code shared} e o Shared Kernel do projeto: guarda apenas conceitos que
 * realmente atravessam varios bounded contexts (auditoria, pagamentos,
 * documentos JSON, erros de API, eventos de integracao e portas de inversao).
 *
 * <p>O modulo e declarado {@link org.springframework.modulith.ApplicationModule.Type#OPEN}
 * porque seus tipos sao infraestrutura deliberadamente publica. Sem isso o
 * Spring Modulith acusaria violacao de fronteira em cada modulo que dependesse
 * de um value object compartilhado.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.ranny.fotografia.shared;

import org.springframework.modulith.ApplicationModule;
