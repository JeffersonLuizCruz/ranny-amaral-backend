package com.ranny.fotografia.armazenamento.service;

import com.ranny.fotografia.armazenamento.ArmazenamentoProperties;
import com.ranny.fotografia.armazenamento.api.ArmazenamentoDtos;
import com.ranny.fotografia.armazenamento.model.AmostraArmazenamento;
import com.ranny.fotografia.armazenamento.repository.AmostraArmazenamentoRepository;
import com.ranny.fotografia.arquivo.ArquivoConsulta;
import com.ranny.fotografia.galeria.GaleriaConsulta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Consultas da tela de armazenamento e reconciliacao banco x bucket.
 *
 * <p>Nunca apaga nada: a reconciliacao apenas informa divergencias. O expurgo e
 * uma acao explicita do ADMIN na galeria.
 */
@Service
public class ArmazenamentoService {

    private static final int AMOSTRA_RECONCILIACAO = 50;

    private final AmostraArmazenamentoRepository repository;
    private final ColetorArmazenamento coletor;
    private final ArquivoConsulta arquivoConsulta;
    private final GaleriaConsulta galeriaConsulta;
    private final ArmazenamentoProperties properties;

    public ArmazenamentoService(AmostraArmazenamentoRepository repository,
                                ColetorArmazenamento coletor,
                                ArquivoConsulta arquivoConsulta,
                                GaleriaConsulta galeriaConsulta,
                                ArmazenamentoProperties properties) {
        this.repository = repository;
        this.coletor = coletor;
        this.arquivoConsulta = arquivoConsulta;
        this.galeriaConsulta = galeriaConsulta;
        this.properties = properties;
    }

    @Transactional
    public ArmazenamentoDtos.ResumoResponse resumo() {
        AmostraArmazenamento amostra = repository.findFirstByOrderByColetadoEmDesc()
                .orElseGet(coletor::coletar);
        List<GaleriaConsulta.ResumoArmazenamento> galerias =
                galeriaConsulta.galeriasParaArmazenamento();
        long elegiveis = galerias.stream().filter(GaleriaConsulta.ResumoArmazenamento::elegivel).count();
        return resumo(amostra, elegiveis);
    }

    @Transactional
    public List<ArmazenamentoDtos.GaleriaArmazenamento> galerias() {
        List<GaleriaConsulta.ResumoArmazenamento> resumos =
                galeriaConsulta.galeriasParaArmazenamento();
        List<String> ids = resumos.stream()
                .map(GaleriaConsulta.ResumoArmazenamento::galeriaId)
                .toList();
        var totais = arquivoConsulta.totaisPorReferencia(ids);
        List<ArmazenamentoDtos.GaleriaArmazenamento> itens = new ArrayList<>(resumos.size());
        for (GaleriaConsulta.ResumoArmazenamento resumo : resumos) {
            ArquivoConsulta.TotaisReferencia total = totais.get(resumo.galeriaId());
            itens.add(new ArmazenamentoDtos.GaleriaArmazenamento(
                    resumo.galeriaId(),
                    resumo.clienteNome(),
                    resumo.status(),
                    total == null ? 0 : total.totalBytes(),
                    total == null ? 0 : total.quantidade(),
                    resumo.entregueEm(),
                    resumo.zipBaixadoEm(),
                    resumo.zipConcluidoEm(),
                    resumo.expurgoStatus(),
                    resumo.expurgoEfetivadoEm(),
                    resumo.bytesExpurgados(),
                    resumo.elegivel(),
                    resumo.motivoBloqueio()));
        }
        itens.sort((a, b) -> Long.compare(b.totalBytes(), a.totalBytes()));
        return itens;
    }

