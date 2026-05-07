#!/usr/bin/env bash
# 3. Deploy both images to EC2 (no build). Provisions EC2 with Terraform if needed,
#    copies compose files, launches docker-compose and verifies the deployment.
#    Usage: ./infrastructure/scripts/deploy.sh
#
#    Env vars (all optional):
#      RESET_DB_VOLUME=true     Wipe and recreate the postgres volume
#      RESET_ALL_VOLUMES=true   Wipe all compose volumes
#      AUTO_APPLY=false         Skip terraform apply (just redeploy compose)
#      VERIFY_DEPLOYMENT=false  Skip post-deploy health checks
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STACK_DIR="$ROOT_DIR/infrastructure/terraform/aws/dev-ec2"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE_NAME="${AWS_PROFILE_NAME:-jbello}"
EXPECTED_ACCOUNT_ID="${EXPECTED_ACCOUNT_ID:-742671448563}"
APP_NAME="${APP_NAME:-dentis}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
AUTO_APPLY="${AUTO_APPLY:-true}"
RESET_DB_VOLUME="${RESET_DB_VOLUME:-false}"
RESET_ALL_VOLUMES="${RESET_ALL_VOLUMES:-false}"
VERIFY_DEPLOYMENT="${VERIFY_DEPLOYMENT:-true}"

export AWS_PROFILE="$AWS_PROFILE_NAME"
export AWS_DEFAULT_PROFILE="$AWS_PROFILE_NAME"

# ── Bypass corporate proxy (Zscaler) for AWS/EC2 traffic ─────────────────────
# Zscaler injects HTTP_PROXY into every terminal session; when disconnected
# from the corporate VPN the proxy host is unreachable and all AWS calls fail.
unset HTTP_PROXY HTTPS_PROXY http_proxy https_proxy
export NO_PROXY="localhost,127.0.0.1,::1,.amazonaws.com,.aws.amazon.com"
export no_proxy="$NO_PROXY"

for cmd in aws terraform ssh scp curl; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "[ERROR] Required command not found: $cmd" >&2; exit 1; }
done

# ── AWS account check ────────────────────────────────────────────────────────
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
[[ -n "$ACCOUNT_ID" ]] || { echo "[ERROR] Could not get AWS account ID." >&2; exit 1; }
[[ "$ACCOUNT_ID" == "$EXPECTED_ACCOUNT_ID" ]] || {
  echo "[ERROR] Wrong AWS account: $ACCOUNT_ID (expected $EXPECTED_ACCOUNT_ID)" >&2; exit 1
}

# ── terraform.tfvars: generate on first run ──────────────────────────────────
if [[ ! -f "$STACK_DIR/terraform.tfvars" ]]; then
  echo ""
  echo "First run detected – generating terraform.tfvars"
  echo "--------------------------------------------------"

  MY_IP="$(curl -4 -s https://checkip.amazonaws.com 2>/dev/null | tr -d '[:space:]')"
  [[ -n "$MY_IP" ]] && echo "[INFO] Your public IP: $MY_IP" || MY_IP="0.0.0.0"

  BACKEND_REPO="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${APP_NAME}-${ENVIRONMENT}-backend"
  WEB_REPO="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${APP_NAME}-${ENVIRONMENT}-web"

  echo "[ACTION] ECR repository URLs (press Enter to accept defaults):"
  read -rp "  Backend [$BACKEND_REPO]: " input_backend
  read -rp "  Web     [$WEB_REPO]: "    input_web
  [[ -n "$input_backend" ]] && BACKEND_REPO="$input_backend"
  [[ -n "$input_web" ]]     && WEB_REPO="$input_web"

  # Generate SSH key if missing
  SSH_KEY_PATH="$HOME/.ssh/dentis-dev-ec2"
  if [[ ! -f "$SSH_KEY_PATH" ]]; then
    echo "[INFO] Generating SSH key pair: $SSH_KEY_PATH"
    ssh-keygen -t ed25519 -C "dentis-dev-ec2" -f "$SSH_KEY_PATH" -N ""
  fi

  cat > "$STACK_DIR/terraform.tfvars" <<TFVARS
aws_region             = "$AWS_REGION"
environment            = "$ENVIRONMENT"
app_name               = "$APP_NAME"
instance_type          = "t2.small"
root_volume_size       = 30

