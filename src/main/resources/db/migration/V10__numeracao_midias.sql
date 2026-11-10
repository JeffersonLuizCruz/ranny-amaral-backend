-- V10 - Numero estavel das midias.
--
-- O numero exibido no grid e o `ordem` da midia. A renumeracao preserva a
-- ordem visual atual e remove duplicidades antigas; a constraint garante que
-- dois uploads nao recebam o mesmo numero no mesmo tipo de midia.

with numeradas as (
    select id,
           row_number() over (
               partition by galeria_id, tipo
               order by ordem, criado_em, id
           ) as nova_ordem
    from midias_galeria
)
update midias_galeria midia
set ordem = numerada.nova_ordem
from numeradas numerada
where midia.id = numerada.id;

alter table midias_galeria
    add constraint midias_galeria_galeria_tipo_ordem_key unique (galeria_id, tipo, ordem);
