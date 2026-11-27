#!/usr/bin/env bash
#
# Redefine a senha do admin do banco de desenvolvimento.
#
# Uso:
#   bash scripts/reset-dev-admin.sh
#   ADMIN_PASSWORD='outra-senha' bash scripts/reset-dev-admin.sh
#
# O script so atua no Postgres local do docker-compose.dev.yml. Nao use em
# producao. Pare o backend antes para evitar estado antigo em cache.
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

CONTAINER="${POSTGRES_CONTAINER:-fotografia-dev-postgres}"
DB="${POSTGRES_DB:-photoizer}"
USER="${POSTGRES_USER:-photoizer}"
EMAIL="${ADMIN_EMAIL:-admin@rannyamaral.com.br}"
SENHA="${ADMIN_PASSWORD:-admin12345}"

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "Container '$CONTAINER' nao esta rodando."
  echo "Suba com: docker compose -f docker-compose.dev.yml up -d"
  exit 1
fi

HTPASSWD="$(command -v htpasswd || true)"
if [[ -z "$HTPASSWD" ]]; then
  echo "htpasswd nao encontrado. No macOS ele fica em /usr/sbin/htpasswd." >&2
  exit 1
fi

HASH="$("$HTPASSWD" -bnBC 10 "" "$SENHA" | cut -d: -f2 | sed 's/^\$2y\$/\$2a\$/')"

docker exec -i "$CONTAINER" psql -U "$USER" -d "$DB" -v ON_ERROR_STOP=1 \
  -v hash="$HASH" -v email="$EMAIL" <<'SQL'
UPDATE usuarios
SET senha_hash = :'hash',
    deve_trocar_senha = false,
    senha_alterada_em = now(),
    tentativas_falhas = 0,
    bloqueado_ate = null,
    token_version = token_version + 1,
    lock_version = lock_version + 1,
    atualizado_em = now()
WHERE email = :'email';
SQL

echo "Senha de '$EMAIL' redefinida no banco '$DB'."
echo "Reinicie o backend e entre com a senha informada."