ssh_allowed_cidr       = "$MY_IP/32"
app_allowed_cidr       = "0.0.0.0/0"
web_allowed_cidr       = "0.0.0.0/0"

create_key_pair        = true
ssh_public_key_path    = "~/.ssh/dentis-dev-ec2.pub"
assign_eip             = true

ecr_repository_url     = "$BACKEND_REPO"
app_image_tag          = "latest"
web_ecr_repository_url = "$WEB_REPO"
web_image_tag          = "latest"

app_port               = 8080
web_port               = 80

db_name                = "dentis_db"
db_user                = "dentis"
db_password            = "dentis"
jwt_secret             = "dentis-jwt-secret-key-minimum-256-bits-long-for-hs256"

mail_host              = "mailhog"
mail_port              = 1025
mail_username          = ""
mail_password          = ""

spring_profiles_active = "dev"
TFVARS
  echo "[OK] terraform.tfvars created."
fi

# ── Read values from tfvars ──────────────────────────────────────────────────
read_var() {
  sed -nE "s/^$1[[:space:]]*=[[:space:]]*\"?([^\"]*)\"?[[:space:]]*$/\1/p" \
    "$STACK_DIR/terraform.tfvars" | tail -1
}

APP_REPO="$(read_var ecr_repository_url)"
APP_TAG="$(read_var app_image_tag)";   [[ -n "$APP_TAG" ]]  || APP_TAG="latest"
WEB_REPO="$(read_var web_ecr_repository_url)"
WEB_TAG="$(read_var web_image_tag)";   [[ -n "$WEB_TAG" ]]  || WEB_TAG="latest"
APP_PORT="$(read_var app_port)";       [[ -n "$APP_PORT" ]] || APP_PORT="8080"
WEB_PORT="$(read_var web_port)";       [[ -n "$WEB_PORT" ]] || WEB_PORT="80"

[[ -n "$APP_REPO" && -n "$WEB_REPO" ]] || {
  echo "[ERROR] ECR repository URLs missing in terraform.tfvars" >&2; exit 1
}

# ── Resolve SSH private key ──────────────────────────────────────────────────
SSH_KEY="${SSH_KEY:-}"
if [[ -z "$SSH_KEY" ]]; then
  PUB="$(read_var ssh_public_key_path)"
  PUB="${PUB/#\~/$HOME}"
  PRIV="${PUB%.pub}"
  for candidate in "$PRIV" "$HOME/.ssh/dentis-dev-ec2" "$HOME/.ssh/dentis-key"; do
    [[ -f "$candidate" ]] && { SSH_KEY="$candidate"; break; }
  done
fi
[[ -n "$SSH_KEY" ]] || { echo "[ERROR] No SSH private key found. Set SSH_KEY=/path/to/key" >&2; exit 1; }
chmod 600 "$SSH_KEY"

# ── Terraform provision ──────────────────────────────────────────────────────
cd "$STACK_DIR"
terraform init -input=false >/dev/null

if [[ "$AUTO_APPLY" == "true" ]]; then
  echo "[INFO] Running terraform apply..."
  terraform apply -auto-approve
fi

INSTANCE_IP="$(terraform output -raw public_ip 2>/dev/null || echo '')"
[[ -n "$INSTANCE_IP" ]] || { echo "[ERROR] No EC2 public_ip in Terraform output." >&2; exit 1; }
echo "[INFO] EC2 IP: $INSTANCE_IP  |  SSH key: $SSH_KEY"

# ── Generate .env for compose ────────────────────────────────────────────────
cat > "$ROOT_DIR/.env.ec2" <<ENV
WEB_IMAGE=${WEB_REPO}:${WEB_TAG}
APP_IMAGE=${APP_REPO}:${APP_TAG}
WEB_HOST_PORT=${WEB_PORT}
APP_HOST_PORT=${APP_PORT}
DB_NAME=${DB_NAME:-dentis_db}
DB_USER=${DB_USER:-dentis}
DB_PASSWORD=${DB_PASSWORD:-dentis}
JWT_SECRET=${JWT_SECRET:-dentis-jwt-secret-key-minimum-256-bits-long-for-hs256}
MAIL_HOST=${MAIL_HOST:-mailhog}
MAIL_PORT=${MAIL_PORT:-1025}
MAIL_USERNAME=${MAIL_USERNAME:-}
MAIL_PASSWORD=${MAIL_PASSWORD:-}
MAIL_SMTP_HOST_PORT=${MAIL_PORT:-1025}
MAIL_UI_HOST_PORT=8025
ENV

