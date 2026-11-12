-- V16 - Link unico da galeria.
--
-- O link de selecao passa a ser o link unico do cliente e continua valido
-- durante a edicao e a entrega. O tipo ENTREGA permanece apenas para
-- compatibilidade com links ja emitidos.

alter table links_galeria drop constraint links_galeria_tipo_check;

update links_galeria set tipo = 'GALERIA' where tipo = 'SELECAO';

alter table links_galeria add constraint links_galeria_tipo_check
    check (tipo in ('SELECAO', 'ENTREGA', 'GALERIA'));
