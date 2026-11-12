package com.ranny.fotografia.armazenamento.service;

import com.ranny.fotografia.armazenamento.ArmazenamentoProperties;
import com.ranny.fotografia.armazenamento.model.AmostraArmazenamento;
import com.ranny.fotografia.armazenamento.model.OrigemColeta;
import com.ranny.fotografia.armazenamento.repository.AmostraArmazenamentoRepository;
import com.ranny.fotografia.arquivo.ArquivoConsulta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coleta periodica do consumo de disco.
 *
 * <p>Mede o filesystem, o tamanho do banco e o catalogo de arquivos. O consumo de
 * infraestrutura (Coolify, Docker, logs, WAL) e derivado: total - livre - CRM.
 */
@Service
public class ColetorArmazenamento {

    private static final Logger log = LoggerFactory.getLogger(ColetorArmazenamento.class);

    private final AmostraArmazenamentoRepository repository;
    private final MedidorVolume medidorVolume;
    private final ArquivoConsulta arquivoConsulta;
    private final ArmazenamentoProperties properties;

    @PersistenceContext
    private EntityManager entityManager;

    public ColetorArmazenamento(AmostraArmazenamentoRepository repository,
                                MedidorVolume medidorVolume,
                                ArquivoConsulta arquivoConsulta,
                                ArmazenamentoProperties properties) {
        this.repository = repository;
        this.medidorVolume = medidorVolume;
        this.arquivoConsulta = arquivoConsulta;
        this.properties = properties;
    }

    @Scheduled(cron = "${app.armazenamento.coleta-cron:0 15 3 * * *}")
    public void coletarAgendado() {
        if (!properties.isColetaHabilitada()) {
            return;
        }
        try {
            coletar();
        } catch (RuntimeException ex) {
            log.warn("Falha na coleta agendada de armazenamento: {}", ex.getMessage());
        }
    }

    @Transactional
    public AmostraArmazenamento coletar() {
        MedidorVolume.Medicao medicao = medidorVolume.medir();
        ArquivoConsulta.TotaisPorFinalidade totais = arquivoConsulta.totaisPorFinalidade();
        Long postgres = medirPostgres();
        AmostraArmazenamento amostra = AmostraArmazenamento.registrar(
                medicao.origem() == null ? OrigemColeta.INDISPONIVEL : medicao.origem(),
                medicao.totalBytes(),
                medicao.livreBytes(),
                totais.totalBytes(),
                totais.quantidade(),
                postgres,
                totais.originaisBytes(),
                totais.editadasBytes(),
                totais.derivadosBytes(),
                totais.videosBytes(),
                totais.comprovantesBytes(),
                properties.getReservaBytes());
        return repository.save(amostra);
    }

    private Long medirPostgres() {
        try {
            Object resultado = entityManager
                    .createNativeQuery("select pg_database_size(current_database())")
                    .getSingleResult();
            return resultado == null ? null : ((Number) resultado).longValue();
        } catch (RuntimeException ex) {
            // H2 e ambientes sem permissao: o tamanho do banco fica indisponivel.
            log.debug("Tamanho do banco indisponivel: {}", ex.getMessage());
            return null;
        }
    }
}
