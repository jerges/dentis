#!/usr/bin/env bash
# Updates the SSH ingress rule on the dentis-dev Security Group with your current public IP.
# Run this whenever your local IP changes and SSH stops working.
set -euo pipefail

AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
EXPECTED_ACCOUNT_ID="${EXPECTED_ACCOUNT_ID:-742671448563}"
SG_NAME="${SG_NAME:-dentis-dev-ec2-sg}"

export AWS_PROFILE="$AWS_PROFILE_NAME"
export AWS_DEFAULT_PROFILE="$AWS_PROFILE_NAME"

MY_IP="$(curl -4 -s https://checkip.amazonaws.com | tr -d '[:space:]')"
echo "[INFO] Your current public IP: ${MY_IP}"

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
if [[ -z "$ACCOUNT_ID" ]]; then
  echo "[ERROR] Could not get AWS account ID." >&2
  exit 1
fi
if [[ "$ACCOUNT_ID" != "$EXPECTED_ACCOUNT_ID" ]]; then
  echo "[ERROR] Wrong AWS account: $ACCOUNT_ID (expected $EXPECTED_ACCOUNT_ID)." >&2
  echo "[ERROR] Use AWS_PROFILE_NAME=jbello or export AWS_PROFILE=jbello." >&2
  exit 1
fi

SG_ID="$(aws ec2 describe-security-groups \
  --region "$AWS_REGION" \
  --filters "Name=group-name,Values=${SG_NAME}" \
  --query 'SecurityGroups[0].GroupId' \
  --output text)"

if [[ -z "$SG_ID" || "$SG_ID" == "None" ]]; then
  echo "[ERROR] Security group '${SG_NAME}' not found. Is the stack deployed?" >&2
  exit 1
fi

echo "[INFO] Security Group: $SG_ID"

# Remove all existing SSH (port 22) ingress rules
OLD_CIDRS=$(aws ec2 describe-security-groups \
  --region "$AWS_REGION" \
  --group-ids "$SG_ID" \
  --query 'SecurityGroups[0].IpPermissions[?FromPort==`22`].IpRanges[*].CidrIp' \
  --output text 2>/dev/null || true)

if [[ -n "$OLD_CIDRS" ]]; then
  for cidr in $OLD_CIDRS; do
    echo "[INFO] Removing old SSH rule: $cidr"
    aws ec2 revoke-security-group-ingress \
      --region "$AWS_REGION" \
      --group-id "$SG_ID" \
      --protocol tcp --port 22 \
      --cidr "$cidr" --no-cli-pager 2>/dev/null || true
  done
fi

# Add current IP
aws ec2 authorize-security-group-ingress \
  --region "$AWS_REGION" \
  --group-id "$SG_ID" \
  --protocol tcp --port 22 \
  --cidr "${MY_IP}/32" --no-cli-pager >/dev/null

echo "[INFO] ✓ SSH access updated to ${MY_IP}/32"

# Also update terraform.tfvars to keep it in sync
STACK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../terraform/aws/dev-ec2" && pwd)"
if [[ -f "$STACK_DIR/terraform.tfvars" ]]; then
  sed -i.bak "s|ssh_allowed_cidr = .*|ssh_allowed_cidr = \"${MY_IP}/32\"|" "$STACK_DIR/terraform.tfvars"
  rm -f "$STACK_DIR/terraform.tfvars.bak"
  echo "[INFO] ✓ terraform.tfvars updated with new SSH CIDR"
fi

# Show connection info
INSTANCE_IP=$(cd "$STACK_DIR" && terraform output -raw public_ip 2>/dev/null || true)
if [[ -n "$INSTANCE_IP" ]]; then
  echo ""
  echo "[INFO] Connect with:"
  echo "       ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@${INSTANCE_IP}"
fi

