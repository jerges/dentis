#!/usr/bin/env bash
# Provision EC2 with Terraform and deploy docker-compose using existing ECR images.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACK_DIR="$ROOT_DIR/infrastructure/terraform/aws/dev-ec2"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
EXPECTED_ACCOUNT_ID="${EXPECTED_ACCOUNT_ID:-742671448563}"
AUTO_APPLY="${AUTO_APPLY:-true}"
RESET_DB_VOLUME="${RESET_DB_VOLUME:-false}"
RESET_ALL_VOLUMES="${RESET_ALL_VOLUMES:-false}"

export AWS_PROFILE="$AWS_PROFILE_NAME"
export AWS_DEFAULT_PROFILE="$AWS_PROFILE_NAME"

# Optional explicit SSH key path override
SSH_KEY="${SSH_KEY:-${SSH_PRIVATE_KEY_PATH:-}}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] Required command not found: $1" >&2
    exit 1
  fi
}

require_cmd terraform
require_cmd ssh
require_cmd scp
require_cmd aws

CURRENT_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
if [[ -z "$CURRENT_ACCOUNT_ID" ]]; then
  echo "[ERROR] Could not get AWS account ID. Check credentials/profile." >&2
  exit 1
fi
if [[ "$CURRENT_ACCOUNT_ID" != "$EXPECTED_ACCOUNT_ID" ]]; then
  echo "[ERROR] Wrong AWS account: $CURRENT_ACCOUNT_ID (expected $EXPECTED_ACCOUNT_ID)." >&2
  echo "[ERROR] Use AWS_PROFILE_NAME=jbello or export AWS_PROFILE=jbello." >&2
  exit 1
fi

resolve_from_tfvars() {
  local key="$1"
  local value
  value=$(sed -n "s/^${key}[[:space:]]*=[[:space:]]*\"\(.*\)\"[[:space:]]*$/\1/p" "$STACK_DIR/terraform.tfvars" | tail -n 1)
  echo "$value"
}

resolve_ssh_key() {
  if [[ -n "$SSH_KEY" && -f "$SSH_KEY" ]]; then
    echo "$SSH_KEY"
    return
  fi

  local tf_pub
  tf_pub="$(resolve_from_tfvars ssh_public_key_path || true)"
  if [[ -n "$tf_pub" ]]; then
    local pub_expanded="${tf_pub/#\~/$HOME}"
    local inferred_priv="${pub_expanded%.pub}"
    if [[ -f "$inferred_priv" ]]; then
      echo "$inferred_priv"
      return
    fi
  fi

  # Fallbacks commonly used in this repo/history
  for candidate in "$HOME/.ssh/dentis-dev-ec2" "$HOME/.ssh/dentis-key" "$HOME/.ssh/mac-jb-key"; do
    if [[ -f "$candidate" ]]; then
      echo "$candidate"
      return
    fi
  done

  echo ""
}

if [[ ! -f "$STACK_DIR/terraform.tfvars" ]]; then
  echo "[ERROR] Missing $STACK_DIR/terraform.tfvars" >&2
  exit 1
fi

APP_REPO="$(resolve_from_tfvars ecr_repository_url)"
APP_TAG="$(resolve_from_tfvars app_image_tag)"
WEB_REPO="$(resolve_from_tfvars web_ecr_repository_url)"
WEB_TAG="$(resolve_from_tfvars web_image_tag)"

if [[ -z "$APP_REPO" || -z "$WEB_REPO" ]]; then
  echo "[ERROR] Missing ECR repository URLs in terraform.tfvars" >&2
  exit 1
fi

if [[ -z "$APP_TAG" ]]; then APP_TAG="latest"; fi
if [[ -z "$WEB_TAG" ]]; then WEB_TAG="latest"; fi

APP_IMAGE="${APP_IMAGE:-${APP_REPO}:${APP_TAG}}"
WEB_IMAGE="${WEB_IMAGE:-${WEB_REPO}:${WEB_TAG}}"

echo "[INFO] Terraform stack: $STACK_DIR"
echo "[INFO] AWS profile: $AWS_PROFILE_NAME"
echo "[INFO] AWS account: $CURRENT_ACCOUNT_ID"
echo "[INFO] RESET_DB_VOLUME: $RESET_DB_VOLUME"
echo "[INFO] RESET_ALL_VOLUMES: $RESET_ALL_VOLUMES"
echo "[INFO] APP image: $APP_IMAGE"
echo "[INFO] WEB image: $WEB_IMAGE"

