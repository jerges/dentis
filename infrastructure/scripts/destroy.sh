#!/usr/bin/env bash
# 4. Destroy all AWS resources: EC2, VPC, SGs, EIP and cleanup ECR.
#    Usage: ./infrastructure/scripts/destroy.sh
#
#    Env vars:
#      DELETE_ECR=true            Also delete ECR repositories (takes precedence)
#      KEEP_LATEST_ECR_IMAGE=true Keep only latest image per ECR repo (default)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACK_DIR="$ROOT_DIR/infrastructure/terraform/aws/dev-ec2"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
EXPECTED_ACCOUNT_ID="${EXPECTED_ACCOUNT_ID:-742671448563}"
APP_NAME="${APP_NAME:-dentis}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
DELETE_ECR="${DELETE_ECR:-false}"
KEEP_LATEST_ECR_IMAGE="${KEEP_LATEST_ECR_IMAGE:-true}"

export AWS_PROFILE="$AWS_PROFILE_NAME"
export AWS_DEFAULT_PROFILE="$AWS_PROFILE_NAME"

# Bypass Zscaler corporate proxy for AWS traffic
unset HTTP_PROXY HTTPS_PROXY http_proxy https_proxy
export NO_PROXY="localhost,127.0.0.1,::1,.amazonaws.com,.aws.amazon.com"
export no_proxy="$NO_PROXY"

for cmd in aws terraform; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "[ERROR] Required command not found: $cmd" >&2; exit 1; }
done

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
[[ "$ACCOUNT_ID" == "$EXPECTED_ACCOUNT_ID" ]] || {
  echo "[ERROR] Wrong AWS account: $ACCOUNT_ID (expected $EXPECTED_ACCOUNT_ID)" >&2; exit 1
}

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║  DESTROY – AWS resources will be deleted ║"
echo "╚══════════════════════════════════════════╝"
echo ""
echo "  Stack : $STACK_DIR"
echo "  ECR   : DELETE_ECR=$DELETE_ECR"
echo "  ECR   : KEEP_LATEST_ECR_IMAGE=$KEEP_LATEST_ECR_IMAGE"
echo ""
read -rp "  Type 'yes' to confirm: " confirm
[[ "$confirm" == "yes" ]] || { echo "Aborted."; exit 0; }

# ── Terraform destroy ────────────────────────────────────────────────────────
if [[ -d "$STACK_DIR" ]]; then
  echo ""
  echo "[INFO] Running terraform destroy..."
  cd "$STACK_DIR"
  terraform init -input=false >/dev/null
  terraform destroy -auto-approve
  echo "[OK] Terraform resources destroyed."
else
  echo "[WARN] Terraform stack directory not found: $STACK_DIR"
fi

# ── ECR cleanup ───────────────────────────────────────────────────────────────
prune_repo_keep_latest() {
  local repo="$1"

  if ! aws ecr describe-repositories --region "$AWS_REGION" --repository-names "$repo" >/dev/null 2>&1; then
    echo "[INFO] ECR repo not found (skip): $repo"
    return
  fi

  local raw
  raw="$(aws ecr describe-images \
    --region "$AWS_REGION" \
    --repository-name "$repo" \
    --query 'sort_by(imageDetails,&imagePushedAt)[*].imageDigest' \
    --output text 2>/dev/null || true)"

  if [[ -z "$raw" || "$raw" == "None" ]]; then
    echo "[INFO] ECR repo empty (nothing to prune): $repo"
    return
  fi

  local -a digests=()
  local digest
  for digest in $raw; do
    [[ -n "$digest" ]] && digests+=("$digest")
  done

  if [[ ${#digests[@]} -le 1 ]]; then
    echo "[INFO] Repo $repo already has <=1 image."
    return
  fi

  local latest_digest="${digests[${#digests[@]}-1]}"
  local deleted=0
  for digest in "${digests[@]}"; do
    if [[ "$digest" != "$latest_digest" ]]; then
      aws ecr batch-delete-image \
        --region "$AWS_REGION" \
        --repository-name "$repo" \
        --image-ids imageDigest="$digest" >/dev/null || true
      ((deleted += 1))
    fi
  done

  echo "[OK] Pruned ECR repo $repo: deleted $deleted image(s), kept latest digest $latest_digest"
}

if [[ "$DELETE_ECR" == "true" ]]; then
  echo ""
  echo "[INFO] Deleting ECR repositories..."
  for repo in "${APP_NAME}-${ENVIRONMENT}-backend" "${APP_NAME}-${ENVIRONMENT}-web"; do
    if aws ecr describe-repositories --region "$AWS_REGION" --repository-names "$repo" >/dev/null 2>&1; then
      aws ecr delete-repository --region "$AWS_REGION" --repository-name "$repo" --force >/dev/null
      echo "[OK] Deleted ECR repo: $repo"
    else
      echo "[INFO] ECR repo not found (already deleted): $repo"
    fi
  done
elif [[ "$KEEP_LATEST_ECR_IMAGE" == "true" ]]; then
  echo ""
  echo "[INFO] Pruning ECR images (keeping only latest per repository)..."
  prune_repo_keep_latest "${APP_NAME}-${ENVIRONMENT}-backend"
  prune_repo_keep_latest "${APP_NAME}-${ENVIRONMENT}-web"
else
  echo ""
  echo "[INFO] Skipping ECR cleanup"
fi

# ── Cleanup local temp files ─────────────────────────────────────────────────
rm -f "$ROOT_DIR/.env.ec2"
echo ""
echo "✓ Cleanup complete."
echo "  Note: terraform.tfvars has been kept at $STACK_DIR/terraform.tfvars"
echo "  Delete it manually if you want a completely fresh start."

