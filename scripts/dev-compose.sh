#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$ROOT_DIR/docker-compose.dev.yml}"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env.local}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] Required command not found: $1" >&2
    exit 1
  fi
}

require_cmd docker

registry_from_env() {
  local image_ref="$1"
  printf '%s' "$image_ref" | cut -d'/' -f1
}

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "[ERROR] Compose file not found: $COMPOSE_FILE" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "[ERROR] Env file not found: $ENV_FILE" >&2
  echo "[INFO] Create it from your local settings or export ENV_FILE=/path/to/file" >&2
  exit 1
fi

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

usage() {
  cat <<'EOF'
Usage: scripts/dev-compose.sh <command> [args]

Commands:
  ecr-login     Log Docker into the ECR registry used by .env.local
  up            Pull missing images and start the local stack
  down          Stop and remove containers
  restart       Restart the local stack
  pull          Pull app and web images from the registry
  ps            Show container status
  logs [svc]    Follow logs for all services or one service
  config        Render the resolved compose configuration
  reset-db      Recreate the stack and reset PostgreSQL volume
  help          Show this help
EOF
}

command="${1:-help}"
shift || true

case "$command" in
  up)
    compose up -d
    ;;
  ecr-login)
    require_cmd aws
    web_image="$(grep '^WEB_IMAGE=' "$ENV_FILE" | cut -d'=' -f2- || true)"
    app_image="$(grep '^APP_IMAGE=' "$ENV_FILE" | cut -d'=' -f2- || true)"
    registry="$(registry_from_env "${web_image:-$app_image}")"
    if [[ -z "$registry" || "$registry" != *.amazonaws.com ]]; then
      echo "[ERROR] Could not infer a valid ECR registry from $ENV_FILE" >&2
      exit 1
    fi
    region="${AWS_REGION:-us-east-1}"
    aws ecr get-login-password --region "$region" | docker login --username AWS --password-stdin "$registry"
    ;;
  down)
    compose down
    ;;
  restart)
    compose down
    compose up -d
    ;;
  pull)
    compose pull web app
    ;;
  ps)
    compose ps
    ;;
  logs)
    if [[ $# -gt 0 ]]; then
      compose logs -f "$1"
    else
      compose logs -f
    fi
    ;;
  config)
    compose config
    ;;
  reset-db)
    compose down -v
    compose up -d postgres mailhog
    compose run --rm liquibase
    compose up -d app web
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    echo "[ERROR] Unknown command: $command" >&2
    usage
    exit 1
    ;;
esac

