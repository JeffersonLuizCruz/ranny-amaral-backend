package com.ranny.fotografia.contrato.model.documento;

import com.ranny.fotografia.shared.model.TipoDocumento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * PATTERN: Snapshot / Memento.
 *
 * <p>O contrato congela os dados de cliente, pacote, agendamento e estudio no
 * momento da emissao. Se o cadastro mudar depois, o documento assinado continua
 * fiel ao que o cliente aceitou. O snapshot e imutavel e persistido como JSON.
 *
 * <p>Beneficios: seguranca juridica, reprodutibilidade do PDF e independencia
 * entre os modulos no momento da leitura.
 */
public record ContratoSnapshot(
        ClienteSnapshot cliente,
        PacoteSnapshot pacote,
        AgendamentoSnapshot agendamento,
        EstudioSnapshot estudio,
        int modeloVersao
) {

    public record ClienteSnapshot(
            String nome,
            String whatsapp,
            String email,
            String documento,
            TipoDocumento tipoDocumento,
            String cidade
    ) {
    }

    public record PacoteSnapshot(
            String nome,
            int duracaoMinutos,
            BigDecimal valor,
            List<String> itensInclusos,
            int quantidadeFotosInclusas,
            int quantidadeVideosInclusos,
            BigDecimal valorFotoExtra,
            PromocaoSnapshot promocaoAplicada,
            BigDecimal valorEfetivo
    ) {
    }

    public record PromocaoSnapshot(BigDecimal valorPromocional, String rotulo) {
    }

    public record AgendamentoSnapshot(
            OffsetDateTime dataHoraInicio,
            OffsetDateTime dataHoraFim,
            String local,
            BigDecimal valorAcordado
    ) {
    }

    public record EstudioSnapshot(
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
            Integer prazoPagamentoDias
    ) {
    }
}
