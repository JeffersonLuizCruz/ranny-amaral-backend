#!/usr/bin/env bash
#
# Limpa todos os dados do banco de desenvolvimento, preservando schema e o
# histórico do Flyway. Use apenas no ambiente local.
#
# Uso:
#   bash scripts/reset-dev-db.sh
#
# Depois, para subir sem os dados de demonstração:
#   APP_SEED_ENABLED=false ./mvnw spring-boot:run
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

CONTAINER="${POSTGRES_CONTAINER:-fotografia-dev-postgres}"
DB="${POSTGRES_DB:-photoizer}"
USER="${POSTGRES_USER:-photoizer}"

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "Container '$CONTAINER' não está rodando."
  echo "Suba com: docker compose -f docker-compose.dev.yml up -d"
  exit 1
fi

docker compose -f docker-compose.dev.yml exec -T postgres \
  psql -U "$USER" -d "$DB" -v ON_ERROR_STOP=1 <<'SQL'
DO $$
DECLARE
  tabela record;
BEGIN
  FOR tabela IN
    SELECT tablename
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename <> 'flyway_schema_history'
  LOOP
    EXECUTE format('TRUNCATE TABLE %I RESTART IDENTITY CASCADE', tabela.tablename);
  END LOOP;
END $$;
SQL

echo "Banco '$DB' limpo. Schema e flyway_schema_history preservados."
echo "Para subir sem seed de demonstração: APP_SEED_ENABLED=false ./mvnw spring-boot:run"
