-- V11 - Token de curta duracao para o staff baixar as fotos selecionadas.
--
-- O navegador nao envia o JWT ao abrir um link direto de download. O endpoint
-- autenticado gera este token de capability, valido por poucos minutos, e a
-- rota publica faz o streaming do ZIP sem expor o JWT na URL.

create table tokens_download_selecao (
    id varchar(64) not null,
    galeria_id varchar(64) not null,
    selecao_id varchar(64) not null,
    token varchar(80) not null,
    expira_em timestamp(6) with time zone not null,
    criado_em timestamp(6) with time zone not null,
    atualizado_em timestamp(6) with time zone not null,
    constraint tokens_download_selecao_pkey primary key (id),
    constraint tokens_download_selecao_token_key unique (token)
);

create index idx_tokens_download_selecao_galeria
    on tokens_download_selecao (galeria_id);

create index idx_tokens_download_selecao_expira
    on tokens_download_selecao (expira_em);
