package com.ranny.fotografia.galeria.e2e

import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.arquivo.storage.FakeObjectStorage
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.pacote.model.FaixaFotoExtra
import com.ranny.fotografia.pacote.model.PromocaoPacote
import com.ranny.fotografia.pacote.repository.PacoteRepository
import com.ranny.fotografia.shared.model.FormaPagamentoSaldo
import com.ranny.fotografia.shared.model.PagamentoEntrada
import com.ranny.fotografia.shared.model.PagamentoSaldo
import com.ranny.fotografia.support.Cenario
import com.ranny.fotografia.support.JsonCorpo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared
import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.HexFormat
import java.util.zip.ZipInputStream

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * E2E de API com um cliente por cenário.
 *
 * <p>Gera um relatório em {@code target/relatorio-e2e/relatorio-clientes.md}
 * com o valor esperado, o obtido e o status de cada cliente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GaleriaE2ECenariosSpec extends Specification {

    private static final BigDecimal BASE = new BigDecimal("15.00")
    private static final int FOTOS_INCLUSAS = 5

    @Autowired
    MockMvc mockMvc

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PacoteRepository pacoteRepository

    @Autowired
    AgendamentoRepository agendamentoRepository

    @Autowired
    FakeObjectStorage storage

    @Shared
    List<Map<String, String>> resultados = Collections.synchronizedList([])

    @Shared
    File relatorioDir

    def setupSpec() {
        relatorioDir = new File("target/relatorio-e2e")
        relatorioDir.deleteDir()
        relatorioDir.mkdirs()
    }

    def cleanupSpec() {
        def ordenados = resultados.sort { a, b ->
            (a.id.replaceAll(/^S(\d+).*/, '$1') as int) <=>
                    (b.id.replaceAll(/^S(\d+).*/, '$1') as int)
        }
        def md = new StringBuilder()
        md << "# Relatório E2E — Galeria por cliente\n\n"
        md << "Gerado em: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern('dd/MM/yyyy HH:mm:ss'))}\n\n"
        md << "| Cliente | Cenário | Esperado | Obtido | Status |\n"
        md << "|---|---|---|---|---|\n"
        ordenados.each { r ->
            md << "| ${r.cliente} | ${r.cenario} | ${r.esperado} | ${r.obtido} | ${r.status} |\n"
        }
        ordenados.each { r ->
            md << "\n## ${r.cliente} — ${r.cenario}\n\n"
            md << "- Esperado: ${r.esperado}\n"
            md << "- Obtido: ${r.obtido}\n"
            md << "- Status: **${r.status}**\n"
            if (r.detalhe) {
                md << "- Detalhe: ${r.detalhe}\n"
            }
        }
        new File(relatorioDir, "relatorio-clientes.md").text = md.toString()
    }

    def "S1 — cliente dentro da cota, sem extras"() {
        given:
        def r = resultado("S1", "Cliente S1", "Dentro da cota", 'R$0 e 2 PRONTA liberadas')
        def p = prepararGaleria("Cliente S1", 3, 2)

        when:
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        def d = detalhe(p.galeriaId)
        def pub = publica(p.linkToken)

        r.obtido = "extras=${d.selecao.fotosExtras}; valor=${d.selecao.valorExtras}; " +
                "prontas=${d.galeria.fotosProntas}; liberadas=${d.galeria.fotosLiberadas}"
        r.detalhe = "pacote=${d.pacoteNome}; " +
                "PRONTA disponíveis=${pub.midias.count { it.disponivelParaDownload }}; " +
                "prévia marcada=${pub.midias.every { it.previaMarcada }}"

        then:
        d.selecao.fotosExtras == 0
        d.selecao.valorExtras == 0.0d
        d.galeria.fotosProntas == 2
        d.galeria.fotosLiberadas == 2
        d.pacoteNome == "Promocional Cliente S1"
        pub.pacoteNome == "Promocional Cliente S1"
        pub.midias.findAll { it.finalidade == "pronta" }.every { it.disponivelParaDownload }
        marcarPassou(r)
    }

    def "S2 — uma foto extra"() {
        given:
        def r = resultado("S2", "Cliente S2", "1 extra", 'R$15')
        def p = prepararGaleria("Cliente S2", 3, 3)

        when:
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        def antes = detalhe(p.galeriaId)
        isentarExtras(p.galeriaId)
        def depois = detalhe(p.galeriaId)
        def pub = publica(p.linkToken)

        r.obtido = "extras=${depois.selecao.fotosExtras}; valor=${depois.selecao.valorExtras}; " +
                "liberadas=${depois.galeria.fotosLiberadas}"
        r.detalhe = "antes da isenção: liberadas=${antes.galeria.fotosLiberadas}; " +
                "PRONTA disponíveis=${pub.midias.count { it.disponivelParaDownload }}"

        then:
        depois.selecao.fotosExtras == 1
        depois.selecao.valorExtras == 15.0d
        antes.galeria.fotosLiberadas == 0
        depois.galeria.fotosLiberadas == 3
        pub.midias.findAll { it.finalidade == "pronta" }.every { it.disponivelParaDownload }
        marcarPassou(r)
    }

    def "S3 — duas fotos extras"() {
        given:
        def r = resultado("S3", "Cliente S3", "2 extras", 'R$30')
        def p = prepararGaleria("Cliente S3", 4, 3)

        when:
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        isentarExtras(p.galeriaId)
        def d = detalhe(p.galeriaId)

        r.obtido = "extras=${d.selecao.fotosExtras}; valor=${d.selecao.valorExtras}; " +
                "liberadas=${d.galeria.fotosLiberadas}"
        r.detalhe = "detalhamento=${d.extras.detalhamento}"

        then:
        d.selecao.fotosExtras == 2
        d.selecao.valorExtras == 30.0d
        d.extras.detalhamento*.valorUnitario == [15.0d]
        d.galeria.fotosLiberadas == 3
        marcarPassou(r)
    }

    def "S4 — tres fotos extras pela faixa de dez reais"() {
        given:
        def r = resultado("S4", "Cliente S4", "3 extras / virada de faixa", 'R$30')
        def p = prepararGaleria("Cliente S4", 5, 3)

        when:
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        isentarExtras(p.galeriaId)
        def d = detalhe(p.galeriaId)
        def pub = publica(p.linkToken)

        r.obtido = "extras=${d.selecao.fotosExtras}; valor=${d.selecao.valorExtras}; " +
                "prontas=${d.galeria.fotosProntas}; liberadas=${d.galeria.fotosLiberadas}"
        r.detalhe = "detalhamento=${d.extras.detalhamento}; " +
                "fila=${d.galeria.fotosSelecionadas - d.galeria.fotosProntas}; " +
                "PRONTA disponíveis=${pub.midias.count { it.disponivelParaDownload }}"

        then:
        d.selecao.fotosExtras == 3
        d.selecao.valorExtras == 30.0d
        d.extras.valorMedioUnitario == 10.0d
        d.extras.detalhamento*.quantidade == [3]
        d.extras.detalhamento*.valorUnitario == [10.0d]
        d.galeria.fotosSelecionadas == 8
        d.galeria.fotosProntas == 3
        d.galeria.fotosLiberadas == 3
        marcarPassou(r)
    }

    def "S9 — reabertura de dois para tres extras, delta zero"() {
        given:
        def r = resultado("S9", "Cliente S9", "2→3 extras", 'delta R$0')
        def p = prepararGaleria("Cliente S9", 5, 3)
        selecionar(p.linkToken, p.edicaoIds[0..3] + p.prontaIds)
        isentarExtras(p.galeriaId)
        mockMvc.perform(post("/api/v1/galerias/${p.galeriaId}/selecao/reabrir"))
                .andExpect(status().isOk())

        when:
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        def d = detalhe(p.galeriaId)

        r.obtido = "extras=${d.selecao.fotosExtras}; valor=${d.selecao.valorExtras}; " +
                "coberto=${d.resumoExtras.valorCoberto}; pendente=${d.resumoExtras.valorPendente}"
        r.detalhe = "pedidos=${d.pedidosExtras.size()}; status=${d.galeria.status}"

        then:
        d.selecao.fotosExtras == 3
        d.selecao.valorExtras == 30.0d
        d.resumoExtras.quantidadeTotal == 3
        d.resumoExtras.valorTotal == 30.0d
        d.resumoExtras.quantidadePendente == 0
        d.resumoExtras.valorPendente == 0.0d
        d.pedidosExtras.size() == 1
        d.galeria.status == "em_edicao"
        marcarPassou(r)
    }

    def "S10 — reabertura de tres para quatro extras, delta dez"() {
        given:
        def r = resultado("S10", "Cliente S10", "3→4 extras", 'delta R$10')
        def p = prepararGaleria("Cliente S10", 5, 4)
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds[0..2])
        isentarExtras(p.galeriaId)
        mockMvc.perform(post("/api/v1/galerias/${p.galeriaId}/selecao/reabrir"))
                .andExpect(status().isOk())

        when:
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        def antes = detalhe(p.galeriaId)
        isentarExtras(p.galeriaId)
        def depois = detalhe(p.galeriaId)

        r.obtido = "extras=${antes.selecao.fotosExtras}; total=${antes.selecao.valorExtras}; " +
                "pendente=${antes.resumoExtras.valorPendente}"
        r.detalhe = "coberto=${antes.resumoExtras.valorCoberto}; " +
                "delta=${antes.resumoExtras.valorPendente}; " +
                "liberadas depois=${depois.galeria.fotosLiberadas}"

        then:
        antes.selecao.fotosExtras == 4
        antes.selecao.valorExtras == 40.0d
        antes.resumoExtras.valorCoberto == 30.0d
        antes.resumoExtras.valorPendente == 10.0d
        depois.galeria.fotosLiberadas == 4
        marcarPassou(r)
    }

    def "S11 — reabertura de tres para cinco extras, delta vinte"() {
        given:
        def r = resultado("S11", "Cliente S11", "3→5 extras", 'delta R$20')
        def p = prepararGaleria("Cliente S11", 5, 5)
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds[0..2])
        isentarExtras(p.galeriaId)
        mockMvc.perform(post("/api/v1/galerias/${p.galeriaId}/selecao/reabrir"))
                .andExpect(status().isOk())

        when:
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        def antes = detalhe(p.galeriaId)
        isentarExtras(p.galeriaId)
        def depois = detalhe(p.galeriaId)

        r.obtido = "extras=${antes.selecao.fotosExtras}; total=${antes.selecao.valorExtras}; " +
                "coberto=${antes.resumoExtras.valorCoberto}; pendente=${antes.resumoExtras.valorPendente}"
        r.detalhe = "liberadas depois=${depois.galeria.fotosLiberadas}"

        then:
        antes.selecao.fotosExtras == 5
        antes.selecao.valorExtras == 50.0d
        antes.resumoExtras.valorCoberto == 30.0d
        antes.resumoExtras.valorPendente == 20.0d
        depois.galeria.fotosLiberadas == 5
        marcarPassou(r)
    }

    def "S16/S17 — filtros, fila de edicao e lote"() {
        given:
        def r = resultado("S16/S17", "Cliente S16", "Filtros, fila e lote de edição",
                "selecionadas=8; prontas=3; liberadas=3; fila=5; após edição fila=4")
        def p = prepararGaleria("Cliente S16", 5, 3)
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        isentarExtras(p.galeriaId)
        def antes = detalhe(p.galeriaId)
        enviarVersaoEditada(p.galeriaId, p.edicaoIds[0], Color.BLUE, 0)
        mockMvc.perform(post("/api/v1/galerias/${p.galeriaId}/entrega/liberar-prontas"))
                .andExpect(status().isOk())

        when:
        def depois = detalhe(p.galeriaId)
        def filaAntes = antes.galeria.fotosSelecionadas - antes.galeria.fotosProntas
        def filaDepois = depois.galeria.fotosSelecionadas - depois.galeria.fotosProntas

        r.obtido = "selecionadas=${antes.galeria.fotosSelecionadas}; " +
                "prontas=${antes.galeria.fotosProntas}→${depois.galeria.fotosProntas}; " +
                "liberadas=${antes.galeria.fotosLiberadas}→${depois.galeria.fotosLiberadas}; " +
                "fila=${filaAntes}→${filaDepois}"

        then:
        antes.galeria.fotosSelecionadas == 8
        antes.galeria.fotosProntas == 3
        antes.galeria.fotosLiberadas == 3
        filaAntes == 5
        depois.galeria.fotosProntas == 4
        depois.galeria.fotosLiberadas == 4
        filaDepois == 4
        marcarPassou(r)
    }

    def "S13 — pacote sem faixa, tres extras custam quarenta e cinco"() {
        given:
        def r = resultado("S13", "Cliente S13", "Sem faixa / 3 extras", 'R$45')
        def p = prepararGaleria("Cliente S13", 5, 3, List.of())

        when:
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        isentarExtras(p.galeriaId)
        def d = detalhe(p.galeriaId)

        r.obtido = "extras=${d.selecao.fotosExtras}; valor=${d.selecao.valorExtras}; " +
                "unitario=${d.extras.detalhamento[0]?.valorUnitario}"
        r.detalhe = "faixas=${d.galeria.faixasFotoExtra}"

        then:
        d.selecao.fotosExtras == 3
        d.selecao.valorExtras == 45.0d
        d.extras.detalhamento*.valorUnitario == [15.0d]
        marcarPassou(r)
    }

    def "S14 — troca de finalidade antes da selecao"() {
        given:
        def r = resultado("S14", "Cliente S14", "EDICAO→PRONTA antes da seleção",
                "foto entra em prontas e baixa")
        def p = prepararGaleria("Cliente S14", 3, 2)
        def alterada = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${p.galeriaId}/midias/${p.edicaoIds[0]}/finalidade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([finalidade: "pronta"])))
                .andExpect(status().isOk())
                .andReturn())

        when:
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        def d = detalhe(p.galeriaId)
        def pub = publica(p.linkToken)

        r.obtido = "finalidade=${alterada.finalidade}; prontas=${d.galeria.fotosProntas}; " +
                "liberadas=${d.galeria.fotosLiberadas}"
        r.detalhe = "foto alterada disponível=" +
                pub.midias.find { it.id == p.edicaoIds[0] }.disponivelParaDownload

        then:
        alterada.finalidade == "pronta"
        d.galeria.fotosProntas == 3
        d.galeria.fotosLiberadas == 3
        pub.midias.find { it.id == p.edicaoIds[0] }.disponivelParaDownload
        marcarPassou(r)
    }

    def "S15 — troca de finalidade depois da selecao e bloqueada"() {
        given:
        def r = resultado("S15", "Cliente S15", "Troca depois da seleção", "bloqueio")
        def p = prepararGaleria("Cliente S15", 3, 2)
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)

        when:
        def tentativa = mockMvc.perform(
                post("/api/v1/galerias/${p.galeriaId}/midias/${p.edicaoIds[0]}/finalidade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([finalidade: "pronta"])))
                .andReturn()
        def corpo = JsonCorpo.mapa(tentativa)

        r.obtido = "status=${tentativa.response.status}; codigo=${corpo.codigo}"

        then:
        tentativa.response.status == 422
        corpo.codigo == "FINALIDADE_BLOQUEADA"
        marcarPassou(r)
    }

    def "S18/S19 — entrega final, ZIP e marca d'água"() {
        given:
        def r = resultado("S18/S19", "Cliente S18", "Entrega, ZIP e marca d'água",
                "entrega concluída, ZIP 8 arquivos, prévia marcada e download limpo")
        def p = prepararGaleria("Cliente S18", 5, 3)
        selecionar(p.linkToken, p.edicaoIds + p.prontaIds)
        isentarExtras(p.galeriaId)
        p.edicaoIds.eachWithIndex { id, index ->
            enviarVersaoEditada(p.galeriaId, id, Color.RED, index)
        }
        mockMvc.perform(post("/api/v1/galerias/${p.galeriaId}/entrega/liberar-prontas"))
                .andExpect(status().isOk())

        when:
        def entrega = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${p.galeriaId}/entrega/publicar"))
                .andExpect(status().isOk())
                .andReturn())
        def zip = mockMvc.perform(
                get("/api/v1/public/galerias/${entrega.token}/download-zip")
                        .param("versao", "auto"))
                .andExpect(status().isOk())
                .andReturn()
        def pub = publica(entrega.token)
        def download = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/public/galerias/${entrega.token}/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([midiaId: p.prontaIds[0], versao: "auto"])))
                .andExpect(status().isOk())
                .andReturn())
        def d = detalhe(p.galeriaId)

        r.obtido = "status=${d.galeria.status}; zip=${contarEntradasZip(zip.response.contentAsByteArray)}; " +
                "marcadas=${pub.midias.every { it.previaMarcada }}; download=${download.url != null}"
        r.detalhe = "originalUrl/editadaUrl nulos=" +
                pub.midias.every { it.originalUrl == null && it.editadaUrl == null }

        then:
        d.galeria.status == "entregue"
        contarEntradasZip(zip.response.contentAsByteArray) == 8
        pub.midias.every { it.previaMarcada }
        pub.midias.every { it.originalUrl == null && it.editadaUrl == null }
        download.url != null
        marcarPassou(r)
    }

    def "S20 — snapshot do pacote promocional"() {
        given:
        def r = resultado("S20", "Cliente S20", "Snapshot do pacote", 'galeria mantém R$15 / faixa R$10')
        def p = prepararGaleria("Cliente S20", 3, 2)
        def antes = detalhe(p.galeriaId)
        def pacote = pacoteRepository.findById(p.pacoteId).orElseThrow()
        pacote.atualizar(pacote.nome, pacote.duracaoMinutos, pacote.valor, pacote.descricao,
                pacote.corHex, pacote.itensInclusos, pacote.ativo, FOTOS_INCLUSAS, 0,
                new BigDecimal("20.00"),
                [FaixaFotoExtra.de(3, new BigDecimal("12.00"))], null)
        pacoteRepository.saveAndFlush(pacote)

        when:
        def depois = detalhe(p.galeriaId)

        r.obtido = "antes=${antes.galeria.valorFotoExtra}/${antes.galeria.faixasFotoExtra[0].valorUnitario}; " +
                "depois=${depois.galeria.valorFotoExtra}/${depois.galeria.faixasFotoExtra[0].valorUnitario}"

        then:
        antes.galeria.valorFotoExtra == 15.0d
        antes.galeria.faixasFotoExtra[0].valorUnitario == 10.0d
        depois.galeria.valorFotoExtra == 15.0d
        depois.galeria.faixasFotoExtra[0].valorUnitario == 10.0d
        marcarPassou(r)
    }

    // ---------------------------------------------------------------------
    // Apoio
    // ---------------------------------------------------------------------

    private boolean marcarPassou(Map<String, String> r) {
        r.status = "PASSOU"
        return true
    }

    private Map<String, String> resultado(String id, String cliente, String cenario, String esperado) {
        def r = [id: id, cliente: cliente, cenario: cenario, esperado: esperado,
                 obtido: "—", status: "FALHOU", detalhe: ""] as Map<String, String>
        resultados << r
        return r
    }

    private Map prepararGaleria(String clienteNome, int edicao, int pronta,
                                List<FaixaFotoExtra> faixas = [FaixaFotoExtra.de(3, new BigDecimal("10.00"))]) {
        def cliente = Cenario.cliente(clienteRepository, clienteNome)
        def promocao = PromocaoPacote.de(true, new BigDecimal("399.00"),
                LocalDate.now().plusDays(30), "Promocional E2E")
        def pacote = Cenario.pacote(pacoteRepository, "Promocional ${clienteNome}",
                new BigDecimal("500.00"), FOTOS_INCLUSAS, 0, BASE, promocao, faixas)
        def agendamento = Cenario.agendamento(agendamentoRepository, cliente, pacote,
                Cenario.amanhaAsDez(), StatusAgendamento.REALIZADO)
        agendamento.atualizarPagamentoEntrada(
                new BigDecimal("150.00"), new BigDecimal("30.00"),
                PagamentoEntrada.confirmada(new BigDecimal("150.00"), new BigDecimal("30.00"),
                        Instant.now(), "Teste"))
        agendamento.atualizarPagamentoFinal(
                PagamentoSaldo.recebido(new BigDecimal("350.00"), FormaPagamentoSaldo.PIX,
                        Instant.now(), "Teste"))
        agendamentoRepository.saveAndFlush(agendamento)

        def galeria = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/galerias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([agendamentoId: agendamento.id])))
                .andExpect(status().isOk())
                .andReturn())

        def edicaoIds = (1..edicao).collect { index ->
            enviarFoto(galeria.id, "edicao-${index}.jpg",
                    imagemJpeg(120 + index, 80, new Color(20 * index % 255, 80, 120)),
                    "edicao")
        }
        def prontaIds = (1..pronta).collect { index ->
            enviarFoto(galeria.id, "pronta-${index}.jpg",
                    imagemJpeg(140 + index, 90, new Color(120, 20 * index % 255, 80)),
                    "pronta")
        }
        def link = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeria.id}/selecao/publicar"))
                .andExpect(status().isOk())
                .andReturn())

        return [clienteId: cliente.id, pacoteId: pacote.id, galeriaId: galeria.id,
                edicaoIds: edicaoIds, prontaIds: prontaIds, linkToken: link.token]
    }

    private String enviarFoto(String galeriaId, String nome, byte[] imagem, String finalidade) {
        storage.simular(imagem.length, "image/jpeg", imagem)
        def sessao = JsonCorpo.mapa(mockMvc.perform(post("/api/v1/galerias/${galeriaId}/midias/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([
                        nomeArquivo   : nome,
                        mimeType      : "image/jpeg",
                        tamanho       : imagem.length,
                        checksumSha256: sha256(imagem),
                        tipo          : "foto",
                        finalidade    : finalidade
                ])))
                .andExpect(status().isOk())
                .andReturn())
        return JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: [], tipo: "foto", finalidade: finalidade])))
                .andExpect(status().isOk())
                .andReturn()).id
    }

    private void enviarVersaoEditada(String galeriaId, String midiaId, Color cor, int seed) {
        def imagem = imagemJpeg(150 + seed, 95, cor)
        storage.simular(imagem.length, "image/jpeg", imagem)
        def sessao = JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/${midiaId}/editada/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([
                                nomeArquivo          : "editada-${seed}.jpg",
                                mimeType             : "image/jpeg",
                                tamanho              : imagem.length,
                                checksumSha256       : sha256(imagem),
                                liberarAutomaticamente: true
                        ])))
                .andExpect(status().isOk())
                .andReturn())
        mockMvc.perform(
                post("/api/v1/galerias/${galeriaId}/midias/${midiaId}/editada/upload/${sessao.arquivoId}/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([partes: []])))
                .andExpect(status().isOk())
    }

    private Map selecionar(String token, List<String> fotoIds) {
        return JsonCorpo.mapa(mockMvc.perform(
                post("/api/v1/public/galerias/${token}/selecao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonCorpo.json([fotoIds: fotoIds, videoIds: []])))
                .andExpect(status().isOk())
                .andReturn())
    }

    private void isentarExtras(String galeriaId) {
        mockMvc.perform(post("/api/v1/galerias/${galeriaId}/extras/isentar"))
                .andExpect(status().isOk())
    }

    private Map detalhe(String galeriaId) {
        return JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/galerias/${galeriaId}"))
                .andExpect(status().isOk())
                .andReturn())
    }

    private Map publica(String token) {
        return JsonCorpo.mapa(mockMvc.perform(
                get("/api/v1/public/galerias/${token}"))
                .andExpect(status().isOk())
                .andReturn())
    }

    private static int contarEntradasZip(byte[] conteudo) {
        int total = 0
        new ZipInputStream(new ByteArrayInputStream(conteudo)).withCloseable { zip ->
            while (zip.nextEntry != null) {
                total++
            }
        }
        return total
    }

    private static byte[] imagemJpeg(int largura, int altura, Color cor) {
        def imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB)
        def graphics = imagem.createGraphics()
        graphics.setColor(cor)
        graphics.fillRect(0, 0, imagem.width, imagem.height)
        graphics.dispose()
        def saida = new ByteArrayOutputStream()
        ImageIO.write(imagem, "jpg", saida)
        return saida.toByteArray()
    }

    private static String sha256(byte[] conteudo) {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(conteudo))
    }
}
