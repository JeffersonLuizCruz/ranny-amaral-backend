-- Garante no banco a mesma regra aplicada pelo dominio:
-- saldo final confirmado exige valor recebido maior ou igual ao saldo previsto.
--
-- A regra impede que escritas diretas ou cargas legadas marquem o saldo como
-- quitado sem o valor correspondente. Entradas isentas/pendentes e saldos
-- pendentes/isentos/cancelados nao sao afetados.

alter table contratos
    add constraint ck_contratos_saldo_confirmado_integral
    check (
        saldo_status <> 'CONFIRMADO'
        or coalesce(saldo_valor, 0) >= saldo_restante
    );

alter table agendamentos
    add constraint ck_agendamentos_saldo_confirmado_integral
    check (
        saldo_status <> 'CONFIRMADO'
        or coalesce(saldo_valor, 0) >= saldo_restante
    );
