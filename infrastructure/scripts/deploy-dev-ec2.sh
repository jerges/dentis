#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACK_DIR="$ROOT_DIR/infrastructure/terraform/aws/dev-ec2"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
EXPECTED_ACCOUNT_ID="${EXPECTED_ACCOUNT_ID:-742671448563}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
APP_NAME="${APP_NAME:-dentis}"
INSTANCE_TYPE="${INSTANCE_TYPE:-t3.medium}"
APP_IMAGE_TAG="${APP_IMAGE_TAG:-latest}"
WEB_IMAGE_TAG="${WEB_IMAGE_TAG:-latest}"
DOCKER_PLATFORMS="${DOCKER_PLATFORMS:-linux/amd64,linux/arm64}"
SSH_PRIVATE_KEY_PATH="${SSH_PRIVATE_KEY_PATH:-$HOME/.ssh/dentis-dev-ec2}"
SSH_PUBLIC_KEY_PATH="${SSH_PUBLIC_KEY_PATH:-$HOME/.ssh/dentis-dev-ec2.pub}"
APP_PORT="${APP_PORT:-8080}"
WEB_PORT="${WEB_PORT:-80}"
APP_ALLOWED_CIDR="${APP_ALLOWED_CIDR:-0.0.0.0/0}"
WEB_ALLOWED_CIDR="${WEB_ALLOWED_CIDR:-0.0.0.0/0}"

BACKEND_REPO_NAME="${BACKEND_REPO_NAME:-${APP_NAME}-${ENVIRONMENT}-backend}"
WEB_REPO_NAME="${WEB_REPO_NAME:-${APP_NAME}-${ENVIRONMENT}-web}"

export AWS_PROFILE="$AWS_PROFILE_NAME"
export AWS_DEFAULT_PROFILE="$AWS_PROFILE_NAME"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] Required command not found: $1" >&2
    exit 1
  fi
}

require_cmd aws
require_cmd terraform
require_cmd docker
require_cmd ssh
require_cmd curl

# Use defaults from application.yml
DB_PASSWORD="${DB_PASSWORD:-dentis}"
JWT_SECRET="${JWT_SECRET:-dentis-jwt-secret-key-minimum-256-bits-long-for-hs256}"

if [[ -z "${DB_PASSWORD}" || -z "${JWT_SECRET}" ]]; then
  echo "[ERROR] DB_PASSWORD and JWT_SECRET must not be empty." >&2
  exit 1
fi

echo "[INFO] Dev defaults enabled: running non-interactive."
echo "[INFO] Using defaults from application.yml"

MAIL_HOST="${MAIL_HOST:-mailhog}"
MAIL_PORT="${MAIL_PORT:-1025}"
MAIL_USERNAME="${MAIL_USERNAME:-}"
MAIL_PASSWORD="${MAIL_PASSWORD:-}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
DB_NAME="${DB_NAME:-dentis_db}"
DB_USER="${DB_USER:-dentis}"

if [[ ! -f "$SSH_PRIVATE_KEY_PATH" || ! -f "$SSH_PUBLIC_KEY_PATH" ]]; then
  echo "[INFO] SSH key pair not found. Generating: $SSH_PRIVATE_KEY_PATH"
  mkdir -p "$(dirname "$SSH_PRIVATE_KEY_PATH")"
  ssh-keygen -t ed25519 -C "${APP_NAME}-${ENVIRONMENT}-ec2" -f "$SSH_PRIVATE_KEY_PATH" -N ""
fi

chmod 600 "$SSH_PRIVATE_KEY_PATH"
chmod 644 "$SSH_PUBLIC_KEY_PATH"

PUBLIC_IP="$(curl -4 -s https://checkip.amazonaws.com | tr -d '[:space:]')"
if [[ -z "$PUBLIC_IP" ]]; then
  echo "[ERROR] Could not detect your public IP for SSH allowlist." >&2
  exit 1
fi
SSH_ALLOWED_CIDR="${SSH_ALLOWED_CIDR:-${PUBLIC_IP}/32}"

echo "[INFO] Using AWS region: $AWS_REGION"
echo "[INFO] Using AWS profile: $AWS_PROFILE_NAME"
echo "[INFO] Detected local public IP for SSH: $SSH_ALLOWED_CIDR"
echo "[INFO] Docker platforms for ECR images: $DOCKER_PLATFORMS"

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
if [[ -z "$ACCOUNT_ID" ]]; then
  echo "[ERROR] Could not get AWS account ID. Check aws credentials." >&2
  exit 1
