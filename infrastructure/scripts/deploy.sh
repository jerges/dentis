#!/usr/bin/env bash
# Deploy completo: provisiona EC2 con Terraform, copia ficheros, lanza docker-compose
# y verifica el despliegue. También instala/actualiza el CloudWatch Agent.
#
# Uso: ./infrastructure/scripts/deploy.sh
#
# Env vars (todas opcionales):
#   RESET_DB_VOLUME=true     Eliminar y recrear el volumen de postgres
#   RESET_ALL_VOLUMES=true   Eliminar todos los volúmenes de compose
#   AUTO_APPLY=false         Saltar terraform apply (solo redeploy compose)
#   VERIFY_DEPLOYMENT=false  Saltar health checks post-deploy
#   ALERT_EMAIL=user@example.com  Email para alarmas CloudWatch (sobreescribe tfvars)
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

# Ensure AWS/Terraform/SSH commands do not inherit shell proxy settings.
unset HTTP_PROXY HTTPS_PROXY ALL_PROXY NO_PROXY http_proxy https_proxy all_proxy no_proxy

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
instance_type          = "t4g.small"
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
web_port               = 8081
landing_port           = 80

db_name                = "dentis_db"
db_user                = "dentis"
db_password            = "dentis"
jwt_secret             = "dentis-jwt-secret-key-minimum-256-bits-long-for-hs256"

mail_host              = "mailhog"
mail_port              = 1025
mail_username          = ""
mail_password          = ""

spring_profiles_active = "dev"
alert_email            = "${ALERT_EMAIL:-}"
TFVARS
  echo "[OK] terraform.tfvars created."
fi

# ── Patch existing tfvars if landing_port is missing ────────────────────────
TFVARS_CHANGED=false
TMP_TFVARS="$(mktemp)"
cp "$STACK_DIR/terraform.tfvars" "$TMP_TFVARS"

if ! grep -q "^landing_port" "$STACK_DIR/terraform.tfvars"; then
  echo "[INFO] Migrating terraform.tfvars: splitting web_port→8081 and landing_port=80..."
  TMP="$(mktemp)"
  while IFS= read -r line; do
    if [[ "$line" =~ ^web_port[[:space:]]*=[[:space:]]*80 ]]; then
      printf 'web_port               = 8081\n'
    else
      printf '%s\n' "$line"
    fi
  done < "$STACK_DIR/terraform.tfvars" > "$TMP"
  printf 'landing_port           = 80\n' >> "$TMP"
  mv "$TMP" "$STACK_DIR/terraform.tfvars"
  TFVARS_CHANGED=true
fi

if ! grep -q "^alert_email" "$STACK_DIR/terraform.tfvars"; then
  ALERT_EMAIL_VAL="${ALERT_EMAIL:-}"
  printf 'alert_email            = "%s"\n' "$ALERT_EMAIL_VAL" >> "$STACK_DIR/terraform.tfvars"
  TFVARS_CHANGED=true
fi


[[ "$TFVARS_CHANGED" == "true" ]] && echo "[OK] terraform.tfvars migrated."
rm -f "$TMP_TFVARS"

# ── Read values from tfvars ──────────────────────────────────────────────────
read_var() {
  sed -nE "s/^$1[[:space:]]*=[[:space:]]*\"?([^\"]*)\"?[[:space:]]*$/\1/p" \
    "$STACK_DIR/terraform.tfvars" | tail -1
}

APP_REPO="$(read_var ecr_repository_url)"
APP_TAG="$(read_var app_image_tag)";   [[ -n "$APP_TAG" ]]  || APP_TAG="latest"
WEB_REPO="$(read_var web_ecr_repository_url)"
WEB_TAG="$(read_var web_image_tag)";   [[ -n "$WEB_TAG" ]]  || WEB_TAG="latest"
APP_PORT="$(read_var app_port)";           [[ -n "$APP_PORT" ]]     || APP_PORT="8080"
WEB_PORT="$(read_var web_port)";           [[ -n "$WEB_PORT" ]]     || WEB_PORT="8081"
LANDING_PORT="$(read_var landing_port)";   [[ -n "$LANDING_PORT" ]] || LANDING_PORT="80"
ATTACHMENTS_BUCKET="$(terraform -chdir="$STACK_DIR" output -raw attachments_bucket_name 2>/dev/null || echo '')"

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
AWS_REGION=${AWS_REGION}
WEB_IMAGE=${WEB_REPO}:${WEB_TAG}
APP_IMAGE=${APP_REPO}:${APP_TAG}
WEB_HOST_PORT=${WEB_PORT}
APP_HOST_PORT=${APP_PORT}
LANDING_HOST_PORT=${LANDING_PORT}
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
S3_ATTACHMENTS_BUCKET=${ATTACHMENTS_BUCKET}
GRAFANA_USER=${GRAFANA_USER:-admin}
GRAFANA_PASSWORD=${GRAFANA_PASSWORD:-dentis2026}
PROMETHEUS_PORT=${PROMETHEUS_PORT:-9090}
GRAFANA_PORT=${GRAFANA_PORT:-3000}
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

