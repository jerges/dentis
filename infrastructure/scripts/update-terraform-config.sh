#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACK_DIR="$ROOT_DIR/infrastructure/terraform/aws/dev-ec2"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
EXPECTED_ACCOUNT_ID="${EXPECTED_ACCOUNT_ID:-742671448563}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
APP_NAME="${APP_NAME:-dentis}"
# Set to false to only update tfvars without applying
AUTO_APPLY="${AUTO_APPLY:-true}"

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

echo "[INFO] Updating Terraform configuration with latest ECR images"
echo "[INFO] AWS Region: $AWS_REGION"
echo "[INFO] AWS Profile: $AWS_PROFILE_NAME"
echo "[INFO] Environment: $ENVIRONMENT"

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

BACKEND_REPO_URL="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${BACKEND_REPO_NAME}"
WEB_REPO_URL="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${WEB_REPO_NAME}"

# Get latest image digest from ECR
get_latest_image_tag() {
  local repo_url="$1"
  local repo_name="${repo_url##*/}"

  echo "[INFO] Fetching latest image from ECR: $repo_name"

  local latest_tag
  latest_tag=$(aws ecr describe-images \
    --region "$AWS_REGION" \
    --repository-name "$repo_name" \
    --filter tagStatus=TAGGED \
    --query 'sort_by(imageDetails, &imagePushedAt)[-1].imageTags[0]' \
    --output text 2>/dev/null || echo "latest")

  if [[ "$latest_tag" == "None" || -z "$latest_tag" ]]; then
    latest_tag="latest"
  fi

  echo "$latest_tag"
}

BACKEND_TAG=$(get_latest_image_tag "$BACKEND_REPO_URL")
WEB_TAG=$(get_latest_image_tag "$WEB_REPO_URL")

echo "[INFO] Latest backend image tag: $BACKEND_TAG"
echo "[INFO] Latest web image tag: $WEB_TAG"

if [[ ! -f "$STACK_DIR/terraform.tfvars" ]]; then
  echo "[ERROR] terraform.tfvars not found at $STACK_DIR"
  echo "[ERROR] Please run deploy-dev-ec2.sh first to initialize the stack"
  exit 1
fi

echo "[INFO] Reading current terraform.tfvars..."
# Read current values from terraform.tfvars
declare -A current_values
while IFS= read -r line; do
  if [[ "$line" =~ ^[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*= ]]; then
    key="${line%%[[:space:]]*=*}"
    value="${line#*= }"
    current_values["$key"]="$value"
  fi
done < "$STACK_DIR/terraform.tfvars"

# Update image tags in terraform.tfvars
echo "[INFO] Updating image tags in terraform.tfvars"
sed -i.bak "s/app_image_tag = .*/app_image_tag = \"$BACKEND_TAG\"/" "$STACK_DIR/terraform.tfvars"
sed -i.bak "s/web_image_tag = .*/web_image_tag = \"$WEB_TAG\"/" "$STACK_DIR/terraform.tfvars"
rm -f "$STACK_DIR/terraform.tfvars.bak"

echo "[INFO] ✓ Updated terraform.tfvars with latest image tags"
echo "[INFO] Backend image tag: $BACKEND_TAG"
echo "[INFO] Web image tag: $WEB_TAG"

# Apply terraform to redeploy EC2 with new images
if [[ "$AUTO_APPLY" == "true" ]]; then
  echo "[INFO] Applying Terraform changes..."
  cd "$STACK_DIR"
  terraform init -input=false >/dev/null
  terraform apply -auto-approve

  INSTANCE_IP="$(terraform output -raw public_ip 2>/dev/null || echo '')"
  if [[ -n "$INSTANCE_IP" ]]; then
    echo ""
    echo "[INFO] ✓ Deploy complete"
    echo "[INFO] Frontend URL: http://${INSTANCE_IP}"
    echo "[INFO] Backend URL:  http://${INSTANCE_IP}:8080"
    echo "[INFO] MailHog URL:  http://${INSTANCE_IP}:8025"
    echo "[INFO] SSH:          ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@${INSTANCE_IP}"
  fi
else
  echo "[INFO]"
  echo "[INFO] Run 'terraform apply -auto-approve' to update the EC2 instance:"
  echo "[INFO] cd $STACK_DIR && terraform apply -auto-approve"
fi