# ── Wait for SSH ─────────────────────────────────────────────────────────────
echo "[INFO] Waiting for SSH..."
for _ in $(seq 1 30); do
  ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=5 \
    ec2-user@"$INSTANCE_IP" "echo ready" >/dev/null 2>&1 && break
  sleep 5
done

# ── Wait for Docker daemon (user_data may still be installing it) ─────────────
echo "[INFO] Waiting for Docker daemon to be ready..."
for _ in $(seq 1 24); do
  ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=5 \
    ec2-user@"$INSTANCE_IP" "sudo docker info >/dev/null 2>&1" && break
  echo "  ... Docker not ready yet, retrying in 10s"
  sleep 10
done
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no \
  ec2-user@"$INSTANCE_IP" "sudo docker info >/dev/null 2>&1" || {
  echo "[ERROR] Docker daemon is not running on EC2 after 4 minutes." >&2
  echo "        Check user_data logs: sudo cat /var/log/cloud-init-output.log" >&2
  exit 1
}

# ── Copy files to EC2 ────────────────────────────────────────────────────────
echo "[INFO] Copying files to EC2..."
scp -i "$SSH_KEY" -o StrictHostKeyChecking=no \
  "$ROOT_DIR/docker-compose.dev.yml" ec2-user@"$INSTANCE_IP":/tmp/docker-compose.yml >/dev/null
scp -i "$SSH_KEY" -o StrictHostKeyChecking=no \
  "$ROOT_DIR/.env.ec2" ec2-user@"$INSTANCE_IP":/tmp/.env >/dev/null
scp -r -i "$SSH_KEY" -o StrictHostKeyChecking=no \
  "$ROOT_DIR/dentis-api/src/main/resources" ec2-user@"$INSTANCE_IP":/tmp/dentis-api-resources >/dev/null

# ── Deploy on EC2 ────────────────────────────────────────────────────────────
echo "[INFO] Deploying on EC2..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" \
  "RESET_DB_VOLUME='$RESET_DB_VOLUME' RESET_ALL_VOLUMES='$RESET_ALL_VOLUMES' bash -s" <<'ENDSSH'
set -euo pipefail

