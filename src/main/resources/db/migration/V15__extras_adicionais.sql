-- V15 - Extras adicionais apos pagamento e trava de midias entregues.
--
-- `selecao_entregue_id` guarda a ultima selecao efetivamente entregue. Em
-- reaberturas futuras, as midias dessa selecao continuam obrigatorias para
-- impedir que o cliente troque fotos ja baixadas sem pagar o adicional.
-- Pedidos de extras passam a ser cumulativos (schema ja permite mais de uma
-- linha por galeria); o indice acelera a busca do pedido pendente.

alter table galerias
    add column selecao_entregue_id varchar(64);

update galerias g
   set selecao_entregue_id = (
       select s.id
         from selecoes_galeria s
        where s.galeria_id = g.id
        order by s.enviada_em desc
        limit 1
   )
 where exists (
           select 1 from links_galeria l
            where l.galeria_id = g.id and l.tipo = 'ENTREGA'
       )
    or exists (
           select 1 from downloads_galeria d
            where d.galeria_id = g.id
       );

create index idx_pedidos_extras_galeria_status
    on pedidos_extras (galeria_id, status);
