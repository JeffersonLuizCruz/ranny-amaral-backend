-- V1 - Baseline do núcleo do CRM.
--
-- Schema gerado a partir das entidades JPA com o dialeto PostgreSQL e revisado
-- para índices de consulta. O Hibernate roda com ddl-auto=validate; qualquer
-- mudança futura deve entrar em uma nova migração (V2, V3, ...).

-- ---------------------------------------------------------------------------
-- Clientes
-- ---------------------------------------------------------------------------
create table clientes (
    atualizado_em timestamp(6) with time zone not null,
    criado_em timestamp(6) with time zone not null,
    lock_version bigint not null,
    tipo_documento varchar(10),
    origem varchar(20) not null,
    documento varchar(30),
    whatsapp varchar(30),
    id varchar(64) not null,
    cidade varchar(120),
    nome varchar(120) not null,
    email varchar(180),
    observacoes varchar(1000),
    constraint clientes_pkey primary key (id),
    constraint clientes_origem_check check (origem in ('INSTAGRAM','INDICACAO','GOOGLE','WHATSAPP','SITE','CONTRATO','OUTRO')),
    constraint clientes_tipo_documento_check check (tipo_documento in ('RG','CPF'))
);

create index idx_clientes_nome on clientes (nome);
create index idx_clientes_whatsapp on clientes (whatsapp);
create index idx_clientes_documento on clientes (documento);

-- ---------------------------------------------------------------------------
-- Pacotes
-- ---------------------------------------------------------------------------
create table pacotes (
    ativo boolean not null,
    duracao_minutos integer not null,
    promocao_ativa boolean,
    promocao_valida_ate date,
    promocao_valor numeric(10,2),
    quantidade_fotos_inclusas integer not null,
    quantidade_videos_inclusos integer not null,
    valor numeric(10,2) not null,
    valor_foto_extra numeric(10,2) not null,
    atualizado_em timestamp(6) with time zone not null,
    criado_em timestamp(6) with time zone not null,
    lock_version bigint not null,
    cor_hex varchar(9) not null,
    id varchar(64) not null,
    nome varchar(120) not null,
    promocao_rotulo varchar(120),
    descricao varchar(1000) not null,
    constraint pacotes_pkey primary key (id)
);

create index idx_pacotes_ativo on pacotes (ativo);
create index idx_pacotes_nome on pacotes (nome);

create table pacote_itens (
    posicao integer not null,
    pacote_id varchar(64) not null,
    item varchar(200) not null,
    constraint pacote_itens_pkey primary key (posicao, pacote_id),
    constraint pacote_itens_posicao_check check (posicao >= 0),
    constraint fk_pacote_itens_pacote foreign key (pacote_id) references pacotes (id)
);

create index idx_pacote_itens_pacote on pacote_itens (pacote_id);

-- ---------------------------------------------------------------------------
-- Agenda
-- ---------------------------------------------------------------------------
create table agendamentos (
    cond_fotos_inclusas integer,
    cond_promocao_valor numeric(10,2),
    cond_valor_foto_extra numeric(10,2),
    cond_valor_tabela numeric(10,2),
    cond_videos_inclusos integer,
    entrada_percentual numeric(5,2),
    entrada_valor numeric(10,2),
    percentual_entrada numeric(5,2) not null,
    saldo_restante numeric(10,2) not null,
    saldo_valor numeric(10,2),
    valor_acordado numeric(10,2) not null,
    valor_entrada numeric(10,2) not null,
    atualizado_em timestamp(6) with time zone not null,
    criado_em timestamp(6) with time zone not null,
    data_hora_fim timestamp(6) with time zone not null,
    data_hora_inicio timestamp(6) with time zone not null,
    entrada_confirmado_em timestamp(6) with time zone,
    lock_version bigint not null,
    saldo_recebido_em timestamp(6) with time zone,
    entrada_status varchar(20) not null,
    saldo_forma_pagamento varchar(20),
    saldo_status varchar(20),
    status varchar(20) not null,
    contato_whatsapp varchar(30),
    cliente_id varchar(64),
    contrato_id varchar(64),
    galeria_id varchar(64),
    id varchar(64) not null,
    pacote_id varchar(64) not null,
    cond_promocao_rotulo varchar(120),
    contato_nome varchar(120),
    entrada_confirmado_por varchar(120),
    saldo_recebido_por varchar(120),
    local varchar(180) not null,
    entrada_observacoes varchar(1000),
    observacoes varchar(1000),
    saldo_observacoes varchar(1000),
    entrada_comprovante text,
    entrada_rejeicao text,
    constraint agendamentos_pkey primary key (id),
    constraint agendamentos_entrada_status_check check (entrada_status in ('PENDENTE','CONFIRMADO','ISENTO','CANCELADO')),
    constraint agendamentos_saldo_forma_pagamento_check check (saldo_forma_pagamento in ('PIX','DINHEIRO','OUTRO')),
    constraint agendamentos_saldo_status_check check (saldo_status in ('PENDENTE','CONFIRMADO','ISENTO','CANCELADO')),
    constraint agendamentos_status_check check (status in ('RESERVADO','CONFIRMADO','REALIZADO','CANCELADO'))
);

