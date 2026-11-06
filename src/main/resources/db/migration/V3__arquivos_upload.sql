-- V3 - Catalogo generico de arquivos e vinculo do comprovante.
--
-- Os bytes ficam no object storage (MinIO/S3). O banco guarda apenas metadados,
-- a finalidade e a chave opaca. A separacao permite reutilizar o mesmo modelo
-- para comprovantes e, futuramente, para fotos de galeria e derivados.

create table arquivos (
    id varchar(64) not null,
    lock_version bigint not null,
    finalidade varchar(30) not null,
    referencia_id varchar(64) not null,
    provider varchar(20) not null,
    bucket varchar(120) not null,
    object_key varchar(512) not null,
    nome_original varchar(255) not null,
    mime_type varchar(120) not null,
    tamanho_bytes bigint not null,
    checksum_sha256 varchar(64),
    status varchar(20) not null,
    upload_id varchar(255),
    criado_em timestamp(6) with time zone not null,
    atualizado_em timestamp(6) with time zone not null,
    concluido_em timestamp(6) with time zone,
    expira_em timestamp(6) with time zone,
    constraint arquivos_pkey primary key (id),
    constraint arquivos_finalidade_check check (
        finalidade in ('COMPROVANTE_PAGAMENTO', 'FOTO_ORIGINAL', 'FOTO_DERIVADO', 'CONTRATO_PDF')
    ),
    constraint arquivos_status_check check (
        status in ('PENDENTE', 'ENVIANDO', 'PROCESSANDO', 'DISPONIVEL', 'FALHA', 'CANCELADO')
    )
);

create index idx_arquivos_referencia on arquivos (finalidade, referencia_id);
create index idx_arquivos_status_expira on arquivos (status, expira_em);

create unique index uq_arquivos_checksum_disponivel
    on arquivos (finalidade, referencia_id, checksum_sha256)
    where checksum_sha256 is not null and status = 'DISPONIVEL';

alter table contratos add column entrada_arquivo_id varchar(64);
alter table agendamentos add column entrada_arquivo_id varchar(64);

create index idx_contratos_entrada_arquivo on contratos (entrada_arquivo_id);
create index idx_agendamentos_entrada_arquivo on agendamentos (entrada_arquivo_id);
