# Dentis Dev EC2 — Guía de operación

Entorno de desarrollo corriendo sobre EC2 `t4g.small` (ARM Graviton2) con Docker Compose.

## Estado actual

| Recurso | Valor |
|---|---|
| Instancia | `i-06cdd459dff85e0db` — `t4g.small` (arm64) |
| IP pública (Elastic IP) | `18.210.9.9` |
| Región | `us-east-1` |
| Perfil AWS CLI | `jbello` |
| Stack Terraform | `infrastructure/terraform/aws/dev-ec2/` |
| SSH key | `~/.ssh/dentis-dev-ec2` |

---

## Despliegue completo (primera vez)

```bash
./infrastructure/scripts/deploy.sh
```

El script hace todo: Terraform apply, build/push de imágenes ECR, copia de archivos al EC2 y arranque de Docker Compose con verificación de salud.

## Rebuild de imágenes (cambios en código)

```bash
# Backend (Java/Spring Boot) — build nativo ARM en Apple Silicon
DOCKER_PLATFORMS=linux/arm64 ./infrastructure/scripts/build-backend.sh

# Frontend (Angular)
DOCKER_PLATFORMS=linux/arm64 ./infrastructure/scripts/build-web.sh
```

Cada script:
1. Construye la imagen Docker (multi-stage Maven + JRE / Node + Nginx)
2. Push a ECR
3. SSH al EC2 → `docker compose pull <svc> && docker compose up -d <svc>`

## Redeploy sin rebuild (actualizar config o archivos)

```bash
AUTO_APPLY=false ./infrastructure/scripts/deploy.sh
```

Copia `docker-compose.dev.yml`, `.env.ec2`, recursos Liquibase, landing y monitoring al EC2 y hace `docker compose up -d`.

---

## Arrancar / parar el entorno

```bash
# 1. Iniciar la instancia EC2 (si está stopped)
aws ec2 start-instances --instance-ids i-06cdd459dff85e0db \
  --region us-east-1 --profile jbello

# 2. Iniciar servicios Compose (EC2 ya running)
./infrastructure/scripts/start-ec2.sh

# Para servicios Compose (sin parar EC2)
./infrastructure/scripts/stop-ec2.sh

# Parar instancia EC2
aws ec2 stop-instances --instance-ids i-06cdd459dff85e0db \
  --region us-east-1 --profile jbello
```

> La instancia tiene un Lambda de auto-stop: se apaga automáticamente a las 2 horas de estar encendida.

---

## SSH y acceso remoto

```bash
# SSH directo
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@18.210.9.9

# AWS Systems Manager Session Manager (alternativa sin SSH expuesto)
aws ssm start-session --target i-06cdd459dff85e0db \
  --region us-east-1 --profile jbello
```

Ver logs de contenedores:
```bash
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@18.210.9.9
cd /opt/dentis
sudo docker compose ps
sudo docker compose logs --tail=50 app
sudo docker compose logs --tail=50 web
sudo docker compose logs --tail=50 liquibase
```

---

## Gestión de claves SSH

La clave fue generada en `~/.ssh/dentis-dev-ec2` y registrada en Terraform.

```bash
# Verificar permisos correctos
chmod 600 ~/.ssh/dentis-dev-ec2
chmod 644 ~/.ssh/dentis-dev-ec2.pub

# Obtener el comando SSH exacto del estado Terraform
cd infrastructure/terraform/aws/dev-ec2
terraform output ssh_command
```

Para renovar la clave: genera una nueva con `ssh-keygen`, actualiza `ssh_public_key_path` en `terraform.tfvars` y aplica Terraform.

---

## Variables de entorno

El archivo `.env.ec2` (no commiteado, no compartir) se genera automáticamente por `deploy.sh` leyendo los outputs de Terraform:

```dotenv
AWS_REGION=us-east-1
APP_IMAGE=742671448563.dkr.ecr.us-east-1.amazonaws.com/dentis-dev-backend:latest
WEB_IMAGE=742671448563.dkr.ecr.us-east-1.amazonaws.com/dentis-dev-web:latest
WEB_HOST_PORT=8081
APP_HOST_PORT=8080
LANDING_HOST_PORT=80
DB_NAME=dentis_db
DB_USER=dentis
DB_PASSWORD=dentis
JWT_SECRET=dentis-jwt-secret-key-minimum-256-bits-long-for-hs256
S3_ATTACHMENTS_BUCKET=dentis-dev-attachments-742671448563
GRAFANA_USER=admin
GRAFANA_PASSWORD=dentis2026
```

El `.env.ec2` se copia al EC2 como `/opt/dentis/.env` durante el deploy.

---

## Estructura en el EC2

```
/opt/dentis/
├── docker-compose.yml          ← Copia de docker-compose.dev.yml
├── .env                        ← Copia de .env.ec2
├── dentis-api/
│   └── src/main/resources/     ← Changelogs Liquibase y config
├── landing/                    ← Archivos HTML/CSS/JS de la landing
├── infrastructure/
│   └── monitoring/             ← Config Prometheus y dashboards Grafana
└── (volúmenes Docker gestionados por compose)
```

---

## Troubleshooting

### El EC2 no responde por SSH

1. Confirma que la instancia esté `running`:
   ```bash
   aws ec2 describe-instances --instance-ids i-06cdd459dff85e0db \
     --region us-east-1 --profile jbello \
     --query 'Reservations[0].Instances[0].State.Name'
   ```
2. Si está `stopped`, arráncala con `aws ec2 start-instances ...`
3. Confirma que tu IP esté en `ssh_allowed_cidr` (actualmente `0.0.0.0/0`)
4. Usa Session Manager como alternativa si SSH está bloqueado

### El backend no arranca

```bash
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@18.210.9.9
cd /opt/dentis
sudo docker compose logs --tail=100 liquibase   # migraciones
sudo docker compose logs --tail=100 app          # errores de Spring Boot
```

Causas comunes:
- Variables de entorno faltantes en `.env` (especialmente `S3_ATTACHMENTS_BUCKET`)
- Migración Liquibase fallida (mismatch de changelog)
- Puerto 8080 ya en uso

### Resetear la base de datos

```bash
RESET_DB_VOLUME=true ./infrastructure/scripts/deploy.sh
```

Esto para todos los servicios, elimina el volumen `dentis-pgdata`, y reinicia desde cero (Liquibase recreará el esquema con datos demo).

---

## Costes estimados (us-east-1)

| Recurso | Coste aprox. |
|---|---|
| EC2 t4g.small (mientras running) | ~$0.0168/hora (~$0.10 si son 6h/día) |
| EBS gp3 30 GiB | ~$2.40/mes |
| S3 adjuntos (tráfico bajo en dev) | < $0.50/mes |
| CloudWatch Logs (7 días retención) | < $1/mes |
| ECR (2 repos, imágenes ~300 MB) | < $0.30/mes |
| Elastic IP (solo cuando parada) | $0.005/hora |

> El auto-stop Lambda evita costes por olvidar la EC2 encendida.
