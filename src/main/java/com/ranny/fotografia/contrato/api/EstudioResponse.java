package com.ranny.fotografia.contrato.api;

import com.ranny.fotografia.contrato.model.EstudioConfig;

import java.math.BigDecimal;

/** DTO de saida da configuracao do estudio. */
public record EstudioResponse(
        String nome,
        String razaoSocial,
        String cnpj,
        String endereco,
        String cidade,
        String estado,
        String email,
        String telefone,
        String responsavel,
        String site,
        String foro,
        BigDecimal percentualEntradaPadrao,
        String chavePix,
        String instrucoesPagamento,
        String chavePixExtras,
        String instrucoesPagamentoExtras,
        Integer prazoPagamentoDias
) {

    public static EstudioResponse de(EstudioConfig estudio) {
        return new EstudioResponse(
                estudio.getNome(),
                estudio.getRazaoSocial(),
                estudio.getCnpj(),
                estudio.getEndereco(),
                estudio.getCidade(),
                estudio.getEstado(),
                estudio.getEmail(),
                estudio.getTelefone(),
                estudio.getResponsavel(),
                estudio.getSite(),
                estudio.getForo(),
                estudio.getPercentualEntradaPadrao(),
                estudio.getChavePix(),
                estudio.getInstrucoesPagamento(),
                estudio.getChavePixExtras(),
                estudio.getInstrucoesPagamentoExtras(),
                estudio.getPrazoPagamentoDias());
    }
}
