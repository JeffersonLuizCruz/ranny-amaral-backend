-- V4 - Galerias, midias, selecao, extras e entrega.
--
-- O binario continua em `arquivos`/object storage. `midias_galeria` e o ativo
-- semantico (foto ou video) e `midia_derivados` guarda as versoes geradas.
-- A V3 permanece imutavel.

alter table arquivos drop constraint arquivos_finalidade_check;
alter table arquivos add constraint arquivos_finalidade_check check (
    finalidade in (
        'COMPROVANTE_PAGAMENTO',
        'FOTO_ORIGINAL',
        'FOTO_DERIVADO',
        'VIDEO_ORIGINAL',
        'CONTRATO_PDF'
    )
);

create table galerias (
    id varchar(64) not null,
    lock_version bigint not null,
    estudio_id varchar(64) not null,
    agendamento_id varchar(64) not null,
    contrato_id varchar(64),
    cliente_id varchar(64),
    cliente_nome varchar(120),
    status varchar(20) not null,
    fotos_inclusas integer not null,
    videos_inclusos integer not null,
    valor_foto_extra numeric(10,2) not null,
    selecao_atual_id varchar(64),
    pedido_extras_id varchar(64),
    criado_em timestamp(6) with time zone not null,
    atualizado_em timestamp(6) with time zone not null,
    constraint galerias_pkey primary key (id),
    constraint galerias_agendamento_key unique (agendamento_id),
    constraint galerias_status_check check (
        status in (
            'AGUARDANDO_UPLOAD', 'EM_SELECAO', 'SELECAO_ENVIADA',
            'EM_EDICAO', 'PRONTA_ENTREGA', 'ENTREGUE', 'CANCELADA'
        )
    )
);

create index idx_galerias_estudio_status on galerias (estudio_id, status, atualizado_em desc);
create index idx_galerias_cliente on galerias (cliente_id);
create index idx_galerias_cliente_nome on galerias (cliente_nome);

create table midias_galeria (
    id varchar(64) not null,
    lock_version bigint not null,
    galeria_id varchar(64) not null,
    tipo varchar(10) not null,
    arquivo_original_id varchar(64) not null,
    arquivo_editado_id varchar(64),
    nome_original varchar(255) not null,
    mime_type varchar(120) not null,
    tamanho_bytes bigint not null,
    largura integer,
    altura integer,
    duracao_segundos integer,
    ordem integer not null,
    status varchar(20) not null,
    erro_processamento varchar(500),
    criado_em timestamp(6) with time zone not null,
    atualizado_em timestamp(6) with time zone not null,
    constraint midias_galeria_pkey primary key (id),
    constraint midias_galeria_tipo_check check (tipo in ('FOTO', 'VIDEO')),
    constraint midias_galeria_status_check check (
        status in ('ENVIANDO', 'PROCESSANDO', 'DISPONIVEL', 'FALHA', 'REMOVIDA')
    )
);

create index idx_midias_galeria_galeria on midias_galeria (galeria_id, ordem);
create index idx_midias_galeria_status on midias_galeria (galeria_id, status);

create table midia_derivados (
    id varchar(64) not null,
    midia_id varchar(64) not null,
    tipo varchar(20) not null,
    arquivo_id varchar(64) not null,
    largura integer,
    altura integer,
    formato varchar(20),
    criado_em timestamp(6) with time zone not null,
    constraint midia_derivados_pkey primary key (id),
    constraint midia_derivados_tipo_check check (tipo in ('THUMB', 'WEB'))
);

create index idx_midia_derivados_midia on midia_derivados (midia_id);

create table links_galeria (
    id varchar(64) not null,
    lock_version bigint not null,
    galeria_id varchar(64) not null,
    tipo varchar(20) not null,
    token varchar(80) not null,
    status varchar(20) not null,
    expira_em timestamp(6) with time zone not null,
    visualizada_em timestamp(6) with time zone,
    protocolo varchar(30),
    criado_em timestamp(6) with time zone not null,
    atualizado_em timestamp(6) with time zone not null,
    constraint links_galeria_pkey primary key (id),
    constraint links_galeria_token_key unique (token),
    constraint links_galeria_tipo_check check (tipo in ('SELECAO', 'ENTREGA')),
    constraint links_galeria_status_check check (
        status in ('ATIVO', 'EXPIRADO', 'REVOGADO')
    )
);

create index idx_links_galeria_galeria on links_galeria (galeria_id, tipo);

create table selecoes_galeria (
    id varchar(64) not null,
    lock_version bigint not null,
    galeria_id varchar(64) not null,
    protocolo varchar(30) not null,
    status varchar(20) not null,
    fotos_selecionadas integer not null,
    videos_selecionados integer not null,
    fotos_extras integer not null,
    valor_extras numeric(10,2) not null,
    enviada_em timestamp(6) with time zone not null,
    ip varchar(45),
    user_agent varchar(300),
    criado_em timestamp(6) with time zone not null,
    atualizado_em timestamp(6) with time zone not null,
    constraint selecoes_galeria_pkey primary key (id),
    constraint selecoes_galeria_protocolo_key unique (protocolo),
    constraint selecoes_galeria_status_check check (status in ('ENVIADA', 'REABERTA', 'CANCELADA'))
);

create index idx_selecoes_galeria_galeria on selecoes_galeria (galeria_id);

create table selecao_itens (
    id varchar(64) not null,
    selecao_id varchar(64) not null,
    midia_id varchar(64) not null,
    tipo varchar(10) not null,
    constraint selecao_itens_pkey primary key (id),
    constraint selecao_itens_unique unique (selecao_id, midia_id)
);

create table pedidos_extras (
    id varchar(64) not null,
    lock_version bigint not null,
    galeria_id varchar(64) not null,
    selecao_id varchar(64) not null,
    quantidade_fotos integer not null,
    valor_unitario numeric(10,2) not null,
    valor_total numeric(10,2) not null,
    status varchar(20) not null,
    forma_pagamento varchar(20),
    confirmado_em timestamp(6) with time zone,
    confirmado_por varchar(120),
    liberacao_motivo varchar(500),
    liberado_por varchar(120),
    liberado_em timestamp(6) with time zone,
    criado_em timestamp(6) with time zone not null,
    atualizado_em timestamp(6) with time zone not null,
    constraint pedidos_extras_pkey primary key (id),
    constraint pedidos_extras_status_check check (
        status in ('PENDENTE', 'PAGO', 'ISENTO', 'CANCELADO')
    )
);

create index idx_pedidos_extras_galeria on pedidos_extras (galeria_id);

create table downloads_galeria (
    id varchar(64) not null,
    galeria_id varchar(64) not null,
    link_id varchar(64) not null,
    midia_id varchar(64),
    arquivo_id varchar(64) not null,
    ip varchar(45),
    user_agent varchar(300),
    baixado_em timestamp(6) with time zone not null,
    constraint downloads_galeria_pkey primary key (id)
);

create index idx_downloads_galeria on downloads_galeria (galeria_id, baixado_em);

create table eventos_galeria (
    id varchar(64) not null,
    galeria_id varchar(64) not null,
    tipo varchar(40) not null,
    detalhe varchar(500),
    criado_em timestamp(6) with time zone not null,
    constraint eventos_galeria_pkey primary key (id)
);

create index idx_eventos_galeria on eventos_galeria (galeria_id, criado_em);
