package com.ranny.fotografia.contrato.service;

import com.ranny.fotografia.contrato.repository.ContratoRepository;
import com.ranny.fotografia.contrato.repository.SolicitacaoAceiteRepository;
import org.springframework.stereotype.Component;

/**
 * PATTERN: Sequence Generator (Strategy de numeracao).
 *
 * <p>Contratos usam {@code CT-AAAA-0001} e aceites {@code ACE-AAAA-0001}. O
 * gerador consulta o maior numero do ano e incrementa. O metodo e
 * {@code synchronized} para evitar duplicidade dentro de uma instancia; a
 * constraint UNIQUE e a segunda linha de defesa.
 *
 * <p>Limitacao conhecida: em cluster multi-instancia a numeracao deve migrar
 * para uma sequence do banco. O comentario fica explicito para a proxima fase.
 */
@Component
public class ContratoNumerador {

    private final ContratoRepository contratoRepository;
    private final SolicitacaoAceiteRepository solicitacaoRepository;

    public ContratoNumerador(ContratoRepository contratoRepository,
                             SolicitacaoAceiteRepository solicitacaoRepository) {
        this.contratoRepository = contratoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
    }

    public synchronized String proximoNumero(int ano) {
        String prefixo = "CT-%d-".formatted(ano);
        int proximo = contratoRepository.findTopByNumeroStartingWithOrderByNumeroDesc(prefixo)
                .map(contrato -> sequencia(contrato.getNumero(), prefixo))
                .orElse(0) + 1;
        return prefixo + "%04d".formatted(proximo);
    }

    public synchronized String proximoProtocolo(int ano) {
        String prefixo = "ACE-%d-".formatted(ano);
        int proximo = solicitacaoRepository.findTopByProtocoloStartingWithOrderByProtocoloDesc(prefixo)
                .map(solicitacao -> sequencia(solicitacao.getProtocolo(), prefixo))
                .orElse(0) + 1;
        return prefixo + "%04d".formatted(proximo);
    }

    private static int sequencia(String valor, String prefixo) {
        if (valor == null || !valor.startsWith(prefixo)) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.substring(prefixo.length()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
