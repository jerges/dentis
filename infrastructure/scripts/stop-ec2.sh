#!/usr/bin/env bash
# Stop running docker compose services on EC2 without removing containers/volumes.
# Usage:
#   ./infrastructure/scripts/stop-ec2.sh
#   ./infrastructure/scripts/stop-ec2.sh web app
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACK_DIR="$ROOT_DIR/infrastructure/terraform/aws/dev-ec2"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
AWS_REGION="${AWS_REGION:-us-east-1}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/dentis-dev-ec2}"

export AWS_PROFILE="$AWS_PROFILE_NAME"
export AWS_DEFAULT_PROFILE="$AWS_PROFILE_NAME"

unset HTTP_PROXY HTTPS_PROXY ALL_PROXY NO_PROXY http_proxy https_proxy all_proxy no_proxy

for cmd in aws terraform ssh; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "[ERROR] Required command not found: $cmd" >&2; exit 1; }
done

[[ -f "$SSH_KEY" ]] || { echo "[ERROR] SSH key not found: $SSH_KEY" >&2; exit 1; }

INSTANCE_IP="$(cd "$STACK_DIR" && terraform output -raw public_ip 2>/dev/null || true)"
[[ -n "$INSTANCE_IP" ]] || { echo "[ERROR] No EC2 public_ip in Terraform output." >&2; exit 1; }

SERVICES=("$@")

if [[ ${#SERVICES[@]} -eq 0 ]]; then
  REMOTE_CMD='cd /opt/dentis && sudo docker compose stop'
  echo "[INFO] Stopping all compose services on EC2 ($INSTANCE_IP)..."
else
  REMOTE_CMD="cd /opt/dentis && sudo docker compose stop ${SERVICES[*]}"
  echo "[INFO] Stopping services on EC2 ($INSTANCE_IP): ${SERVICES[*]}"
fi

ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" "$REMOTE_CMD"

echo "[OK] Services stopped (containers preserved)."

