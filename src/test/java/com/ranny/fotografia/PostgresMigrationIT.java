package com.ranny.fotografia;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida a evolucao incremental do schema com dados existentes.
 *
 * <p>Aplica V1, insere um contrato com snapshot e migra ate a versao atual.
 * Roda apenas no verify com POSTGRES_TEST_URL definido.
 */
@EnabledIfEnvironmentVariable(named = "POSTGRES_TEST_URL", matches = ".+")
class PostgresMigrationIT {

    private static final String URL = System.getenv()
            .getOrDefault("POSTGRES_TEST_URL", "jdbc:postgresql://localhost:5432/photoizer");
    private static final String USER = System.getenv()
            .getOrDefault("POSTGRES_TEST_USERNAME", "photoizer");
    private static final String PASSWORD = System.getenv()
            .getOrDefault("POSTGRES_TEST_PASSWORD", "photoizer");

    @Test
    void rejeitaSaldoConfirmadoParcialNoBanco() throws Exception {
        String schema = "inv_" + UUID.randomUUID().toString().replace("-", "");

        try {
            Flyway.configure()
                    .dataSource(URL, USER, PASSWORD)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeUpdate("""
                        insert into %s.contratos (
                          id, lock_version, numero, modelo_id, status_aceite,
                          status_pagamento, status_pagamento_final, entrada_status,
                          saldo_status, saldo_valor, titulo, snapshot,
                          valor_total, valor_entrada, percentual_entrada, saldo_restante,
                          data_emissao, historico, criado_em, atualizado_em
                        ) values (
                          'ct-invalido', 0, 'CT-2026-9998', 'modelo-1', 'RASCUNHO',
                          'PENDENTE', 'CONFIRMADO', 'PENDENTE',
                          'CONFIRMADO', 100.00, 'Contrato invalido',
                          '{"cliente":{"nome":"Marina Invalida"},"pacote":{"nome":"Pacote"}}',
                          1000.00, 300.00, 30.00, 700.00,
                          now(), '[]', now(), now()
                        )
                        """.formatted(schema)))
                        .isInstanceOf(SQLException.class);

                try (ResultSet result = statement.executeQuery("""
                        select count(*) from information_schema.table_constraints
                        where table_schema = '%s'
                          and constraint_name in (
                            'ck_contratos_saldo_confirmado_integral',
                            'ck_agendamentos_saldo_confirmado_integral'
                          )
                        """.formatted(schema))) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isEqualTo(2);
                }
            }
        } finally {
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement statement = connection.createStatement()) {
                statement.execute("drop schema if exists %s cascade".formatted(schema));
            }
        }
    }

    @Test
    void migraDadosDaV1ParaV4ComBackfill() throws Exception {
        String schema = "mig_" + UUID.randomUUID().toString().replace("-", "");

        Flyway v1 = Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1"))
                .load();
        v1.migrate();

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        insert into %s.contratos (
                          id, lock_version, numero, modelo_id, status_aceite,
                          status_pagamento, status_pagamento_final, entrada_status, titulo, snapshot,
                          valor_total, valor_entrada, percentual_entrada, saldo_restante,
                          data_emissao, historico, criado_em, atualizado_em
                        ) values (
                          'ct-mig', 0, 'CT-2026-9999', 'modelo-1', 'RASCUNHO',
                          'PENDENTE', 'PENDENTE', 'PENDENTE', 'Contrato legado',
                          '{"cliente":{"nome":"Marina Legado"},"pacote":{"nome":"Pacote Legado"}}',
                          1000.00, 300.00, 30.00, 700.00,
                          now(), '[]', now(), now()
                        )
                        """.formatted(schema));
            }
        }

        Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("""
                    select cliente_nome, pacote_nome
                    from %s.contratos where id = 'ct-mig'
                    """.formatted(schema))) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("cliente_nome")).isEqualTo("Marina Legado");
                assertThat(result.getString("pacote_nome")).isEqualTo("Pacote Legado");
            }

            try (ResultSet result = statement.executeQuery("""
                    select count(*) from information_schema.tables
                    where table_schema = '%s' and table_name in
                      ('arquivos', 'galerias', 'midias_galeria')
                    """.formatted(schema))) {
                result.next();
                assertThat(result.getInt(1)).isEqualTo(3);
            }
        } finally {
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement statement = connection.createStatement()) {
                statement.execute("drop schema if exists %s cascade".formatted(schema));
            }
        }
    }
}
