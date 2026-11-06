package com.ranny.fotografia.cliente.seed;

import com.ranny.fotografia.cliente.model.Cliente;
import com.ranny.fotografia.cliente.model.ClienteOrigem;
import com.ranny.fotografia.cliente.repository.ClienteRepository;
import com.ranny.fotografia.shared.model.TipoDocumento;
import com.ranny.fotografia.shared.seed.DadosDemo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed de desenvolvimento do modulo de clientes.
 *
 * <p>PATTERN: Seeder idempotente (guarda de idempotencia) + Profiles.
 *
 * <p>Cada modulo popula seus proprios dados: isso evita que um seeder global
 * importe repositorios de todos os modulos e viole as fronteiras do Modulith.
 * O {@code @Order} garante a ordem de execucao quando ha referencias logicas.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(10)
public class ClienteDemoSeeder implements ApplicationRunner {

    private final ClienteRepository clienteRepository;

    public ClienteDemoSeeder(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (clienteRepository.count() > 0) {
            return;
        }
        clienteRepository.save(Cliente.restaurar(
                DadosDemo.CLIENTE_MARINA,
                "Marina Alves",
                "(11) 99876-5432",
                "marina.alves@email.com",
                "529.982.247-25",
                TipoDocumento.CPF,
                "São Paulo",
                ClienteOrigem.INSTAGRAM,
                "Prefere tons neutros e luz natural."));
        clienteRepository.save(Cliente.restaurar(
                DadosDemo.CLIENTE_RAFAEL,
                "Rafael Costa",
                "(11) 98765-4321",
                "rafael.costa@email.com",
                "12.345.678-9",
                TipoDocumento.RG,
                "São Paulo",
                ClienteOrigem.INDICACAO,
                "Contato corporativo. Aprovação passa pelo RH."));
    }
}
