-- Armazenamento monitorado e expurgo por galeria.
--
-- O expurgo remove apenas os binarios do object storage. Metadados, historico,
-- financeiro e comprovantes de pagamento permanecem no banco.

alter table galerias
    add column zip_concluido_em timestamp with time zone,
    add column expurgo_status varchar(20) not null default 'NENHUM',
    add column expurgo_solicitado_em timestamp with time zone,
    add column expurgo_solicitado_por varchar(120),
    add column expurgo_motivo varchar(500),
    add column expurgo_efetivado_em timestamp with time zone,
    add column bytes_expurgados bigint not null default 0;

create table armazenamento_amostra (
    id varchar(64) primary key,
    coletado_em timestamp with time zone not null,
    origem_coleta varchar(30) not null,
    total_bytes bigint,
    livre_bytes bigint,
    usado_bytes bigint,
    objetos_bytes bigint not null default 0,
    objetos_total bigint not null default 0,
    postgres_bytes bigint,
    outros_bytes bigint,
    crm_bytes bigint,
    originais_bytes bigint not null default 0,
    editadas_bytes bigint not null default 0,
    derivados_bytes bigint not null default 0,
    videos_bytes bigint not null default 0,
    comprovantes_bytes bigint not null default 0,
    reserva_bytes bigint not null default 0
);

create index idx_armazenamento_amostra_coletado
    on armazenamento_amostra (coletado_em);

create index idx_arquivos_referencia_finalidade
    on arquivos (referencia_id, finalidade, status);

create index idx_galerias_status_expurgo
    on galerias (status, expurgo_status);
