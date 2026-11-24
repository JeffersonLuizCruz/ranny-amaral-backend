-- V22 - Cancelamento de ensaio com justificativa.
--
-- As colunas sao anulaveis de proposito: agendamentos ja cancelados antes
-- desta migracao continuam validos e sem justificativa registrada. Nao ha
-- backfill nem alteracao de status, portanto nenhum dado existente e perdido.

alter table agendamentos
    add column cancelamento_motivo varchar(1000),
    add column cancelado_em timestamptz,
    add column cancelado_por varchar(120);
