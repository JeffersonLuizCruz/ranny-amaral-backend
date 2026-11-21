-- Taxa de deslocamento do ensaio: valor cobrado pelo studio e a decisao de
-- repassar ao cliente ou absorver. O total do agendamento (valor_acordado) ja
-- inclui a taxa quando ela e repassada.

alter table agendamentos
    add column taxa_deslocamento numeric(10,2) not null default 0,
    add column taxa_deslocamento_repassada boolean not null default false;

alter table agendamentos
    add constraint ck_agendamentos_taxa_deslocamento
    check (taxa_deslocamento >= 0);