# ── Install / update CloudWatch Agent on EC2 ────────────────────────────────
echo "[INFO] Configuring CloudWatch Agent..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@"$INSTANCE_IP" bash <<'CWSSH'
set -euo pipefail

# Install agent if missing
if ! command -v /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl &>/dev/null; then
  echo "  Installing amazon-cloudwatch-agent..."
  sudo dnf install -y amazon-cloudwatch-agent 2>/dev/null || \
  sudo yum install -y amazon-cloudwatch-agent
fi

# Write config
sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
sudo tee /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json >/dev/null <<'CWCONFIG'
{
  "agent": { "metrics_collection_interval": 60 },
  "metrics": {
    "namespace": "DentisDevEC2",
    "append_dimensions": { "InstanceId": "${aws:InstanceId}" },
    "metrics_collected": {
      "mem":  { "measurement": ["mem_used_percent"],  "metrics_collection_interval": 60 },
      "disk": { "measurement": ["disk_used_percent"], "resources": ["/"],
                "metrics_collection_interval": 60, "drop_device": true }
    }
  }
}
CWCONFIG

# (Re)start agent with new config
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config -m ec2 -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json >/dev/null
echo "  CloudWatch Agent running."
CWSSH

# ── Copy files to EC2 ────────────────────────────────────────────────────────
echo "[INFO] Copying files to EC2..."
scp -i "$SSH_KEY" -o StrictHostKeyChecking=no \
  "$ROOT_DIR/docker-compose.dev.yml" ec2-user@"$INSTANCE_IP":/tmp/docker-compose.yml >/dev/null
scp -i "$SSH_KEY" -o StrictHostKeyChecking=no \
  "$ROOT_DIR/.env.ec2" ec2-user@"$INSTANCE_IP":/tmp/.env >/dev/null
scp -r -i "$SSH_KEY" -o StrictHostKeyChecking=no \
  "$ROOT_DIR/dentis-api/src/main/resources" ec2-user@"$INSTANCE_IP":/tmp/dentis-api-resources >/dev/null
scp -r -i "$SSH_KEY" -o StrictHostKeyChecking=no \
  "$ROOT_DIR/landing" ec2-user@"$INSTANCE_IP":/tmp/dentis-landing >/dev/null
scp -r -i "$SSH_KEY" -o StrictHostKeyChecking=no \
  "$ROOT_DIR/infrastructure/monitoring" ec2-user@"$INSTANCE_IP":/tmp/dentis-monitoring >/dev/null

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
sudo rm -rf /opt/dentis/landing
sudo mv /tmp/dentis-landing /opt/dentis/landing
sudo rm -rf /opt/dentis/infrastructure/monitoring
sudo mkdir -p /opt/dentis/infrastructure
sudo mv /tmp/dentis-monitoring /opt/dentis/infrastructure/monitoring
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
CW_LOGS_URL="https://console.aws.amazon.com/cloudwatch/home?region=${AWS_REGION}#logsV2:log-groups\$3FlogGroupNameFilter\$3D%2Fdentis%2Fdev"
CW_ALARMS_URL="https://console.aws.amazon.com/cloudwatch/home?region=${AWS_REGION}#alarmsV2:"

echo ""
PROMETHEUS_PORT="${PROMETHEUS_PORT:-9090}"
GRAFANA_PORT="${GRAFANA_PORT:-3000}"

echo "  Landing:    http://$INSTANCE_IP:$LANDING_PORT"
echo "  Frontend:   http://$INSTANCE_IP:$WEB_PORT"
echo "  Backend:    http://$INSTANCE_IP:$APP_PORT"
echo "  MailHog:    http://$INSTANCE_IP:8025"
echo "  Prometheus: http://$INSTANCE_IP:$PROMETHEUS_PORT"
echo "  Grafana:    http://$INSTANCE_IP:$GRAFANA_PORT  (admin / dentis2026)"
echo "  SSH:        ssh -i $SSH_KEY ec2-user@$INSTANCE_IP"
echo ""
echo "  CloudWatch Logs:   $CW_LOGS_URL"
echo "  CloudWatch Alarms: $CW_ALARMS_URL"

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
check "Landing    /"                "http://$INSTANCE_IP:$LANDING_PORT/"                    "landing"
check "Backend    /actuator/health" "http://$INSTANCE_IP:$APP_PORT/actuator/health"        "app"
check "Frontend   /"                "http://$INSTANCE_IP:$WEB_PORT/"                        "web"
check "MailHog    /"                "http://$INSTANCE_IP:8025/"                             "mailhog"
check "Prometheus /-/healthy"       "http://$INSTANCE_IP:$PROMETHEUS_PORT/-/healthy"        "prometheus"
check "Grafana    /api/health"      "http://$INSTANCE_IP:$GRAFANA_PORT/api/health"          "grafana"

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

