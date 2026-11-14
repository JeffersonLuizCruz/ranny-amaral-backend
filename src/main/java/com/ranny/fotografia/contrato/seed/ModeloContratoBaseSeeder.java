package com.ranny.fotografia.contrato.seed;

import com.ranny.fotografia.contrato.model.ModeloContrato;
import com.ranny.fotografia.contrato.repository.ModeloContratoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disponibiliza o modelo de contrato baseado no CRM em instalacoes existentes.
 *
 * <p>O modelo entra inativo quando ja existe algum modelo cadastrado, para que o
 * ADMIN revise as clausulas antes de ativa-lo. Em uma base sem modelos, ele e
 * criado ativo para nao deixar a criacao de contratos sem template.
 */
@Component
@Profile("!test")
@Order(41)
public class ModeloContratoBaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModeloContratoBaseSeeder.class);

    private static final String ID = "modelo-contrato-base-crm";
    private static final String NOME = "Contrato de Fotografia (base CRM)";
    private static final String DESCRICAO =
            "Modelo baseado no contrato do CRM, com entrega, extras, cancelamento e uso de imagem. "
                    + "Revise as cláusulas antes de ativar.";

    private final ModeloContratoRepository modeloRepository;

    public ModeloContratoBaseSeeder(ModeloContratoRepository modeloRepository) {
        this.modeloRepository = modeloRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (modeloRepository.existsById(ID)) {
            return;
        }

        boolean ativo = modeloRepository.count() == 0;
        ModeloContrato modelo = ModeloContrato.restaurar(
                ID, NOME, DESCRICAO, ContratoBaseCrm.clausulas(), ativo, 1);
        modeloRepository.save(modelo);
        log.info("Modelo de contrato base CRM criado ({})", ativo ? "ativo" : "inativo");
    }
}
