-- O expurgo marca arquivos como EXPURGADO, mas a constraint original de V3
-- nao previa esse estado. Sem isso, remover midias e expurgar derivados falha
-- com violacao de check constraint.
alter table arquivos drop constraint arquivos_status_check;

alter table arquivos add constraint arquivos_status_check check (
    status in ('PENDENTE', 'ENVIANDO', 'PROCESSANDO', 'DISPONIVEL', 'FALHA', 'CANCELADO', 'EXPURGADO')
);
