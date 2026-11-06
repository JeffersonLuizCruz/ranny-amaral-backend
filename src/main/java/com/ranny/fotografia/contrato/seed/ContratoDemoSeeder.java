package com.ranny.fotografia.contrato.seed;

import com.ranny.fotografia.contrato.model.Contrato;
import com.ranny.fotografia.contrato.model.DadosContrato;
import com.ranny.fotografia.contrato.model.EstudioConfig;
import com.ranny.fotografia.contrato.model.ModeloContrato;
import com.ranny.fotografia.contrato.model.SolicitacaoAceite;
import com.ranny.fotografia.contrato.model.documento.ContratoClausula;
import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot;
import com.ranny.fotografia.contrato.model.documento.DadosAceite;
import com.ranny.fotografia.contrato.model.documento.StatusSolicitacaoAceite;
import com.ranny.fotografia.contrato.repository.ContratoRepository;
import com.ranny.fotografia.contrato.repository.EstudioConfigRepository;
import com.ranny.fotografia.contrato.repository.ModeloContratoRepository;
import com.ranny.fotografia.contrato.repository.SolicitacaoAceiteRepository;
import com.ranny.fotografia.contrato.service.ResolvedorClausulas;
import com.ranny.fotografia.contrato.service.SnapshotFactory;
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo;
import com.ranny.fotografia.shared.model.TipoDocumento;
import com.ranny.fotografia.shared.seed.DadosDemo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Seed idempotente de estudio, modelo e contratos de demonstracao.
 *
 * <p>Usa a mesma montagem de snapshot e resolucao de clausulas do fluxo real,
 * garantindo que os dados de demonstracao passem pelas mesmas regras de
 * dominio (e nao por inserts manuais).
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(40)
public class ContratoDemoSeeder implements ApplicationRunner {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ContratoRepository contratoRepository;
    private final SolicitacaoAceiteRepository solicitacaoRepository;
    private final ModeloContratoRepository modeloRepository;
    private final EstudioConfigRepository estudioRepository;
    private final SnapshotFactory snapshotFactory;
    private final ResolvedorClausulas resolvedor;

    public ContratoDemoSeeder(ContratoRepository contratoRepository,
                              SolicitacaoAceiteRepository solicitacaoRepository,
                              ModeloContratoRepository modeloRepository,
                              EstudioConfigRepository estudioRepository,
                              SnapshotFactory snapshotFactory,
                              ResolvedorClausulas resolvedor) {
        this.contratoRepository = contratoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.modeloRepository = modeloRepository;
        this.estudioRepository = estudioRepository;
        this.snapshotFactory = snapshotFactory;
        this.resolvedor = resolvedor;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (contratoRepository.count() > 0) {
            return;
        }
        int ano = LocalDate.now(FUSO).getYear();
        EstudioConfig estudio = estudioRepository.findById(EstudioConfig.ID)
                .orElseGet(() -> estudioRepository.save(EstudioConfig.restaurar(
                        "Photoizer Estúdio",
                        "Photoizer Fotografia LTDA",
                        "00.000.000/0001-00",
                        "Rua das Flores, 123",
                        "São Paulo",
                        "SP",
                        "contato@photoizer.com.br",
                        "(11) 99999-9999",
                        "Ana Souza",
                        "www.photoizer.com.br",
                        "São Paulo/SP",
                        new BigDecimal("30.00"),
                        "pix@photoizer.com.br",
                        "Pague a entrada via Pix e anexe o comprovante nesta página. O saldo restante pode ser quitado no dia do ensaio.",
                        3)));

        ModeloContrato modelo = modeloRepository.findById(DadosDemo.MODELO_PADRAO)
                .orElseGet(() -> modeloRepository.save(ModeloContrato.restaurar(
                        DadosDemo.MODELO_PADRAO,
                        "Contrato de Prestação de Serviços Fotográficos",
                        "Modelo padrão para ensaios fotográficos, com cláusulas de entrega, direitos de imagem, pagamento, cancelamento e LGPD.",
                        clausulas(),
                        true,
                        1)));

        Contrato gestante = criarContrato(
                DadosDemo.CONTRATO_GESTANTE, "CT-%d-0001".formatted(ano),
                DadosDemo.CLIENTE_MARINA, DadosDemo.AGENDAMENTO_GESTANTE, DadosDemo.PACOTE_GESTANTE,
                modelo, new BigDecimal("890.00"), new BigDecimal("267.00"));
        SolicitacaoAceite linkMarina = SolicitacaoAceite.restaurar(
                "req-001", gestante.getId(), DadosDemo.LINK_MARINA,
                StatusSolicitacaoAceite.PENDENTE,
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(7, ChronoUnit.DAYS),
                null, null, null);
        solicitacaoRepository.save(linkMarina);
        gestante.registrarLink(linkMarina);

        Contrato corporativo = criarContrato(
                DadosDemo.CONTRATO_CORPORATIVO, "CT-%d-0002".formatted(ano),
                null, DadosDemo.AGENDAMENTO_CORPORATIVO, DadosDemo.PACOTE_CORPORATIVO,
                modelo, new BigDecimal("1490.00"), new BigDecimal("447.00"));

        Contrato familia = criarContrato(
                DadosDemo.CONTRATO_FAMILIA, "CT-%d-0003".formatted(ano),
                DadosDemo.CLIENTE_MARINA, DadosDemo.AGENDAMENTO_FAMILIA, DadosDemo.PACOTE_FAMILIA,
                modelo, new BigDecimal("590.00"), new BigDecimal("177.00"));
        DadosAceite aceite = new DadosAceite(
                "Marina Alves", TipoDocumento.CPF, "529.982.247-25", "São Paulo",
                "(11) 99876-5432", Instant.now().minus(2, ChronoUnit.DAYS),
                "ACE-%d-0001".formatted(ano), "127.0.0.1", "Demo Photoizer CRM",
                "demo-hash-aceite-familia");
        SolicitacaoAceite linkFamilia = SolicitacaoAceite.restaurar(
                "req-003", familia.getId(), DadosDemo.LINK_FAMILIA,
                StatusSolicitacaoAceite.ACEITA,
                Instant.now().minus(4, ChronoUnit.DAYS),
                Instant.now().plus(3, ChronoUnit.DAYS),
                Instant.now().minus(4, ChronoUnit.DAYS), aceite,
                "ACE-%d-0001".formatted(ano));
        solicitacaoRepository.save(linkFamilia);
        familia.registrarAceite(aceite, DadosDemo.CLIENTE_MARINA, linkFamilia.getId());
        familia.confirmarPagamento(Instant.now().minus(2, ChronoUnit.DAYS), "Equipe Photoizer");
        familia.registrarPagamentoFinal(
                new BigDecimal("413.00"), FormaPagamentoSaldo.DINHEIRO,
                Instant.now().minus(1, ChronoUnit.DAYS), "Equipe Photoizer", null);

        contratoRepository.save(gestante);
        contratoRepository.save(corporativo);
        contratoRepository.save(familia);
    }

