-- V6 - Liberacao dos arquivos originais (RAW) e versao baixada.
--
-- O staff pode liberar os originais de uma galeria de forma explicita. A
-- liberacao e auditada e reversivel; o download registra qual versao foi
-- entregue (EDITADA ou ORIGINAL). A V5 permanece imutavel.

alter table galerias
    add column raw_liberado boolean not null default false;

alter table galerias
    add column raw_liberado_em timestamp(6) with time zone;

alter table galerias
    add column raw_liberado_por varchar(120);

alter table downloads_galeria
    add column versao varchar(10);

alter table downloads_galeria
    add constraint downloads_galeria_versao_check check (
        versao is null or versao in ('ORIGINAL', 'EDITADA')
    );
