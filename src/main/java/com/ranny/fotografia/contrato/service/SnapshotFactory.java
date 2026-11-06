package com.ranny.fotografia.contrato.service;

import com.ranny.fotografia.agenda.AgendamentoConsulta;
import com.ranny.fotografia.agenda.AgendamentoConsulta.ResumoAgendamento;
import com.ranny.fotografia.cliente.ClienteConsulta;
import com.ranny.fotografia.cliente.ClienteResumo;
import com.ranny.fotografia.contrato.model.EstudioConfig;
import com.ranny.fotografia.contrato.model.documento.ContratoSnapshot;
import com.ranny.fotografia.pacote.PacoteConsulta;
import com.ranny.fotografia.pacote.PacoteResumo;
import com.ranny.fotografia.shared.exception.RegraDeNegocioException;
import com.ranny.fotografia.shared.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * PATTERN: Builder / Assembler + Anti-Corruption Layer.
 *
 * <p>Monta o snapshot do contrato a partir das APIs publicas de cliente, pacote
 * e agenda, traduzindo os modelos de cada modulo para o documento imutavel do
 * contrato. E a camada anti-corrupcao que impede o contrato de depender das
 * entidades internas dos outros modulos.
 *
 * <p>Beneficios: se agenda ou pacote mudarem seu modelo, apenas o assembler e
 * afetado; o snapshot continua estavel para o juridico.
 */
@Component
public class SnapshotFactory {

    private final ClienteConsulta clienteConsulta;
    private final PacoteConsulta pacoteConsulta;
    private final AgendamentoConsulta agendamentoConsulta;
    private final EstudioService estudioService;

    public SnapshotFactory(ClienteConsulta clienteConsulta,
                           PacoteConsulta pacoteConsulta,
                           AgendamentoConsulta agendamentoConsulta,
                           EstudioService estudioService) {
        this.clienteConsulta = clienteConsulta;
        this.pacoteConsulta = pacoteConsulta;
        this.agendamentoConsulta = agendamentoConsulta;
        this.estudioService = estudioService;
    }

    /**
     * Usa o snapshot enviado pelo frontend quando ele existe (permite revisao
     * manual) ou monta um novo a partir dos dados atuais.
     */
    public ContratoSnapshot montar(ContratoSnapshot informado, String clienteId,
                                   String agendamentoId, String pacoteId, int modeloVersao) {
        if (informado != null) {
            return informado;
        }
        Instant agora = Instant.now();
        ResumoAgendamento agendamento = agendamentoId == null || agendamentoId.isBlank()
                ? null
                : agendamentoConsulta.buscar(agendamentoId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId));

        String clienteResolvido = clienteId != null && !clienteId.isBlank()
                ? clienteId
                : agendamento == null ? null : agendamento.clienteId();
        ClienteResumo cliente = clienteResolvido == null
                ? null
                : clienteConsulta.buscar(clienteResolvido)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteResolvido));

        String pacoteResolvido = pacoteId != null && !pacoteId.isBlank()
                ? pacoteId
                : agendamento == null ? null : agendamento.pacoteId();
        if (pacoteResolvido == null) {
            throw new RegraDeNegocioException("PACOTE_OBRIGATORIO",
                    "O contrato precisa de um pacote vinculado.");
        }
        PacoteResumo pacote = pacoteConsulta.buscar(pacoteResolvido)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pacote", pacoteResolvido));

        EstudioConfig estudio = estudioService.obter();
        return new ContratoSnapshot(
                montarCliente(cliente, agendamento),
                montarPacote(pacote, agendamento, agora),
                montarAgendamento(agendamento),
                montarEstudio(estudio),
                modeloVersao);
    }

    private static ContratoSnapshot.ClienteSnapshot montarCliente(ClienteResumo cliente,
                                                                  ResumoAgendamento agendamento) {
        if (cliente != null) {
            return new ContratoSnapshot.ClienteSnapshot(
                    cliente.nome(), cliente.whatsapp(), cliente.email(),
                    cliente.documento(), cliente.tipoDocumento(), cliente.cidade());
        }
        if (agendamento != null && agendamento.contato() != null) {
            return new ContratoSnapshot.ClienteSnapshot(
                    agendamento.contato().nome(), agendamento.contato().whatsapp(),
                    null, null, null, null);
        }
        throw new RegraDeNegocioException("CLIENTE_OBRIGATORIO",
                "O contrato precisa de um cliente cadastrado ou de um contato de reserva.");
    }

    private static ContratoSnapshot.PacoteSnapshot montarPacote(PacoteResumo pacote,
                                                                 ResumoAgendamento agendamento,
                                                                 Instant agora) {
        int fotos = pacote.quantidadeFotosInclusas();
        int videos = pacote.quantidadeVideosInclusos();
        var valorFotoExtra = pacote.valorFotoExtra();
        ContratoSnapshot.PromocaoSnapshot promocao = pacote.promocaoAplicada(agora)
                .map(p -> new ContratoSnapshot.PromocaoSnapshot(p.valorPromocional(), p.rotulo()))
                .orElse(null);

        if (agendamento != null && agendamento.condicoes() != null) {
            fotos = agendamento.condicoes().quantidadeFotosInclusas();
            videos = agendamento.condicoes().quantidadeVideosInclusos();
            valorFotoExtra = agendamento.condicoes().valorFotoExtra();
            promocao = agendamento.condicoes().promocaoValor() == null ? null
                    : new ContratoSnapshot.PromocaoSnapshot(
                            agendamento.condicoes().promocaoValor(),
                            agendamento.condicoes().promocaoRotulo());
        }

        var valorEfetivo = agendamento != null
                ? agendamento.valorAcordado()
                : pacote.valorEfetivo(agora);

        return new ContratoSnapshot.PacoteSnapshot(
                pacote.nome(),
                pacote.duracaoMinutos(),
                pacote.valor(),
                pacote.itensInclusos() == null ? List.of() : pacote.itensInclusos(),
                fotos,
                videos,
                valorFotoExtra,
                promocao,
                valorEfetivo);
    }

    private static ContratoSnapshot.AgendamentoSnapshot montarAgendamento(ResumoAgendamento agendamento) {
        if (agendamento == null) {
            return null;
        }
        return new ContratoSnapshot.AgendamentoSnapshot(
                agendamento.dataHoraInicio(),
                agendamento.dataHoraFim(),
                agendamento.local(),
                agendamento.valorAcordado());
    }

    private static ContratoSnapshot.EstudioSnapshot montarEstudio(EstudioConfig estudio) {
        return new ContratoSnapshot.EstudioSnapshot(
                estudio.getNome(),
                estudio.getRazaoSocial(),
                estudio.getCnpj(),
                estudio.getEndereco(),
                estudio.getCidade(),
                estudio.getEstado(),
                estudio.getEmail(),
                estudio.getTelefone(),
                estudio.getResponsavel(),
                estudio.getSite(),
                estudio.getForo(),
                estudio.getPercentualEntradaPadrao(),
                estudio.getChavePix(),
                estudio.getInstrucoesPagamento(),
                estudio.getPrazoPagamentoDias());
    }
}