    private Contrato criarContrato(String id, String numero, String clienteId, String agendamentoId,
                                   String pacoteId, ModeloContrato modelo,
                                   BigDecimal valorTotal, BigDecimal valorEntrada) {
        ContratoSnapshot snapshot = snapshotFactory.montar(
                null, clienteId, agendamentoId, pacoteId, modelo.getVersao());
        Instant emissao = Instant.now();
        ResolvedorClausulas.DadosResolucao dados = new ResolvedorClausulas.DadosResolucao(
                numero, emissao, valorTotal, valorEntrada, new BigDecimal("30.00"),
                valorTotal.subtract(valorEntrada), "Pix");
        List<ContratoClausula> resolvidas = resolvedor.resolver(modelo.getClausulas(), snapshot, dados);
        Contrato contrato = Contrato.criar(new DadosContrato(
                id, numero, clienteId, agendamentoId, pacoteId, modelo.getId(), modelo.getNome(),
                resolvidas, modelo.getClausulas(), snapshot, valorTotal, valorEntrada,
                new BigDecimal("30.00"), "Pix",
                "Entrada na reserva; saldo no dia do ensaio, via Pix ou dinheiro, registrado pelo estúdio.",
                emissao, null, null));
        return contrato;
    }

    private static List<ContratoClausula> clausulas() {
        return List.of(
                new ContratoClausula("cl-objeto", "1. Objeto",
                        "O presente contrato tem por objeto a prestação de serviços fotográficos por {{estudio.nome}}, "
                                + "inscrito no CNPJ {{estudio.cnpj}}, com sede em {{estudio.endereco}}, "
                                + "{{estudio.cidade}}/{{estudio.estado}}, ao contratante {{cliente.nome}}, "
                                + "WhatsApp {{cliente.whatsapp}} e e-mail {{cliente.email}}."),
                new ContratoClausula("cl-servicos", "2. Serviços contratados e entregas",
                        "O pacote contratado é {{pacote.nome}}, com duração de {{pacote.duracao}}, no valor de "
                                + "{{pacote.valor}}. Estão inclusos: {{pacote.itens}}. O pacote inclui "
                                + "{{pacote.fotosInclusas}} fotos e {{pacote.videosInclusos}} vídeos. Cada foto "
                                + "adicional custa {{pacote.valorFotoExtra}}. O ensaio ocorrerá em "
                                + "{{agendamento.data}}, às {{agendamento.horario}}, em {{agendamento.local}}."),
                new ContratoClausula("cl-imagem", "3. Direitos autorais e uso de imagem",
                        "O contratante autoriza o uso das imagens para portfólio, site e redes sociais do "
                                + "estúdio, salvo manifestação expressa em contrário por escrito."),
                new ContratoClausula("cl-pagamento", "4. Valor e forma de pagamento",
                        "O valor total do pacote é {{contrato.valorTotal}}. A entrada é de "
                                + "{{contrato.valorEntrada}} ({{contrato.percentualEntrada}}), restando "
                                + "{{contrato.saldoRestante}} para quitar até o dia do ensaio. O pagamento deve "
                                + "ser feito via {{contrato.formaPagamento}}. Chave Pix: {{estudio.chavePix}}."),
                new ContratoClausula("cl-cancelamento", "5. Cancelamento e remarcação",
                        "Cancelamentos com mais de 7 dias de antecedência permitem remarcação sem custo, "
                                + "sujeita à disponibilidade. Cancelamentos com menos de 7 dias podem implicar "
                                + "retenção da entrada."),
                new ContratoClausula("cl-lgpd", "6. Proteção de dados (LGPD)",
                        "Os dados pessoais informados serão utilizados exclusivamente para execução deste "
                                + "contrato, comunicação com o contratante e cumprimento de obrigações legais."),
                new ContratoClausula("cl-gerais", "7. Disposições gerais",
                        "Este contrato representa o acordo integral entre as partes. Fica eleito o foro de "
                                + "{{estudio.foro}} para dirimir eventuais conflitos."));
    }
}
