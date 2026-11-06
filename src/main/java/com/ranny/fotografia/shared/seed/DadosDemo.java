package com.ranny.fotografia.shared.seed;

/**
 * Identificadores estaveis do seed de demonstracao.
 *
 * <p>PATTERN: Constant Object / Shared Kernel.
 *
 * <p>Os seeds de modulos diferentes precisam referenciar os mesmos IDs sem
 * depender uns dos outros. Centralizar as constantes no Shared Kernel evita
 * strings magicas duplicadas e mantem os dados de demonstracao coerentes com os
 * IDs usados pelo frontend.
 */
public final class DadosDemo {

    public static final String CLIENTE_MARINA = "cli-marina";
    public static final String CLIENTE_RAFAEL = "cli-rafael";
    public static final String CLIENTE_JULIANA = "cli-juliana";

    public static final String PACOTE_GESTANTE = "pkg-gestante";
    public static final String PACOTE_CORPORATIVO = "pkg-corporativo";
    public static final String PACOTE_FAMILIA = "pkg-familia";
    public static final String PACOTE_RETRATO = "pkg-retrato-promo";

    public static final String AGENDAMENTO_GESTANTE = "apt-001";
    public static final String AGENDAMENTO_CORPORATIVO = "apt-002";
    public static final String AGENDAMENTO_FAMILIA = "apt-003";

    public static final String MODELO_PADRAO = "tpl-padrao";
    public static final String CONTRATO_GESTANTE = "ct-001";
    public static final String CONTRATO_CORPORATIVO = "ct-002";
    public static final String CONTRATO_FAMILIA = "ct-003";

    public static final String LINK_MARINA = "demo-aceite-marina";
    public static final String LINK_FAMILIA = "demo-aceite-familia";

    private DadosDemo() {
    }
}
