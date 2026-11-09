-- V9 - Chave Pix de extras e comprovante do pedido de extras.
--
-- A chave do ensaio continua em `estudio.chave_pix` e e usada apenas pelo
-- contrato. A chave de compra de fotos extras e separada e aparece somente no
-- fluxo publico da galeria. O pedido de extras guarda um snapshot da chave e o
-- comprovante enviado pelo cliente.

alter table estudio
    add column chave_pix_extras varchar(180);

alter table estudio
    add column instrucoes_pagamento_extras varchar(1500);

alter table pedidos_extras
    add column chave_pix varchar(180);

alter table pedidos_extras
    add column instrucoes_pagamento varchar(1500);

alter table pedidos_extras
    add column comprovante_arquivo_id varchar(64);

alter table pedidos_extras
    add column comprovante_nome varchar(255);

alter table pedidos_extras
    add column comprovante_enviado_em timestamp(6) with time zone;
