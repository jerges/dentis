#!/usr/bin/env bash
# deploy-landing.sh — Despliega la landing page.
#
# Modos de operación (detección automática):
#   1. S3 + CloudFront (producción): usa outputs del stack Terraform aws/
#   2. EC2 dev (fallback): copia archivos al EC2 del stack dev-ec2 vía SCP
#
# Variables de entorno opcionales:
#   LANDING_BUCKET      — Override bucket S3 (modo prod)
#   LANDING_CF_ID       — Override CloudFront ID (modo prod)
#   AWS_REGION          — Región AWS (default: us-east-1)
#   AWS_PROFILE         — Perfil AWS CLI (default: jbello)
#   SSH_KEY             — Clave SSH para EC2 dev
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LANDING_MODULE_DIR="${PROJECT_ROOT}/infrastructure/terraform/aws/modules/landing"
PROD_TF_DIR="${PROJECT_ROOT}/infrastructure/terraform/aws"
DEV_TF_DIR="${PROJECT_ROOT}/infrastructure/terraform/aws/dev-ec2"
LANDING_DIR="${PROJECT_ROOT}/landing"

AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE="${AWS_PROFILE:-jbello}"

export AWS_PROFILE

# ─── Detectar modo ───────────────────────────────────────────────────────────
BUCKET_NAME="${LANDING_BUCKET:-}"
CF_DISTRIBUTION_ID="${LANDING_CF_ID:-}"

if [[ -z "${BUCKET_NAME}" || -z "${CF_DISTRIBUTION_ID}" ]]; then
  # Preferir el módulo standalone de landing (tiene su propio estado)
  if [[ -f "${LANDING_MODULE_DIR}/terraform.tfstate" ]]; then
    STATE_RESOURCES=$(python3 -c "import json,sys; d=json.load(open('${LANDING_MODULE_DIR}/terraform.tfstate')); print(len(d.get('resources',[])))" 2>/dev/null || echo "0")
    if [[ "${STATE_RESOURCES}" -gt 0 ]]; then
      echo "[INFO] Leyendo outputs del módulo landing (standalone)..."
      cd "${LANDING_MODULE_DIR}"
      BUCKET_NAME=$(terraform output -raw bucket_name 2>/dev/null || true)
      CF_DISTRIBUTION_ID=$(terraform output -raw cloudfront_distribution_id 2>/dev/null || true)
    fi
  fi
fi

if [[ -z "${BUCKET_NAME}" || -z "${CF_DISTRIBUTION_ID}" ]]; then
  # Fallback: leer del stack root (si fue aplicado como parte de la infra completa)
  if [[ -d "${PROD_TF_DIR}/.terraform" ]]; then
    echo "[INFO] Leyendo outputs Terraform (stack root)..."
    cd "${PROD_TF_DIR}"
    BUCKET_NAME=$(terraform output -raw landing_bucket_name 2>/dev/null || true)
    CF_DISTRIBUTION_ID=$(terraform output -raw landing_cloudfront_id 2>/dev/null || true)
  fi
fi

# ─── Modo producción: S3 + CloudFront ────────────────────────────────────────
if [[ -n "${BUCKET_NAME}" && -n "${CF_DISTRIBUTION_ID}" ]]; then
  echo ""
  echo "[INFO] Modo: S3 + CloudFront (producción)"
  echo "  Fuente : ${LANDING_DIR}"
  echo "  Bucket : s3://${BUCKET_NAME}"
  echo "  CF dist: ${CF_DISTRIBUTION_ID}"
  echo ""

  aws s3 sync \
    "${LANDING_DIR}/" \
    "s3://${BUCKET_NAME}/" \
    --region "${AWS_REGION}" \
    --profile "${AWS_PROFILE}" \
    --delete \
    --cache-control "max-age=86400" \
    --exclude ".DS_Store" \
    --exclude "*.map"

  # index.html con cache corto para que los deploys propaguen rápido
  aws s3 cp \
    "${LANDING_DIR}/index.html" \
    "s3://${BUCKET_NAME}/index.html" \
    --region "${AWS_REGION}" \
    --profile "${AWS_PROFILE}" \
    --cache-control "max-age=300, must-revalidate" \
    --content-type "text/html; charset=utf-8"

  echo "[INFO] Creando invalidación CloudFront..."
  INVALIDATION_ID=$(aws cloudfront create-invalidation \
    --distribution-id "${CF_DISTRIBUTION_ID}" \
    --paths "/*" \
    --profile "${AWS_PROFILE}" \
    --query 'Invalidation.Id' \
    --output text)

  echo "[OK] Invalidación creada: ${INVALIDATION_ID}"
  echo "[OK] Los cambios estarán activos en ~60 segundos."
  exit 0
fi

# ─── Modo dev-ec2: copiar al EC2 vía SCP ────────────────────────────────────
echo "[INFO] No hay outputs S3/CloudFront — usando modo dev-ec2."

INSTANCE_IP=""
if [[ -d "${DEV_TF_DIR}/.terraform" ]]; then
  INSTANCE_IP="$(cd "${DEV_TF_DIR}" && terraform output -raw public_ip 2>/dev/null || true)"
fi

if [[ -z "${INSTANCE_IP}" ]]; then
  echo "[ERROR] No se encontró IP de EC2 en el estado Terraform." >&2
  echo "        Opciones:" >&2
  echo "          1. Ejecuta ./infrastructure/scripts/deploy.sh primero para provisionar EC2." >&2
  echo "          2. Pasa las variables de entorno LANDING_BUCKET y LANDING_CF_ID para modo producción." >&2
  exit 1
fi

# Resolver clave SSH
SSH_KEY="${SSH_KEY:-}"
if [[ -z "${SSH_KEY}" ]]; then
  for candidate in "$HOME/.ssh/dentis-dev-ec2" "$HOME/.ssh/dentis-key" "$HOME/.ssh/id_ed25519"; do
    [[ -f "${candidate}" ]] && { SSH_KEY="${candidate}"; break; }
  done
fi
[[ -n "${SSH_KEY}" ]] || { echo "[ERROR] No se encontró clave SSH. Define la variable SSH_KEY." >&2; exit 1; }

echo ""
echo "[INFO] Modo: EC2 dev"
echo "  Fuente  : ${LANDING_DIR}"
echo "  EC2     : ${INSTANCE_IP}"
echo "  SSH key : ${SSH_KEY}"
echo ""

# Copiar landing al EC2
scp -r -i "${SSH_KEY}" -o StrictHostKeyChecking=no \
  "${LANDING_DIR}/" ec2-user@"${INSTANCE_IP}":/tmp/dentis-landing-update >/dev/null

# Mover a /opt/dentis/landing en el EC2 y recargar nginx
ssh -i "${SSH_KEY}" -o StrictHostKeyChecking=no ec2-user@"${INSTANCE_IP}" bash <<'ENDSSH'
set -euo pipefail
sudo rm -rf /opt/dentis/landing
sudo mv /tmp/dentis-landing-update /opt/dentis/landing
sudo chown -R ec2-user:ec2-user /opt/dentis/landing
sudo chmod -R 755 /opt/dentis/landing

# Recargar nginx del contenedor landing sin bajarlo
if sudo docker ps --format '{{.Names}}' | grep -q dentis-landing; then
  sudo docker exec dentis-landing nginx -s reload
  echo "[OK] nginx recargado en el contenedor dentis-landing."
else
  echo "[WARN] Contenedor dentis-landing no encontrado. Levantando..."
  cd /opt/dentis && sudo docker compose up -d landing
fi
ENDSSH

echo "[OK] Landing actualizada en http://${INSTANCE_IP}:80"
