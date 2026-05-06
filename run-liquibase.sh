#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./run-liquibase.sh                 # apply pending changesets (update)
#   ./run-liquibase.sh update          # same as default
#   ./run-liquibase.sh status          # show pending changesets
#   ./run-liquibase.sh history         # show executed changesets
#   ./run-liquibase.sh validate        # validate changelog
#
# Optional environment variables:
#   DB_NAME, DB_USER, DB_PASSWORD, DB_PORT

ACTION="${1:-update}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

case "$ACTION" in
  update|status|history|validate)
    ;;
  *)
    echo "Unsupported action: $ACTION"
    echo "Allowed actions: update | status | history | validate"
    exit 1
    ;;
esac

echo "[liquibase] Ensuring postgres is running..."
(
  cd "$ROOT_DIR"
  docker compose --profile dev up -d postgres
)

echo "[liquibase] Running action: $ACTION"
(
  cd "$ROOT_DIR"
  docker compose --profile dev run --rm liquibase \
    --search-path=/liquibase/changelog \
    --changelog-file=db/changelog/db.changelog-master.yaml \
    --url="jdbc:postgresql://postgres:5432/${DB_NAME:-dentis_db}" \
    --username="${DB_USER:-dentis}" \
    --password="${DB_PASSWORD:-dentis}" \
    "$ACTION"
)

echo "[liquibase] Done."

