-- V19 - Finalidade da midia e marca d'agua nas previas.
--
-- finalidade separa a foto original que ainda sera editada (EDICAO) da foto
-- que o estudio ja publica pronta para download (PRONTA). As duas continuam
-- contando na mesma cota e nos extras.
--
-- marcada identifica o derivado de previa com o nome do estudio aplicado na
-- imagem. O download continua usando o arquivo limpo.

alter table midias_galeria
    add column finalidade varchar(20) not null default 'EDICAO';

alter table midias_galeria
    add constraint midias_galeria_finalidade_check
    check (finalidade in ('EDICAO', 'PRONTA'));

create index idx_midias_galeria_finalidade
    on midias_galeria (galeria_id, finalidade);

alter table midia_derivados
    add column marcada boolean not null default false;

create index idx_midia_derivados_marcada
    on midia_derivados (midia_id, marcada);
