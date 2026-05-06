#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
EXPECTED_ACCOUNT_ID="${EXPECTED_ACCOUNT_ID:-742671448563}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
APP_NAME="${APP_NAME:-dentis}"
APP_IMAGE_TAG="${APP_IMAGE_TAG:-latest}"
DOCKER_PLATFORMS="${DOCKER_PLATFORMS:-linux/amd64,linux/arm64}"

BACKEND_REPO_NAME="${BACKEND_REPO_NAME:-${APP_NAME}-${ENVIRONMENT}-backend}"

export AWS_PROFILE="$AWS_PROFILE_NAME"
export AWS_DEFAULT_PROFILE="$AWS_PROFILE_NAME"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] Required command not found: $1" >&2
    exit 1
  fi
}

require_cmd aws
require_cmd docker

echo "[INFO] Building backend image"
echo "[INFO] AWS Region: $AWS_REGION"
echo "[INFO] AWS Profile: $AWS_PROFILE_NAME"
echo "[INFO] Environment: $ENVIRONMENT"
echo "[INFO] Image Tag: $APP_IMAGE_TAG"
echo "[INFO] Docker platforms: $DOCKER_PLATFORMS"

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
  else
    echo "[INFO] ECR repository already exists: $repo_name"
  fi
}

create_ecr_repo_if_missing "$BACKEND_REPO_NAME"

BACKEND_REPO_URL="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${BACKEND_REPO_NAME}"
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

echo "[INFO] Logging Docker into ECR"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY" >/dev/null 2>&1

echo "[INFO] Building and pushing backend image: ${BACKEND_REPO_URL}:${APP_IMAGE_TAG}"
docker buildx build \
  --platform "$DOCKER_PLATFORMS" \
  -f "$ROOT_DIR/infrastructure/docker/Dockerfile" \
  -t "${BACKEND_REPO_URL}:${APP_IMAGE_TAG}" \
  --push \
  "$ROOT_DIR"


echo "[INFO] ✓ Backend image built and pushed successfully"
echo "[INFO] Image URL: ${BACKEND_REPO_URL}:${APP_IMAGE_TAG}"
