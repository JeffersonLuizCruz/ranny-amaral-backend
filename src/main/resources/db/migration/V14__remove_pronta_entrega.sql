-- V14 - Remove o estado intermediario PRONTA_ENTREGA.
--
-- A publicacao da entrega passa a ser feita diretamente a partir de EM_EDICAO;
-- a prontidao e calculada pelos arquivos finais e pelos extras. Galerias sem
-- extras passam da selecao direto para EM_EDICAO. Como o projeto esta em
-- desenvolvimento, os estados antigos sao apenas convertidos.

update galerias
set status = 'EM_EDICAO'
where status = 'PRONTA_ENTREGA';

update galerias g
set status = 'EM_EDICAO'
where g.status = 'SELECAO_ENVIADA'
  and not exists (
      select 1
      from pedidos_extras p
      where p.galeria_id = g.id
        and p.status = 'PENDENTE'
  );

alter table galerias
    drop constraint galerias_status_check;

alter table galerias
    add constraint galerias_status_check check (
        status in (
            'AGUARDANDO_UPLOAD', 'EM_SELECAO', 'SELECAO_ENVIADA',
            'EM_EDICAO', 'ENTREGUE', 'CANCELADA'
        )
    );