fi
if [[ "$ACCOUNT_ID" != "$EXPECTED_ACCOUNT_ID" ]]; then
  echo "[ERROR] Wrong AWS account: $ACCOUNT_ID (expected $EXPECTED_ACCOUNT_ID)." >&2
  echo "[ERROR] Use AWS_PROFILE_NAME=jbello or export AWS_PROFILE=jbello." >&2
  exit 1
fi

create_ecr_repo_if_missing() {
  local repo_name="$1"
  if ! aws ecr describe-repositories --region "$AWS_REGION" --repository-names "$repo_name" >/dev/null 2>&1; then
    echo "[INFO] Creating ECR repository: $repo_name"
    aws ecr create-repository \
      --region "$AWS_REGION" \
      --repository-name "$repo_name" \
      --image-scanning-configuration scanOnPush=true \
      --image-tag-mutability MUTABLE >/dev/null
  fi
}

create_ecr_repo_if_missing "$BACKEND_REPO_NAME"
create_ecr_repo_if_missing "$WEB_REPO_NAME"

BACKEND_REPO_URL="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${BACKEND_REPO_NAME}"
WEB_REPO_URL="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${WEB_REPO_NAME}"
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

echo "[INFO] Logging Docker into ECR"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"

echo "[INFO] Building and pushing backend image: ${BACKEND_REPO_URL}:${APP_IMAGE_TAG}"
docker buildx build --platform "$DOCKER_PLATFORMS" -f "$ROOT_DIR/infrastructure/docker/Dockerfile" -t "${BACKEND_REPO_URL}:${APP_IMAGE_TAG}" --push "$ROOT_DIR"

echo "[INFO] Building and pushing web image: ${WEB_REPO_URL}:${WEB_IMAGE_TAG}"
docker buildx build --platform "$DOCKER_PLATFORMS" -f "$ROOT_DIR/infrastructure/docker/Dockerfile.web" -t "${WEB_REPO_URL}:${WEB_IMAGE_TAG}" --push "$ROOT_DIR"

mkdir -p "$STACK_DIR"

cat > "$STACK_DIR/terraform.tfvars" <<EOF
aws_region = "${AWS_REGION}"
environment = "${ENVIRONMENT}"
app_name = "${APP_NAME}"
instance_type = "${INSTANCE_TYPE}"

ssh_allowed_cidr = "${SSH_ALLOWED_CIDR}"
app_allowed_cidr = "${APP_ALLOWED_CIDR}"
web_allowed_cidr = "${WEB_ALLOWED_CIDR}"

create_key_pair = true
ssh_public_key_path = "${SSH_PUBLIC_KEY_PATH}"

assign_eip = false

ecr_repository_url = "${BACKEND_REPO_URL}"
app_image_tag = "${APP_IMAGE_TAG}"

web_ecr_repository_url = "${WEB_REPO_URL}"
web_image_tag = "${WEB_IMAGE_TAG}"

app_port = ${APP_PORT}
web_port = ${WEB_PORT}

db_name = "${DB_NAME}"
db_user = "${DB_USER}"
db_password = "${DB_PASSWORD}"

jwt_secret = "${JWT_SECRET}"

mail_host = "${MAIL_HOST}"
mail_port = ${MAIL_PORT}
mail_username = "${MAIL_USERNAME}"
mail_password = "${MAIL_PASSWORD}"

spring_profiles_active = "${SPRING_PROFILES_ACTIVE}"
EOF

echo "[INFO] Running Terraform apply"
cd "$STACK_DIR"
terraform init
terraform apply -auto-approve

INSTANCE_IP="$(terraform output -raw public_ip)"

echo "[INFO] Waiting for SSH on ${INSTANCE_IP}"
for _ in $(seq 1 30); do
  if ssh -i "$SSH_PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no -o ConnectTimeout=5 ec2-user@"$INSTANCE_IP" "echo ready" >/dev/null 2>&1; then
    break
  fi
  sleep 10
done

echo "[INFO] Forcing remote compose refresh"
ssh -i "$SSH_PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" bash <<EOF
set -euo pipefail
aws ecr get-login-password --region ${AWS_REGION} | sudo docker login --username AWS --password-stdin ${ECR_REGISTRY}
cd /opt/dentis
sudo docker compose pull
sudo docker compose up -d
EOF

echo
terraform output ssh_command
echo "Frontend URL: $(terraform output -raw web_url)"
echo "Backend URL:  $(terraform output -raw app_url)"
echo "MailHog URL:  $(terraform output -raw mailhog_url)"