create index idx_agendamentos_inicio on agendamentos (data_hora_inicio);
create index idx_agendamentos_cliente on agendamentos (cliente_id);
create index idx_agendamentos_pacote on agendamentos (pacote_id);
create index idx_agendamentos_contrato on agendamentos (contrato_id);
create index idx_agendamentos_status on agendamentos (status);

-- ---------------------------------------------------------------------------
-- Contratos
-- ---------------------------------------------------------------------------
create table contratos (
    entrada_percentual numeric(5,2),
    entrada_valor numeric(10,2),
    percentual_entrada numeric(5,2) not null,
    saldo_restante numeric(10,2) not null,
    saldo_valor numeric(10,2),
    valor_entrada numeric(10,2) not null,
    valor_total numeric(10,2) not null,
    atualizado_em timestamp(6) with time zone not null,
    criado_em timestamp(6) with time zone not null,
    data_emissao timestamp(6) with time zone not null,
    data_validade timestamp(6) with time zone,
    entrada_confirmado_em timestamp(6) with time zone,
    lock_version bigint not null,
    saldo_recebido_em timestamp(6) with time zone,
    entrada_status varchar(20) not null,
    saldo_forma_pagamento varchar(20),
    saldo_status varchar(20),
    status_aceite varchar(20) not null,
    status_pagamento varchar(20) not null,
    status_pagamento_final varchar(20) not null,
    numero varchar(30) not null,
    forma_pagamento varchar(40),
    agendamento_id varchar(64),
    cliente_id varchar(64),
    id varchar(64) not null,
    modelo_id varchar(64) not null,
    pacote_id varchar(64),
    solicitacao_atual_id varchar(64),
    entrada_confirmado_por varchar(120),
    saldo_recebido_por varchar(120),
    titulo varchar(180) not null,
    condicoes_pagamento varchar(1000),
    entrada_observacoes varchar(1000),
    observacoes varchar(1000),
    saldo_observacoes varchar(1000),
    aceite text,
    clausulas text,
    clausulas_modelo text,
    entrada_comprovante text,
    entrada_rejeicao text,
    historico text not null,
    snapshot text not null,
    constraint contratos_pkey primary key (id),
    constraint contratos_numero_key unique (numero),
    constraint contratos_entrada_status_check check (entrada_status in ('PENDENTE','CONFIRMADO','ISENTO','CANCELADO')),
    constraint contratos_saldo_forma_pagamento_check check (saldo_forma_pagamento in ('PIX','DINHEIRO','OUTRO')),
    constraint contratos_saldo_status_check check (saldo_status in ('PENDENTE','CONFIRMADO','ISENTO','CANCELADO')),
    constraint contratos_status_aceite_check check (status_aceite in ('RASCUNHO','PENDENTE','ACEITO','EXPIRADO','CANCELADO')),
    constraint contratos_status_pagamento_check check (status_pagamento in ('PENDENTE','CONFIRMADO','ISENTO','CANCELADO')),
    constraint contratos_status_pagamento_final_check check (status_pagamento_final in ('PENDENTE','CONFIRMADO','ISENTO','CANCELADO'))
);

