-- V17 - Liberacao progressiva por midia.
--
-- liberada_em marca quando a midia ficou disponivel para download. O cliente
-- nao ve versao editada/RAW; o backend resolve o melhor arquivo.
-- liberacao_automatica define se a midia pronta entra na liberacao automatica
-- apos a selecao e a quitacao dos extras.

alter table midias_galeria
    add column liberada_em timestamp(6) with time zone;

-- entregue_em separa "ja foi baixada/entregue" de "esta liberada agora".
-- Revogar uma liberacao remove liberada_em, mas mantem a trava da midia em
-- reaberturas futuras.
alter table midias_galeria
    add column entregue_em timestamp(6) with time zone;

alter table midias_galeria
    add column liberacao_automatica boolean not null default true;

create index idx_midias_galeria_liberada
    on midias_galeria (galeria_id, liberada_em);

create index idx_midias_galeria_entregue
    on midias_galeria (galeria_id, entregue_em);
