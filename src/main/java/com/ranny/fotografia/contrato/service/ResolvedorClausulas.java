package com.ranny.fotografia.contrato.service;

import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot;
import com.ranny.fotografia.shared.model.FaixaFotoExtraSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PATTERN: Template Method (resolucao de placeholders) + Strategy de formatacao.
 *
 * <p>Os modelos guardam placeholders ({@code {{cliente.nome}}}) que precisam ser
 * substituidos pelos dados reais no momento da emissao. O resolvedor aplica um
 * mapa de variaveis sobre as clausulas imutaveis, sem alterar o modelo original
 * — o contrato guarda tanto o texto resolvido quanto o template.
 *
 * <p>Beneficios: o PDF/contrato nasce pronto no backend, e a formatacao
 * (moeda, data, hora) fica centralizada e consistente.
 */
@Component
public class ResolvedorClausulas {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", BRASIL);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", BRASIL);

    public record DadosResolucao(
            String numero,
            Instant dataEmissao,
            BigDecimal valorTotal,
            BigDecimal valorEntrada,
            BigDecimal percentualEntrada,
            BigDecimal saldoRestante,
            String formaPagamento
    ) {
    }

    public List<ContratoClausula> resolver(List<ContratoClausula> modelo, ContratoSnapshot snapshot,
                                           DadosResolucao dados) {
        Map<String, String> variaveis = variaveis(snapshot, dados);
        return modelo.stream()
                .map(clausula -> new ContratoClausula(
                        clausula.id(),
                        clausula.titulo(),
                        substituir(clausula.corpo(), variaveis)))
                .toList();
    }

    private Map<String, String> variaveis(ContratoSnapshot snapshot, DadosResolucao dados) {
        Map<String, String> variaveis = new HashMap<>();
        ContratoSnapshot.EstudioSnapshot estudio = snapshot.estudio();
        if (estudio != null) {
            variaveis.put("estudio.nome", texto(estudio.nome()));
            variaveis.put("estudio.cnpj", texto(estudio.cnpj()));
            variaveis.put("estudio.endereco", texto(estudio.endereco()));
            variaveis.put("estudio.cidade", texto(estudio.cidade()));
            variaveis.put("estudio.estado", texto(estudio.estado()));
            variaveis.put("estudio.chavePix", texto(estudio.chavePix()));
            variaveis.put("estudio.instrucoesPagamento", texto(estudio.instrucoesPagamento()));
            variaveis.put("estudio.foro", texto(estudio.foro()));
        }
        ContratoSnapshot.ClienteSnapshot cliente = snapshot.cliente();
        if (cliente != null) {
            variaveis.put("cliente.nome", texto(cliente.nome()));
            variaveis.put("cliente.whatsapp", texto(cliente.whatsapp()));
            variaveis.put("cliente.email", texto(cliente.email()));
        }
        ContratoSnapshot.PacoteSnapshot pacote = snapshot.pacote();
        if (pacote != null) {
            variaveis.put("pacote.nome", texto(pacote.nome()));
            variaveis.put("pacote.duracao", pacote.duracaoMinutos() + " minutos");
            variaveis.put("pacote.valor", moeda(pacote.valorEfetivo() == null ? pacote.valor() : pacote.valorEfetivo()));
            variaveis.put("pacote.itens", String.join(", ", pacote.itensInclusos() == null ? List.of() : pacote.itensInclusos()));
            variaveis.put("pacote.fotosInclusas", String.valueOf(pacote.quantidadeFotosInclusas()));
            variaveis.put("pacote.videosInclusos", String.valueOf(pacote.quantidadeVideosInclusos()));
            variaveis.put("pacote.valorFotoExtra", moeda(pacote.valorFotoExtra()));
            variaveis.put("pacote.regraFotoExtra",
                    regraFotoExtra(pacote.valorFotoExtra(), pacote.faixasFotoExtra()));
        }
        ContratoSnapshot.AgendamentoSnapshot agendamento = snapshot.agendamento();
        if (agendamento != null && agendamento.dataHoraInicio() != null) {
            var dataHora = agendamento.dataHoraInicio().atZoneSameInstant(FUSO);
            variaveis.put("agendamento.data", DATA.format(dataHora));
            variaveis.put("agendamento.horario", HORA.format(dataHora));
            variaveis.put("agendamento.local", texto(agendamento.local()));
        }
        variaveis.put("contrato.valorTotal", moeda(dados.valorTotal()));
        variaveis.put("contrato.valorEntrada", moeda(dados.valorEntrada()));
        variaveis.put("contrato.percentualEntrada", texto(dados.percentualEntrada()) + "%");
        variaveis.put("contrato.saldoRestante", moeda(dados.saldoRestante()));
        variaveis.put("contrato.formaPagamento", texto(dados.formaPagamento()));
        return variaveis;
    }

    private static String substituir(String texto, Map<String, String> variaveis) {
        if (texto == null) {
            return null;
        }
        String resultado = texto;
        for (Map.Entry<String, String> entrada : variaveis.entrySet()) {
            resultado = resultado.replace("{{" + entrada.getKey() + "}}", entrada.getValue());
        }
        return resultado;
    }

    private static String regraFotoExtra(BigDecimal valorBase,
                                         List<FaixaFotoExtraSnapshot> faixas) {
        if (faixas == null || faixas.isEmpty()) {
            return moeda(valorBase) + " por foto extra";
        }
        List<FaixaFotoExtraSnapshot> ordenadas = faixas.stream()
                .filter(faixa -> faixa != null && faixa.quantidadeMinima() > 0)
                .sorted(java.util.Comparator.comparingInt(FaixaFotoExtraSnapshot::quantidadeMinima))
                .toList();
        if (ordenadas.isEmpty()) {
            return moeda(valorBase) + " por foto extra";
        }
        StringBuilder texto = new StringBuilder();
        int primeiraFaixa = ordenadas.get(0).quantidadeMinima();
        if (primeiraFaixa > 2) {
            texto.append(moeda(valorBase))
                    .append(" da 1a a ").append(primeiraFaixa - 1).append("a foto extra");
        } else if (primeiraFaixa == 2) {
            texto.append(moeda(valorBase)).append(" na 1a foto extra");
        }
        for (int i = 0; i < ordenadas.size(); i++) {
            if (texto.length() > 0) {
                texto.append("; ");
            }
            FaixaFotoExtraSnapshot faixa = ordenadas.get(i);
            int proxima = i + 1 < ordenadas.size()
                    ? ordenadas.get(i + 1).quantidadeMinima()
                    : Integer.MAX_VALUE;
            texto.append(moeda(faixa.valorUnitario())).append(" da ")
                    .append(faixa.quantidadeMinima()).append("a foto extra");
            if (proxima != Integer.MAX_VALUE) {
                texto.append(" a ").append(proxima - 1).append("a");
            } else {
                texto.append(" em diante");
            }
        }
        return texto.toString();
    }

    private static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor == null ? BigDecimal.ZERO : valor);
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }
}
