-- V2 - Busca e paginacao de contratos.
--
-- O snapshot continua imutavel em TEXT, mas os nomes de cliente e pacote
-- passam a existir em colunas relacionais para busca paginada sem varrer JSON.
-- A V1 permanece imutavel; esta migracao e incremental.

alter table contratos add column cliente_nome varchar(120);
alter table contratos add column pacote_nome varchar(120);

update contratos
set cliente_nome = snapshot::jsonb #>> '{cliente,nome}',
    pacote_nome  = snapshot::jsonb #>> '{pacote,nome}'
where snapshot is not null;

create index idx_contratos_cliente_nome on contratos (cliente_nome);
create index idx_contratos_pacote_nome on contratos (pacote_nome);
create index idx_contratos_status_data on contratos (status_aceite, data_emissao desc);

create index idx_agendamentos_status_inicio on agendamentos (status, data_hora_inicio);
