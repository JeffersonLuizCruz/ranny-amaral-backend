package com.ranny.fotografia.contrato.seed;

import com.ranny.fotografia.contrato.model.documento.ContratoClausula;

import java.util.List;

/**
 * Texto base do contrato de fotografia adaptado do contrato do projeto CRM.
 *
 * <p>As cláusulas usam os mesmos placeholders resolvidos por
 * {@link com.ranny.fotografia.contrato.service.ResolvedorClausulas}. Dados que
 * não existem no domínio atual (taxa de deslocamento, lista de profissionais e
 * estado do cliente) foram omitidos de propósito.
 */
public final class ContratoBaseCrm {

    private ContratoBaseCrm() {
    }

    public static List<ContratoClausula> clausulas() {
        return List.of(
                new ContratoClausula("cl-objeto", "1. Objeto e qualificação das partes", """
                        O presente contrato tem por objeto a prestação de serviços fotográficos por
                        {{estudio.nome}}, inscrita no CNPJ nº {{estudio.cnpj}}, com sede em
                        {{estudio.endereco}}, {{estudio.cidade}}/{{estudio.estado}}, ao contratante
                        {{cliente.nome}}, WhatsApp {{cliente.whatsapp}} e e-mail {{cliente.email}},
                        cuja qualificação completa consta no aceite eletrônico.
                        """.strip().replaceAll("\\s+", " ")),
                new ContratoClausula("cl-ensaio", "2. Informações do ensaio", """
                        O ensaio será realizado em {{agendamento.data}}, às {{agendamento.horario}},
                        com duração de {{pacote.duracao}}, em {{agendamento.local}}.
                        Fotógrafo responsável: {{estudio.responsavel}}.
                        """.strip().replaceAll("\\s+", " ")),
                new ContratoClausula("cl-pacote", "3. Pacote contratado", """
                        O pacote contratado é {{pacote.nome}}, no valor de {{pacote.valor}},
                        incluindo {{pacote.fotosInclusas}} fotos e {{pacote.videosInclusos}} vídeos.
                        Itens inclusos: {{pacote.itens}}.
                        Fotos adicionais: {{pacote.regraFotoExtra}}.
                        """.strip().replaceAll("\\s+", " ")),
                new ContratoClausula("cl-pagamento", "4. Valores e forma de pagamento", """
                        O valor total do serviço é {{contrato.valorTotal}}. A reserva é de
                        {{contrato.valorEntrada}} ({{contrato.percentualEntrada}}), restando
                        {{contrato.saldoRestante}} a ser pago ao final do ensaio. O pagamento deve
                        ser feito via {{contrato.formaPagamento}}. Chave Pix:
                        {{estudio.chavePix}}. {{estudio.instrucoesPagamento}}
                        O pagamento da reserva garante o bloqueio da data e horário na agenda da
                        Contratada.
                        """.strip().replaceAll("\\s+", " ")),
                new ContratoClausula("cl-entrega", "5. Entrega das fotografias", """
                        As fotos do ensaio serão disponibilizadas ao Cliente em até 2 (dois) dias
                        após a realização do ensaio para a seleção das imagens. Após a seleção, a
                        entrega final ocorrerá em até 2 (dois) dias. As fotos serão entregues em
                        formato digital, em alta resolução. Fotos extras além do pacote contratado
                        serão cobradas conforme a regra de foto extra acima.
                        """.strip().replaceAll("\\s+", " ")),
                new ContratoClausula("cl-cancelamento", "6. Cancelamento e remarcação", """
                        Caso o Cliente cancele o ensaio por qualquer motivo, o valor pago como
                        reserva não será reembolsado, pois garante a reserva da data na agenda da
                        Contratada. Caso ocorra algum imprevisto que impeça a presença da Contratada,
                        poderá haver a substituição por outro fotógrafo profissional de padrão
                        equivalente. Caso não seja possível a substituição, o valor pago será
                        devolvido integralmente ao Cliente. Se houver imprevisto relacionado à
                        antecipação de voo, chuva ou doença, o ensaio será cancelado e haverá o
                        reembolso completo do valor da reserva.
                        """.strip().replaceAll("\\s+", " ")),
                new ContratoClausula("cl-imagem", "7. Direitos autorais e uso de imagem", """
                        O uso das imagens para portfólio, site e redes sociais do estúdio depende de
                        autorização expressa do Contratante, registrada no aceite eletrônico deste
                        contrato. A ausência de autorização não impede a execução dos serviços
                        contratados. Os direitos autorais das imagens permanecem com a Contratada,
                        sendo vedada a reprodução comercial pelo Cliente sem autorização escrita.
                        """.strip().replaceAll("\\s+", " ")),
                new ContratoClausula("cl-gerais", "8. Proteção de dados e disposições gerais", """
                        Os dados pessoais informados serão utilizados exclusivamente para a execução
                        deste contrato, comunicação com o Contratante e cumprimento de obrigações
                        legais, nos termos da LGPD. Este contrato representa o acordo integral entre
                        as partes. Qualquer alteração deverá ser realizada por escrito. Fica eleito o
                        foro de {{estudio.foro}} para dirimir eventuais conflitos.
                        """.strip().replaceAll("\\s+", " ")));
    }
}
