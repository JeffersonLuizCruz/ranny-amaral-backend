package com.ranny.fotografia.support

import com.ranny.fotografia.agenda.model.Agendamento
import com.ranny.fotografia.agenda.model.CondicoesPacote
import com.ranny.fotografia.agenda.model.ContatoReserva
import com.ranny.fotografia.agenda.model.StatusAgendamento
import com.ranny.fotografia.agenda.repository.AgendamentoRepository
import com.ranny.fotografia.cliente.model.Cliente
import com.ranny.fotografia.cliente.model.ClienteOrigem
import com.ranny.fotografia.cliente.repository.ClienteRepository
import com.ranny.fotografia.contrato.model.EstudioConfig
import com.ranny.fotografia.contrato.model.ModeloContrato
import com.ranny.fotografia.contrato.model.documento.ContratoClausula
import com.ranny.fotografia.contrato.repository.EstudioConfigRepository
import com.ranny.fotografia.contrato.repository.ModeloContratoRepository
import com.ranny.fotografia.pacote.model.Pacote
import com.ranny.fotografia.pacote.model.PromocaoPacote
import com.ranny.fotografia.pacote.repository.PacoteRepository

import java.time.OffsetDateTime

/**
 * Fabrica de cenarios de teste.
 *
 * <p>PATTERN: Object Mother / Test Data Builder.
 *
 * <p>Recebe os repositorios por parametro (composicao), em vez de herdar uma
 * classe base de teste. Cada spec escolhe apenas os dados que precisa e
 * sobrescreve os defaults.
 */
final class Cenario {

    private Cenario() {
    }

    static Cliente cliente(ClienteRepository repository,
                           String nome = "Marina Teste",
                           String whatsapp = "(11) 99999-0000",
                           ClienteOrigem origem = ClienteOrigem.INSTAGRAM) {
        return repository.save(Cliente.criar(
                nome, whatsapp, "${nome.toLowerCase().replaceAll('\\s+', '.')}@teste.com",
                null, null, "São Paulo", origem, null))
    }

    static Pacote pacote(PacoteRepository repository,
                         String nome = "Pacote Teste",
                         BigDecimal valor = new BigDecimal("1000.00"),
                         int fotos = 20,
                         int videos = 1,
                         BigDecimal fotoExtra = new BigDecimal("50.00"),
                         PromocaoPacote promocao = null) {
        return repository.save(Pacote.criar(
                nome, 90, valor, "Pacote de teste", "#7C3AED",
                ["${fotos} fotos tratadas".toString(), "${videos} vídeos inclusos".toString()],
                true, fotos, videos, fotoExtra, promocao))
    }

    static Agendamento agendamento(AgendamentoRepository repository,
                                   Cliente cliente,
                                   Pacote pacote,
                                   OffsetDateTime inicio = amanhaAsDez(),
                                   StatusAgendamento status = StatusAgendamento.RESERVADO) {
        ContatoReserva contato = cliente == null
                ? ContatoReserva.de("Juliana Prado", "(11) 91234-5678")
                : ContatoReserva.de(cliente.nome, cliente.whatsapp)
        CondicoesPacote condicoes = CondicoesPacote.de(
                pacote.quantidadeFotosInclusas, pacote.quantidadeVideosInclusos,
                pacote.valorFotoExtra, pacote.valor, null)
        return repository.save(Agendamento.criar(
                cliente?.id, contato, pacote.id, inicio, inicio.plusMinutes(pacote.duracaoMinutos),
                "Estúdio Teste", status, null,
                pacote.valor, pacote.valor.multiply(new BigDecimal("0.30")).setScale(2),
                new BigDecimal("30.00"), condicoes))
    }

    static ModeloContrato modelo(ModeloContratoRepository repository) {
        return repository.save(ModeloContrato.criar(
                "Modelo de Teste",
                "Modelo usado nos testes",
                [
                        new ContratoClausula("cl-objeto", "1. Objeto",
                                "Serviços para {{cliente.nome}} no valor de {{contrato.valorTotal}}."),
                        new ContratoClausula("cl-pagamento", "2. Pagamento",
                                "Entrada de {{contrato.valorEntrada}} e saldo de {{contrato.saldoRestante}}.")
                ],
                true))
    }

    static EstudioConfig estudio(EstudioConfigRepository repository) {
        return repository.findById(EstudioConfig.ID)
                .orElseGet({ repository.save(EstudioConfig.padrao()) })
    }

    static OffsetDateTime amanhaAsDez() {
        return OffsetDateTime.now()
                .plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0)
    }
}
