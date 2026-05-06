#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACK_DIR="$ROOT_DIR/infrastructure/terraform/aws/dev-ec2"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
EXPECTED_ACCOUNT_ID="${EXPECTED_ACCOUNT_ID:-742671448563}"
AUTO_APPROVE="${AUTO_APPROVE:-true}"
PLAN_ONLY="${PLAN_ONLY:-false}"

export AWS_PROFILE="$AWS_PROFILE_NAME"
export AWS_DEFAULT_PROFILE="$AWS_PROFILE_NAME"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] Required command not found: $1" >&2
    exit 1
  fi
}

usage() {
  cat <<'EOF'
Destroy Dentis dev EC2 stack.

Usage:
  ./infrastructure/scripts/destroy-dev-ec2.sh

Optional environment variables:
  AWS_REGION=us-east-1
  AWS_PROFILE_NAME=jbello
  AUTO_APPROVE=true|false   (default: true)
  PLAN_ONLY=true|false      (default: false)

Examples:
  ./infrastructure/scripts/destroy-dev-ec2.sh
  AUTO_APPROVE=false ./infrastructure/scripts/destroy-dev-ec2.sh
  PLAN_ONLY=true ./infrastructure/scripts/destroy-dev-ec2.sh
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_cmd aws
require_cmd terraform

if [[ ! -d "$STACK_DIR" ]]; then
  echo "[ERROR] Terraform stack folder not found: $STACK_DIR" >&2
  exit 1
fi

echo "[INFO] Destroying dev-ec2 Terraform stack"
echo "[INFO] AWS Region: $AWS_REGION"
echo "[INFO] AWS Profile: $AWS_PROFILE_NAME"
echo "[INFO] Stack dir: $STACK_DIR"

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
if [[ -z "$ACCOUNT_ID" ]]; then
  echo "[ERROR] Could not get AWS account ID. Check AWS credentials/profile." >&2
  exit 1
fi
if [[ "$ACCOUNT_ID" != "$EXPECTED_ACCOUNT_ID" ]]; then
  echo "[ERROR] Wrong AWS account: $ACCOUNT_ID (expected $EXPECTED_ACCOUNT_ID)." >&2
  echo "[ERROR] Use AWS_PROFILE_NAME=jbello or export AWS_PROFILE=jbello." >&2
  exit 1
fi

echo "[INFO] AWS Account: $ACCOUNT_ID"

cd "$STACK_DIR"
terraform init -input=false >/dev/null

if [[ "$PLAN_ONLY" == "true" ]]; then
  echo "[INFO] Running destroy plan only"
  terraform plan -destroy
  exit 0
fi

if [[ "$AUTO_APPROVE" == "true" ]]; then
  terraform destroy -auto-approve
else
  terraform destroy
fi

# Release any orphaned Elastic IPs tagged as dentis-dev
echo "[INFO] Checking for orphaned Elastic IPs..."
ORPHAN_EIPS=$(aws ec2 describe-addresses --region "$AWS_REGION" --output json \
  | python3 -c "
import json,sys
d=json.load(sys.stdin)
for a in d['Addresses']:
  tags={t['Key']:t['Value'] for t in a.get('Tags',[])}
  if not a.get('InstanceId') and tags.get('Project')=='dentis':
    print(a['AllocationId'])
" 2>/dev/null || true)

if [[ -n "$ORPHAN_EIPS" ]]; then
  while IFS= read -r alloc_id; do
    echo "[INFO] Releasing orphaned EIP: $alloc_id"
    aws ec2 release-address --region "$AWS_REGION" --allocation-id "$alloc_id"
  done <<< "$ORPHAN_EIPS"
else
  echo "[INFO] No orphaned Elastic IPs found"
fi

echo "[INFO] Destroy finished"

