package com.ranny.fotografia.contrato;

/**
 * API publica de leitura dos dados de pagamento do estudio.
 *
 * <p>O modulo {@code galeria} usa esta porta para exibir a chave Pix de compra
 * de fotos extras. A chave do ensaio continua sendo lida pelo contrato a partir
 * do snapshot, sem exposicao da chave de extras no documento.
 */
public interface EstudioConsulta {

    /** Nome exibido nas paginas publicas e aplicado nas previas das fotos. */
    String nome();

    DadosPagamentoExtras dadosPagamentoExtras();

    record DadosPagamentoExtras(String chavePix, String instrucoesPagamento) {
    }
}
