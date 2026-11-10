-- V13 - Marca de download do ZIP da entrega pelo cliente.
--
-- O filtro "Baixadas" considera apenas o download do ZIP completo. O download
-- individual continua registrado em `downloads_galeria`, mas nao marca a
-- galeria. O backfill recupera os ZIPs antigos pelos eventos de historico.

alter table galerias
    add column zip_baixado_em timestamp(6) with time zone;

update galerias g
set zip_baixado_em = sub.primeiro_download
from (
    select e.galeria_id, min(e.criado_em) as primeiro_download
    from eventos_galeria e
    where e.tipo = 'DOWNLOAD_REGISTRADO'
      and e.detalhe like '%em lote%'
    group by e.galeria_id
) sub
where g.id = sub.galeria_id;

create index idx_galerias_zip_baixado
    on galerias (zip_baixado_em desc)
    where zip_baixado_em is not null;
