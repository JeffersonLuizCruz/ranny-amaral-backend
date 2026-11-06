package com.ranny.fotografia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida o schema real do PostgreSQL.
 *
 * <p>Roda apenas em {@code mvn verify} (Failsafe), com o perfil {@code postgres}.
 * O Flyway aplica as migrações e o Hibernate valida o schema com
 * {@code ddl-auto=validate}; se houver divergência, o contexto falha.
 */
@SpringBootTest
@ActiveProfiles("postgres")
class PostgresSchemaIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aplicaMigracoesEValidaSchema() {
        Integer tabelas = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public'
                  and table_name in (
                    'clientes', 'pacotes', 'pacote_itens', 'agendamentos',
                    'contratos', 'solicitacoes_aceite', 'modelos_contrato',
                    'estudio', 'event_publication', 'event_publication_archive'
                  )
                """, Integer.class);
        assertThat(tabelas).isEqualTo(10);

        Integer migracoes = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class);
        assertThat(migracoes).isGreaterThanOrEqualTo(2);
    }

    @Test
    void aplicaBuscaDesnormalizadaDeContratos() {
        Integer colunas = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'contratos'
                  and column_name in ('cliente_nome', 'pacote_nome')
                """, Integer.class);
        assertThat(colunas).isEqualTo(2);
    }
}