IMDS_TOKEN=$(curl -s -X PUT -H "X-aws-ec2-metadata-token-ttl-seconds: 60" http://169.254.169.254/latest/api/token)
REGION=$(curl -s -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" http://169.254.169.254/latest/meta-data/placement/region)
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
aws ecr get-login-password --region "$REGION" | sudo docker login --username AWS \
  --password-stdin "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com" >/dev/null

sudo mkdir -p /opt/dentis
sudo mv /tmp/docker-compose.yml /opt/dentis/docker-compose.yml
sudo mv /tmp/.env              /opt/dentis/.env
sudo rm -rf /opt/dentis/dentis-api
sudo mkdir -p /opt/dentis/dentis-api/src/main
sudo mv /tmp/dentis-api-resources /opt/dentis/dentis-api/src/main/resources
sudo chown -R ec2-user:ec2-user /opt/dentis
cd /opt/dentis

if [[ "$RESET_ALL_VOLUMES" == "true" ]]; then
  sudo docker compose down -v || true
elif [[ "$RESET_DB_VOLUME" == "true" ]]; then
  sudo docker compose down || true
  sudo docker volume rm dentis-pgdata 2>/dev/null || true
else
  sudo docker compose down 2>/dev/null || true
fi

sudo docker compose pull
sudo docker compose up -d
echo "[INFO] Compose started."
ENDSSH

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo "  Frontend: http://$INSTANCE_IP"
echo "  Backend:  http://$INSTANCE_IP:$APP_PORT"
echo "  MailHog:  http://$INSTANCE_IP:8025"
echo "  SSH:      ssh -i $SSH_KEY ec2-user@$INSTANCE_IP"

# ── Post-deploy verification ─────────────────────────────────────────────────
if [[ "$VERIFY_DEPLOYMENT" != "true" ]]; then
  exit 0
fi

# Wait for backend to become healthy (Spring Boot + Liquibase takes 2-4 min)
echo ""
echo "[INFO] Waiting for backend to become healthy (timeout 5 minutes)..."
APP_HEALTHY=false
for i in $(seq 1 60); do
  code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 \
    "http://$INSTANCE_IP:$APP_PORT/actuator/health" 2>/dev/null || echo "000")
  if [[ "$code" == "200" ]]; then
    APP_HEALTHY=true
    echo "[OK] Backend is healthy after $((i * 5))s (HTTP 200)"
    break
  fi
  # Show container status every 30s so the user can see progress
  if (( i % 6 == 0 )); then
    echo "  ... still waiting (${i}/${60} checks, HTTP $code)"
    ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" \
      "cd /opt/dentis && sudo docker compose ps --format 'table {{.Name}}\t{{.Status}}'" 2>/dev/null || true
  else
    printf "."
  fi
  sleep 5
done
echo ""

if [[ "$APP_HEALTHY" == "false" ]]; then
  echo "[ERROR] Backend did not become healthy within 5 minutes. Dumping logs..."
  echo ""
  echo "── postgres logs ──────────────────────────────────────"
  ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" \
    "cd /opt/dentis && sudo docker compose logs --tail=30 postgres" 2>/dev/null || true
  echo ""
  echo "── liquibase logs ─────────────────────────────────────"
  ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" \
    "cd /opt/dentis && sudo docker compose logs --tail=30 liquibase" 2>/dev/null || true
  echo ""
  echo "── app logs ───────────────────────────────────────────"
  ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" \
    "cd /opt/dentis && sudo docker compose logs --tail=50 app" 2>/dev/null || true
  echo ""
  echo "  Fix the issue and re-run: ./infrastructure/scripts/deploy.sh"
  exit 1
fi

PASS=0; FAIL=0

check() {
  local label="$1" url="$2" svc="${3:-}"
  local code; code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "$url" 2>/dev/null || echo "000")
  if [[ "$code" == "200" ]]; then
    echo "[OK]   $label (HTTP $code)"
    ((PASS++))
  else
    echo "[FAIL] $label (HTTP $code)"
    if [[ -n "$svc" ]]; then
      echo "       Last logs for '$svc':"
      ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" \
        "cd /opt/dentis && sudo docker compose logs --tail=20 $svc" 2>/dev/null | sed 's/^/       /' || true
    fi
    ((FAIL++))
  fi
}

ssh_logs() {
  local svc="$1"
  ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" \
    "cd /opt/dentis && sudo docker compose logs --tail=20 $svc" 2>/dev/null || true
}

echo ""
echo "╔══════════════════════════════════════╗"
echo "║    POST-DEPLOYMENT VERIFICATION      ║"
echo "╚══════════════════════════════════════╝"

echo ""
echo "── Container status ──────────────────────"
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" \
  "cd /opt/dentis && sudo docker compose ps" 2>/dev/null || { echo "[FAIL] Could not reach EC2"; ((FAIL++)); }

echo ""
echo "── Liquibase ─────────────────────────────"
LB_LOGS=$(ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" \
  "cd /opt/dentis && sudo docker compose logs --tail=10 liquibase" 2>/dev/null || echo "")
if echo "$LB_LOGS" | grep -qi "successfully"; then
  echo "[OK]   Liquibase migrations applied"
  ((PASS++))
elif echo "$LB_LOGS" | grep -qi "error\|failed"; then
  echo "[FAIL] Liquibase errors detected:"
  echo "$LB_LOGS"
  ((FAIL++))
else
  echo "[INFO] Liquibase logs:"
  echo "$LB_LOGS"
fi

echo ""
echo "── Health checks ─────────────────────────"
check "Backend  /actuator/health" "http://$INSTANCE_IP:$APP_PORT/actuator/health" "app"
check "Frontend /"                "http://$INSTANCE_IP:$WEB_PORT/"                "web"
check "MailHog  /"                "http://$INSTANCE_IP:8025/"                     "mailhog"

echo ""
echo "──────────────────────────────────────────"
if [[ $FAIL -eq 0 ]]; then
  echo "✓ ALL CHECKS PASSED ($PASS/$((PASS+FAIL)))"
else
  echo "✗ $FAIL ISSUE(S) DETECTED  ($PASS passed / $FAIL failed)"
  echo ""
  echo "  Debug on EC2:"
  echo "    ssh -i $SSH_KEY ec2-user@$INSTANCE_IP"
  echo "    cd /opt/dentis && sudo docker compose logs --tail=50 <service>"
  exit 1
fi