    @Transactional
    public ArmazenamentoDtos.ReconciliacaoResponse reconciliar() {
        List<ArquivoConsulta.ObjetoStorage> objetos = arquivoConsulta.listarObjetosComTamanho("");
        Set<String> catalogo = new HashSet<>(arquivoConsulta.todasAsObjectKeys());
        Set<String> noStorage = new HashSet<>();
        List<String> amostraSomenteStorage = new ArrayList<>();
        long somenteStorage = 0;
        long bytesSomenteStorage = 0;
        for (ArquivoConsulta.ObjetoStorage objeto : objetos) {
            noStorage.add(objeto.objectKey());
            if (!catalogo.contains(objeto.objectKey())) {
                somenteStorage++;
                bytesSomenteStorage += objeto.tamanhoBytes();
                if (amostraSomenteStorage.size() < AMOSTRA_RECONCILIACAO) {
                    amostraSomenteStorage.add(objeto.objectKey());
                }
            }
        }
        List<String> amostraSomenteCatalogo = new ArrayList<>();
        long somenteCatalogo = 0;
        for (String chave : catalogo) {
            if (!noStorage.contains(chave)) {
                somenteCatalogo++;
                if (amostraSomenteCatalogo.size() < AMOSTRA_RECONCILIACAO) {
                    amostraSomenteCatalogo.add(chave);
                }
            }
        }
        return new ArmazenamentoDtos.ReconciliacaoResponse(
                objetos.size(),
                catalogo.size(),
                somenteStorage,
                somenteCatalogo,
                bytesSomenteStorage,
                amostraSomenteStorage,
                amostraSomenteCatalogo);
    }

    private ArmazenamentoDtos.ResumoResponse resumo(AmostraArmazenamento amostra, long elegiveis) {
        Long total = amostra.getTotalBytes();
        Long livre = amostra.getLivreBytes();
        Long usado = amostra.getUsadoBytes();
        Integer percentual = total == null || total <= 0 || usado == null
                ? null : (int) Math.round(usado * 100.0 / total);
        String alerta = alerta(livre, total, percentual);
        Long disponivel = livre == null
                ? null : Math.max(0, livre - properties.getReservaBytes());
        return new ArmazenamentoDtos.ResumoResponse(
                amostra.getColetadoEm(),
                amostra.getOrigemColeta().name(),
                total,
                livre,
                usado,
                amostra.getCrmBytes(),
                amostra.getObjetosBytes(),
                amostra.getPostgresBytes(),
                amostra.getOutrosBytes(),
                amostra.getReservaBytes(),
                disponivel,
                percentual,
                alerta,
                diasRestantes(amostra),
                elegiveis,
                new ArmazenamentoDtos.TotaisResponse(
                        amostra.getOriginaisBytes(),
                        amostra.getEditadasBytes(),
                        amostra.getDerivadosBytes(),
                        amostra.getVideosBytes(),
                        amostra.getComprovantesBytes(),
                        amostra.getObjetosBytes(),
                        amostra.getObjetosTotal()));
    }

    private String alerta(Long livre, Long total, Integer percentual) {
        if (livre == null || total == null) {
            return "INDISPONIVEL";
        }
        long reserva = properties.getReservaBytes();
        if (livre <= reserva) {
            return "CRITICO";
        }
        if (percentual != null && percentual >= properties.getPercentualAlerta()) {
            return "ATENCAO";
        }
        return "OK";
    }

    private Long diasRestantes(AmostraArmazenamento amostra) {
        if (amostra.getLivreBytes() == null) {
            return null;
        }
        List<AmostraArmazenamento> historico = repository.findTop30ByOrderByColetadoEmDesc();
        Optional<AmostraArmazenamento> antiga = historico.stream()
                .filter(item -> item.getUsadoBytes() != null)
                .filter(item -> item.getColetadoEm().isBefore(
                        Instant.now().minus(properties.getJanelaEstimativa())))
                .findFirst();
        if (antiga.isEmpty() || amostra.getUsadoBytes() == null) {
            return null;
        }
        long crescimento = amostra.getUsadoBytes() - antiga.get().getUsadoBytes();
        long dias = Math.max(1, Duration.between(
                antiga.get().getColetadoEm(), amostra.getColetadoEm()).toDays());
        if (crescimento <= 0) {
            return null;
        }
        double porDia = (double) crescimento / dias;
        long disponivel = amostra.getLivreBytes() - properties.getReservaBytes();
        return disponivel <= 0 ? 0 : Math.round(disponivel / porDia);
    }
}
