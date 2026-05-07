#!/usr/bin/env bash
# 1. Build backend Docker image, push to ECR and restart app on EC2.
#    If no EC2 exists yet, just pushes the image to ECR.
#    Usage: ./infrastructure/scripts/build-backend.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACK_DIR="$ROOT_DIR/infrastructure/terraform/aws/dev-ec2"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
EXPECTED_ACCOUNT_ID="${EXPECTED_ACCOUNT_ID:-742671448563}"
APP_NAME="${APP_NAME:-dentis}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
APP_IMAGE_TAG="${APP_IMAGE_TAG:-latest}"
DOCKER_PLATFORMS="${DOCKER_PLATFORMS:-linux/amd64,linux/arm64}"
SSH_PRIVATE_KEY_PATH="${SSH_PRIVATE_KEY_PATH:-$HOME/.ssh/dentis-dev-ec2}"

export AWS_PROFILE="$AWS_PROFILE_NAME"
export AWS_DEFAULT_PROFILE="$AWS_PROFILE_NAME"

# Ensure AWS/Docker commands do not inherit shell proxy settings.
unset HTTP_PROXY HTTPS_PROXY ALL_PROXY NO_PROXY http_proxy https_proxy all_proxy no_proxy


for cmd in aws docker ssh terraform; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "[ERROR] Required command not found: $cmd" >&2; exit 1; }
done

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
[[ "$ACCOUNT_ID" == "$EXPECTED_ACCOUNT_ID" ]] || {
  echo "[ERROR] Wrong AWS account: $ACCOUNT_ID (expected $EXPECTED_ACCOUNT_ID)" >&2; exit 1
}

REPO_NAME="${APP_NAME}-${ENVIRONMENT}-backend"
REPO_URL="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}"
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

# Create ECR repo if missing
aws ecr describe-repositories --region "$AWS_REGION" --repository-names "$REPO_NAME" >/dev/null 2>&1 || {
  echo "[INFO] Creating ECR repository: $REPO_NAME"
  aws ecr create-repository --region "$AWS_REGION" --repository-name "$REPO_NAME" \
    --image-scanning-configuration scanOnPush=true >/dev/null
}

echo "[INFO] Logging into ECR..."
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"

echo "[INFO] Building backend image: ${REPO_URL}:${APP_IMAGE_TAG}"
docker buildx build --platform "$DOCKER_PLATFORMS" \
  -f "$ROOT_DIR/infrastructure/docker/Dockerfile" \
  -t "${REPO_URL}:${APP_IMAGE_TAG}" --push "$ROOT_DIR"
echo "[OK] Backend image pushed to ECR"

# Get EC2 IP from Terraform state (if EC2 exists)
INSTANCE_IP=""
if [[ -d "$STACK_DIR" ]]; then
  INSTANCE_IP="$(cd "$STACK_DIR" && terraform output -raw public_ip 2>/dev/null || echo '')"
fi

if [[ -z "$INSTANCE_IP" ]]; then
  echo "[WARN] No EC2 found in Terraform state. Image pushed to ECR."
  echo "       Run ./infrastructure/scripts/deploy.sh to provision and deploy."
  exit 0
fi

echo "[INFO] Restarting backend on EC2 ($INSTANCE_IP)..."
ssh -i "$SSH_PRIVATE_KEY_PATH" -o StrictHostKeyChecking=no -o ConnectTimeout=10 ec2-user@"$INSTANCE_IP" bash <<'ENDSSH'
set -euo pipefail
IMDS_TOKEN=$(curl -s -X PUT -H "X-aws-ec2-metadata-token-ttl-seconds: 60" http://169.254.169.254/latest/api/token)
REGION=$(curl -s -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" http://169.254.169.254/latest/meta-data/placement/region)
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
aws ecr get-login-password --region "$REGION" | sudo docker login --username AWS --password-stdin "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com" >/dev/null
cd /opt/dentis
sudo docker compose pull app
sudo docker compose up -d app
ENDSSH

echo "[OK] Backend restarted."
echo "     Health: $(curl -s -o /dev/null -w '%{http_code}' "http://$INSTANCE_IP:8080/actuator/health" 2>/dev/null || echo 'unreachable')"
echo "     URL: http://$INSTANCE_IP:8080"