cd "$STACK_DIR"
terraform init -input=false >/dev/null

if [[ "$AUTO_APPLY" == "true" ]]; then
  echo "[INFO] Provisioning EC2 with terraform apply..."
  terraform apply -auto-approve
else
  echo "[INFO] AUTO_APPLY=false, skipping terraform apply"
fi

INSTANCE_IP="$(terraform output -raw public_ip 2>/dev/null || echo '')"
if [[ -z "$INSTANCE_IP" ]]; then
  echo "[ERROR] No EC2 public_ip in Terraform output." >&2
  exit 1
fi

RESOLVED_SSH_KEY="$(resolve_ssh_key)"
if [[ -z "$RESOLVED_SSH_KEY" ]]; then
  echo "[ERROR] Could not resolve a local private SSH key. Set SSH_KEY=/path/to/key" >&2
  exit 1
fi

chmod 600 "$RESOLVED_SSH_KEY" || true

echo "[INFO] EC2 IP: $INSTANCE_IP"
echo "[INFO] SSH key: $RESOLVED_SSH_KEY"

cat > "$ROOT_DIR/.env.ec2" <<EOF
WEB_IMAGE=$WEB_IMAGE
APP_IMAGE=$APP_IMAGE
DB_NAME=${DB_NAME:-dentis_db}
DB_USER=${DB_USER:-dentis}
DB_PASSWORD=${DB_PASSWORD:-dentis}
JWT_SECRET=${JWT_SECRET:-dentis-jwt-secret-key-minimum-256-bits-long-for-hs256}
MAIL_HOST=${MAIL_HOST:-mailhog}
MAIL_PORT=${MAIL_PORT:-1025}
MAIL_USERNAME=${MAIL_USERNAME:-}
MAIL_PASSWORD=${MAIL_PASSWORD:-}
EOF

echo "[INFO] Waiting for SSH..."
for _ in $(seq 1 30); do
  if ssh -i "$RESOLVED_SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=5 ec2-user@"$INSTANCE_IP" "echo ready" >/dev/null 2>&1; then
    break
  fi
  sleep 5
done

echo "[INFO] Copying compose and env to EC2..."
scp -i "$RESOLVED_SSH_KEY" -o StrictHostKeyChecking=no "$ROOT_DIR/docker-compose.dev.yml" ec2-user@"$INSTANCE_IP":/tmp/docker-compose.yml >/dev/null
scp -i "$RESOLVED_SSH_KEY" -o StrictHostKeyChecking=no "$ROOT_DIR/.env.ec2" ec2-user@"$INSTANCE_IP":/tmp/.env >/dev/null

echo "[INFO] Deploying containers on EC2..."
ssh -i "$RESOLVED_SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" bash <<ENDSSH
set -euo pipefail

RESET_DB_VOLUME="$RESET_DB_VOLUME"
RESET_ALL_VOLUMES="$RESET_ALL_VOLUMES"

sudo mkdir -p /opt/dentis
sudo mv /tmp/docker-compose.yml /opt/dentis/docker-compose.yml
sudo mv /tmp/.env /opt/dentis/.env
sudo chown -R ec2-user:ec2-user /opt/dentis

cd /opt/dentis

REGION=
REGION=$(curl -s http://169.254.169.254/latest/meta-data/placement/region)
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
aws ecr get-login-password --region "$REGION" | sudo docker login --username AWS --password-stdin "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com" >/dev/null

if [[ "$RESET_ALL_VOLUMES" == "true" ]]; then
  echo "[INFO] RESET_ALL_VOLUMES=true -> cleaning ALL compose volumes..."
  sudo docker compose down -v || true
elif [[ "$RESET_DB_VOLUME" == "true" ]]; then
  echo "[INFO] RESET_DB_VOLUME=true -> cleaning postgres volume..."
  sudo docker compose down -v || true
  sudo docker volume rm dentis-pgdata 2>/dev/null || true
else
  sudo docker compose down 2>/dev/null || true
fi

sudo docker compose pull
sudo docker compose up -d

echo "[INFO] Remote deployment complete"
ENDSSH

echo ""
echo "[INFO] Services available:"
echo "  Frontend: http://$INSTANCE_IP"
echo "  Backend:  http://$INSTANCE_IP:8080"
echo "  MailHog:  http://$INSTANCE_IP:8025"
echo "  SSH:      ssh -i $RESOLVED_SSH_KEY ec2-user@$INSTANCE_IP"
