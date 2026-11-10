-- V12 - Faixas progressivas de preco da foto extra.
--
-- O valor base continua em `pacotes.valor_foto_extra`; cada faixa define o
-- valor unitario a partir de uma quantidade minima. As condicoes sao
-- congeladas no agendamento e na galeria como JSON para preservar vendas ja
-- fechadas.

create table pacote_faixas_foto_extra (
    pacote_id varchar(64) not null,
    quantidade_minima integer not null,
    valor_unitario numeric(10,2) not null,
    constraint pacote_faixas_foto_extra_pkey
        primary key (pacote_id, quantidade_minima),
    constraint fk_pacote_faixas_foto_extra_pacote
        foreign key (pacote_id) references pacotes (id) on delete cascade
);

alter table agendamentos
    add column cond_faixas_foto_extra text;

alter table galerias
    add column faixas_foto_extra text;

alter table pedidos_extras
    add column detalhamento_extras text;
