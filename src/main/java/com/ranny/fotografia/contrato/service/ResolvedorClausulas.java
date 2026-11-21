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
import java.util.ArrayList;
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
            Instant dataValidade,
            BigDecimal valorTotal,
            BigDecimal valorEntrada,
            BigDecimal percentualEntrada,
            BigDecimal saldoRestante,
            String formaPagamento
    ) {
    }

    public List<ContratoClausula> resolver(List<ContratoClausula> modelo, ContratoSnapshot snapshot,
                                           DadosResolucao dados) {
        List<ContratoClausula> fonte = comClausulaDeslocamento(modelo, snapshot, dados);
        Map<String, String> variaveis = variaveis(snapshot, dados);
        return fonte.stream()
                .map(clausula -> new ContratoClausula(
                        clausula.id(),
                        clausula.titulo(),
                        substituir(clausula.corpo(), variaveis)))
                .toList();
    }

    /**
     * Insere a clausula de deslocamento antes da clausula de pagamento. O texto
     * e gerado pelo sistema para garantir o mesmo disclosure em qualquer modelo;
     * se o modelo ja usar os tokens de deslocamento, nada e inserido.
     */
    private static List<ContratoClausula> comClausulaDeslocamento(
            List<ContratoClausula> modelo, ContratoSnapshot snapshot, DadosResolucao dados) {
        if (modelo == null || modelo.isEmpty()) {
            return modelo;
        }
        ContratoSnapshot.DeslocamentoSnapshot deslocamento =
                snapshot == null ? null : snapshot.deslocamento();
        if (deslocamento == null || !deslocamento.repassado()
                || deslocamento.valor() == null || deslocamento.valor().signum() <= 0) {
            return modelo;
        }
        boolean jaUtilizaTokens = modelo.stream().anyMatch(clausula ->
                clausula.corpo() != null && clausula.corpo().contains("{{deslocamento."));
        if (jaUtilizaTokens) {
            return modelo;
        }

        ContratoClausula sistema = new ContratoClausula(
                "cl-deslocamento-sistema",
                "Deslocamento",
                descricaoDeslocamento(deslocamento, dados.valorTotal()));
        List<ContratoClausula> resultado = new ArrayList<>(modelo);
        int indicePagamento = -1;
        int indiceComToken = -1;
        for (int i = 0; i < resultado.size(); i++) {
            ContratoClausula clausula = resultado.get(i);
            String corpo = clausula.corpo();
            if (corpo == null || !corpo.contains("{{contrato.valorTotal}}")) {
                continue;
            }
            if (indiceComToken < 0) {
                indiceComToken = i;
            }
            String titulo = clausula.titulo() == null ? "" : clausula.titulo().toLowerCase(Locale.ROOT);
            if (titulo.contains("pagamento")) {
                indicePagamento = i;
                break;
            }
        }
        int indice = indicePagamento >= 0 ? indicePagamento : indiceComToken;
        if (indice < 0) {
            resultado.add(sistema);
        } else {
            resultado.add(indice, sistema);
        }
        return resultado;
    }

    /** Texto unico usado pela clausula de sistema e pelo token da paleta. */
    public static String descricaoDeslocamento(ContratoSnapshot.DeslocamentoSnapshot deslocamento,
                                               BigDecimal valorTotal) {
        if (deslocamento == null || !deslocamento.repassado()
                || deslocamento.valor() == null || deslocamento.valor().signum() <= 0) {
            return "";
        }
        return "A taxa de deslocamento de %s foi repassada ao contratante e está considerada no valor total de %s."
                .formatted(moeda(deslocamento.valor()), moeda(valorTotal));
    }

    private Map<String, String> variaveis(ContratoSnapshot snapshot, DadosResolucao dados) {
        Map<String, String> variaveis = new HashMap<>();
        ContratoSnapshot.EstudioSnapshot estudio = snapshot.estudio();
        if (estudio != null) {
            variaveis.put("estudio.nome", texto(estudio.nome()));
            variaveis.put("estudio.razaoSocial", texto(estudio.razaoSocial()));
            variaveis.put("estudio.cnpj", texto(estudio.cnpj()));
            variaveis.put("estudio.endereco", texto(estudio.endereco()));
            variaveis.put("estudio.cidade", texto(estudio.cidade()));
            variaveis.put("estudio.estado", texto(estudio.estado()));
            variaveis.put("estudio.email", texto(estudio.email()));
            variaveis.put("estudio.telefone", texto(estudio.telefone()));
            variaveis.put("estudio.responsavel", texto(estudio.responsavel()));
            variaveis.put("estudio.chavePix", texto(estudio.chavePix()));
            variaveis.put("estudio.instrucoesPagamento", texto(estudio.instrucoesPagamento()));
            variaveis.put("estudio.foro", texto(estudio.foro()));
            variaveis.put("estudio.percentualEntradaPadrao",
                    estudio.percentualEntradaPadrao() == null ? ""
                            : texto(estudio.percentualEntradaPadrao()) + "%");
        }
        ContratoSnapshot.ClienteSnapshot cliente = snapshot.cliente();
        if (cliente != null) {
            variaveis.put("cliente.nome", texto(cliente.nome()));
            variaveis.put("cliente.whatsapp", texto(cliente.whatsapp()));
            variaveis.put("cliente.email", texto(cliente.email()));
            variaveis.put("cliente.documento", texto(cliente.documento()));
            variaveis.put("cliente.cidade", texto(cliente.cidade()));
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
        if (agendamento != null) {
            variaveis.put("agendamento.valor", moeda(agendamento.valorAcordado()));
        }
        variaveis.put("contrato.numero", texto(dados.numero()));
        variaveis.put("contrato.dataEmissao", data(dados.dataEmissao()));
        variaveis.put("contrato.validade", data(dados.dataValidade()));
        variaveis.put("contrato.valorTotal", moeda(dados.valorTotal()));
        variaveis.put("contrato.valorEntrada", moeda(dados.valorEntrada()));
        variaveis.put("contrato.percentualEntrada", texto(dados.percentualEntrada()) + "%");
        variaveis.put("contrato.saldoRestante", moeda(dados.saldoRestante()));
        variaveis.put("contrato.formaPagamento", texto(dados.formaPagamento()));
        ContratoSnapshot.DeslocamentoSnapshot deslocamento = snapshot.deslocamento();
        if (deslocamento != null) {
            variaveis.put("deslocamento.valor", moeda(deslocamento.valor()));
            variaveis.put("deslocamento.descricao",
                    descricaoDeslocamento(deslocamento, dados.valorTotal()));
        }
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

    private static String data(Instant valor) {
        if (valor == null) {
            return "";
        }
        return DATA.format(valor.atZone(FUSO));
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }
}
