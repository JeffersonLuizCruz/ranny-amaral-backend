-- Autenticacao e gestao de usuarios.
--
-- A tabela guarda apenas o hash da senha (BCrypt). Senhas temporarias nunca
-- sao persistidas em texto puro: o admin recebe o valor uma unica vez e o
-- usuario e obrigado a troca-lo no primeiro acesso.

create table usuarios (
    id varchar(64) primary key,
    lock_version bigint not null default 0,
    nome varchar(160) not null,
    email varchar(255) not null,
    senha_hash varchar(255) not null,
    role varchar(20) not null,
    ativo boolean not null default true,
    deve_trocar_senha boolean not null default true,
    token_version integer not null default 0,
    tentativas_falhas integer not null default 0,
    bloqueado_ate timestamp with time zone,
    ultimo_login_em timestamp with time zone,
    senha_alterada_em timestamp with time zone,
    criado_em timestamp with time zone not null,
    atualizado_em timestamp with time zone not null,
    constraint ck_usuarios_role check (role in ('ADMIN', 'GESTOR'))
);

create unique index ux_usuarios_email on usuarios (lower(email));

create table refresh_tokens (
    id varchar(64) primary key,
    usuario_id varchar(64) not null references usuarios (id),
    token_hash varchar(64) not null,
    expira_em timestamp with time zone not null,
    revogado_em timestamp with time zone,
    substituido_por varchar(64),
    ip varchar(64),
    user_agent varchar(300),
    criado_em timestamp with time zone not null,
    constraint ux_refresh_tokens_hash unique (token_hash)
);

create index ix_refresh_tokens_usuario on refresh_tokens (usuario_id);
create index ix_refresh_tokens_expira on refresh_tokens (expira_em);

create table auth_eventos (
    id varchar(64) primary key,
    usuario_id varchar(64),
    tipo varchar(40) not null,
    ip varchar(64),
    user_agent varchar(300),
    detalhe varchar(500),
    criado_em timestamp with time zone not null
);

create index ix_auth_eventos_usuario on auth_eventos (usuario_id);
create index ix_auth_eventos_criado on auth_eventos (criado_em);
