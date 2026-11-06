-- V5 - Versao editada da midia.
--
-- A versao editada e um arquivo proprio (FOTO_EDITADA), separado do original e
-- dos derivados tecnicos. Os derivados passam a registrar se vieram do original
-- ou da edicao, permitindo que a entrega priorize a versao final.
-- A V4 permanece imutavel.

alter table arquivos drop constraint arquivos_finalidade_check;
alter table arquivos add constraint arquivos_finalidade_check check (
    finalidade in (
        'COMPROVANTE_PAGAMENTO',
        'FOTO_ORIGINAL',
        'FOTO_DERIVADO',
        'FOTO_EDITADA',
        'VIDEO_ORIGINAL',
        'CONTRATO_PDF'
    )
);

alter table midia_derivados
    add column origem varchar(10) not null default 'ORIGINAL';

alter table midia_derivados
    add constraint midia_derivados_origem_check check (origem in ('ORIGINAL', 'EDITADA'));

create index idx_midia_derivados_midia_origem on midia_derivados (midia_id, origem);
