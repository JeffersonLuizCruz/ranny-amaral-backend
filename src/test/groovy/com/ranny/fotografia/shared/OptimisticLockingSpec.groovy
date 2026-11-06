package com.ranny.fotografia.shared

import com.ranny.fotografia.cliente.model.Cliente
import com.ranny.fotografia.cliente.model.ClienteOrigem
import com.ranny.fotografia.cliente.repository.ClienteRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Specification

/**
 * Verifica que o {@code @Version} das entidades impede lost updates.
 *
 * <p>Não usa {@code @Transactional} no spec: cada operação abre e commita uma
 * transação real, permitindo simular duas leituras concorrentes.
 */
@SpringBootTest
@ActiveProfiles("test")
class OptimisticLockingSpec extends Specification {

    @Autowired
    ClienteRepository clienteRepository

    @Autowired
    PlatformTransactionManager transactionManager

    TransactionTemplate tx

    def setup() {
        tx = new TransactionTemplate(transactionManager)
    }

    def cleanup() {
        tx.execute {
            clienteRepository.deleteAll()
            null
        }
    }

    def "detecta atualizacao concorrente de um cliente"() {
        given:
        def id = tx.execute {
            clienteRepository.save(cliente("Marina Alves")).id
        }
        def desatualizado = tx.execute {
            clienteRepository.findById(id).get()
        }

        tx.execute {
            def atual = clienteRepository.findById(id).get()
            atual.atualizar("Marina Atualizada", "(11) 99999-0001", null, null,
                    null, null, ClienteOrigem.INSTAGRAM, null)
            clienteRepository.saveAndFlush(atual)
            null
        }

        when:
        tx.execute {
            desatualizado.atualizar("Marina Concorrente", "(11) 99999-0001", null, null,
                    null, null, ClienteOrigem.INSTAGRAM, null)
            clienteRepository.saveAndFlush(desatualizado)
            null
        }

        then:
        thrown(OptimisticLockingFailureException)
    }

    private static Cliente cliente(String nome) {
        return Cliente.criar(nome, "(11) 99999-0001", null, null, null, null,
                ClienteOrigem.INSTAGRAM, null)
    }
}