create index idx_contratos_status_aceite on contratos (status_aceite);
create index idx_contratos_cliente on contratos (cliente_id);
create index idx_contratos_agendamento on contratos (agendamento_id);
create index idx_contratos_data_emissao on contratos (data_emissao);

-- ---------------------------------------------------------------------------
-- Solicitacoes de aceite (links publicos)
-- ---------------------------------------------------------------------------
create table solicitacoes_aceite (
    atualizado_em timestamp(6) with time zone not null,
    criada_em timestamp(6) with time zone not null,
    criado_em timestamp(6) with time zone not null,
    expira_em timestamp(6) with time zone not null,
    lock_version bigint not null,
    visualizada_em timestamp(6) with time zone,
    status varchar(20) not null,
    protocolo varchar(30),
    contrato_id varchar(64) not null,
    id varchar(64) not null,
    token varchar(80) not null,
    dados_aceite text,
    constraint solicitacoes_aceite_pkey primary key (id),
    constraint solicitacoes_aceite_protocolo_key unique (protocolo),
    constraint solicitacoes_aceite_token_key unique (token),
    constraint solicitacoes_aceite_status_check check (status in ('PENDENTE','VISUALIZADA','ACEITA','EXPIRADA','CANCELADA'))
);

create index idx_solicitacoes_contrato on solicitacoes_aceite (contrato_id);
create index idx_solicitacoes_expira_em on solicitacoes_aceite (expira_em);

-- ---------------------------------------------------------------------------
-- Modelos de contrato e estudio
-- ---------------------------------------------------------------------------
create table modelos_contrato (
    ativo boolean not null,
    versao integer not null,
    atualizado_em timestamp(6) with time zone not null,
    criado_em timestamp(6) with time zone not null,
    lock_version bigint not null,
    id varchar(64) not null,
    nome varchar(160) not null,
    descricao varchar(1000),
    clausulas text,
    constraint modelos_contrato_pkey primary key (id)
);

create table estudio (
    estado varchar(2),
    percentual_entrada_padrao numeric(5,2) not null,
    prazo_pagamento_dias integer,
    atualizado_em timestamp(6) with time zone not null,
    criado_em timestamp(6) with time zone not null,
    lock_version bigint not null,
    cnpj varchar(20),
    telefone varchar(30),
    id varchar(64) not null,
    cidade varchar(120),
    foro varchar(120),
    responsavel varchar(120),
    nome varchar(160) not null,
    razao_social varchar(160),
    chave_pix varchar(180),
    email varchar(180),
    site varchar(180),
    endereco varchar(200),
    instrucoes_pagamento varchar(1500),
    constraint estudio_pkey primary key (id)
);

-- ---------------------------------------------------------------------------
-- Outbox do Spring Modulith
--
-- serialized_event fica em varchar(255): o evento de contrato e compacto por
-- contrato de teste (EventSerializationSpec garante o limite).
-- ---------------------------------------------------------------------------
create table event_publication (
    completion_attempts integer not null,
    completion_date timestamp(6) with time zone,
    last_resubmission_date timestamp(6) with time zone,
    publication_date timestamp(6) with time zone not null,
    id uuid not null,
    event_type varchar(255) not null,
    listener_id varchar(255) not null,
    serialized_event varchar(255) not null,
    status varchar(255),
    constraint event_publication_pkey primary key (id),
    constraint event_publication_status_check check (status in ('PUBLISHED','PROCESSING','COMPLETED','FAILED','RESUBMITTED'))
);

create index idx_event_publication_completion on event_publication (completion_date);

create table event_publication_archive (
    completion_attempts integer not null,
    completion_date timestamp(6) with time zone,
    last_resubmission_date timestamp(6) with time zone,
    publication_date timestamp(6) with time zone not null,
    id uuid not null,
    event_type varchar(255) not null,
    listener_id varchar(255) not null,
    serialized_event varchar(255) not null,
    status varchar(255),
    constraint event_publication_archive_pkey primary key (id),
    constraint event_publication_archive_status_check check (status in ('PUBLISHED','PROCESSING','COMPLETED','FAILED','RESUBMITTED'))
);

create index idx_event_publication_archive_completion on event_publication_archive (completion_date);
