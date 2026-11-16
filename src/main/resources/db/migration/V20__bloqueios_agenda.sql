-- V20 - Bloqueios pontuais da agenda.
--
-- A equipe pode reservar um dia inteiro ou um intervalo para impedir novos
-- ensaios. O bloqueio e pontual (sem recorrencia) e o backend recusa tanto um
-- ensaio sobre o bloqueio quanto um bloqueio sobre ensaio ativo.

create table agenda_bloqueios (
    id varchar(64) primary key,
    lock_version bigint not null default 0,
    data_hora_inicio timestamptz not null,
    data_hora_fim timestamptz not null,
    dia_inteiro boolean not null default false,
    motivo varchar(255) not null,
    criado_em timestamptz not null,
    atualizado_em timestamptz not null,
    constraint ck_agenda_bloqueios_periodo check (data_hora_fim > data_hora_inicio)
);

create index idx_agenda_bloqueios_periodo
    on agenda_bloqueios (data_hora_inicio, data_hora_fim);
