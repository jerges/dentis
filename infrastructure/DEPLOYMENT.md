# Dentis — Guía de Despliegue

## Arquitectura actual (desarrollo)

```
infrastructure/
├── docker/
│   ├── Dockerfile          ← Backend: Maven multi-stage (JDK 25 → JRE alpine)
│   └── Dockerfile.web      ← Frontend: Node 20 → Nginx 1.27
├── scripts/
│   ├── deploy.sh           ← Orquestador completo (Terraform + files + Compose)
│   ├── build-backend.sh    ← Build + push ECR + restart app en EC2
│   ├── build-web.sh        ← Build + push ECR + restart web en EC2
│   ├── deploy-landing.sh   ← S3 + CloudFront (modo prod) o SCP EC2 (modo dev)
│   ├── start-ec2.sh        ← Inicia servicios Compose en EC2
│   ├── stop-ec2.sh         ← Para servicios Compose en EC2
│   └── destroy.sh          ← Destruye infraestructura Terraform
└── terraform/
    └── aws/
        ├── dev-ec2/        ← Stack activo: EC2 t2.small + S3 + IAM + CloudWatch
        └── modules/
            └── landing/    ← Landing page: S3 privado + CloudFront OAC
```

---

## Entorno de desarrollo (activo)

Stack: EC2 `t2.small` en `us-east-1` con Docker Compose.

### Recursos AWS aprovisionados

| Recurso | Tipo | Nombre / ID |
|---|---|---|
| EC2 | t4g.small (arm64, Graviton2) | `i-06cdd459dff85e0db` |
| IP pública | Elastic IP | `18.210.9.9` |
| ECR backend | Imagen Docker | `742671448563.dkr.ecr.us-east-1.amazonaws.com/dentis-dev-backend:latest` |
| ECR frontend | Imagen Docker | `742671448563.dkr.ecr.us-east-1.amazonaws.com/dentis-dev-web:latest` |
| S3 adjuntos | Bucket privado | `dentis-dev-attachments-742671448563` |
| S3 landing | Bucket privado | `dentis-landing` |
| CloudFront | Distribución OAC | `E2Y4ZFSUC6AGL2` |
| CloudWatch | Logs + Alarms | `/dentis/dev/*` — 5 alarmas |
| Bedrock | IAM role | EC2 role con `bedrock:InvokeModel` |

### URLs del entorno dev

| Servicio | URL |
|---|---|
| Landing page (prod) | `https://d3tv842cpzfh1w.cloudfront.net` |
| Landing page (dev) | `http://18.210.9.9:80` |
| Frontend Angular | `http://18.210.9.9:8081` |
| Backend API | `http://18.210.9.9:8080` |
| MailHog UI | `http://18.210.9.9:8025` |
| Prometheus | `http://18.210.9.9:9090` |
| Grafana | `http://18.210.9.9:3000` (admin / dentis2026) |

### Servicios en Docker Compose

| Contenedor | Imagen | Puerto |
|---|---|---|
| `dentis-app` | ECR backend (JDK 25) | 8080 |
| `dentis-web` | ECR frontend (Nginx) | 8081 |
| `dentis-landing` | nginx:1.27-alpine | 80 |
| `dentis-postgres` | pgvector/pgvector:pg16 | 5432 |
| `dentis-liquibase` | liquibase:4.24 | — |
| `dentis-mailhog` | mailhog:latest | 1025 / 8025 |
| `dentis-prometheus` | prom/prometheus:v2.52.0 | 9090 |
| `dentis-grafana` | grafana:10.4.3 | 3000 |

---

## Despliegue completo (primera vez o actualización total)

```bash
# Requisitos: aws CLI + terraform + docker buildx + ssh configurado
./infrastructure/scripts/deploy.sh
```

El script:
1. Valida cuenta AWS (`742671448563`)
2. Crea `terraform.tfvars` si no existe (interactivo la primera vez)
3. `terraform apply` — provisiona EC2, VPC, IAM, S3, CloudWatch
4. Genera `.env.ec2` con los outputs de Terraform (incluye `S3_ATTACHMENTS_BUCKET`)
5. Espera SSH + Docker daemon
6. Copia archivos al EC2 (`docker-compose.dev.yml`, `.env`, recursos Liquibase, landing, monitoring)
7. `docker compose pull && docker compose up -d`
8. Verifica salud de todos los servicios (timeout 5 min)

Variables de entorno opcionales:
```bash
AUTO_APPLY=false          # Salta terraform apply (solo redeploy)
RESET_DB_VOLUME=true      # Resetea volumen de postgres
RESET_ALL_VOLUMES=true    # Resetea todos los volúmenes
VERIFY_DEPLOYMENT=false   # Salta health checks
```

---

## Rebuild y redeploy de imágenes

Solo cuando hay cambios en el código del backend o el frontend:

```bash
# Backend (Java/Spring Boot)
DOCKER_PLATFORMS=linux/arm64 ./infrastructure/scripts/build-backend.sh

# Frontend (Angular)
DOCKER_PLATFORMS=linux/arm64 ./infrastructure/scripts/build-web.sh
```

El script hace:
1. Build multi-stage Docker (Maven + JRE / Node + Nginx)
2. Push a ECR
3. SSH al EC2 → `docker compose pull <svc> && docker compose up -d <svc>`

> **Nota:** `linux/arm64` es el target correcto para el EC2 `t4g.small` (Graviton2 ARM64).
> En Apple Silicon, la compilación es nativa (sin QEMU) — build en ~3-5 min.

---

## Despliegue de la landing page

```bash
./infrastructure/scripts/deploy-landing.sh
```

Detección automática de modo:
- **S3 + CloudFront** (modo activo): lee outputs del módulo `modules/landing/`
- **EC2 dev** (fallback): copia archivos al EC2 vía SCP y recarga nginx

---

## Arrancar / parar el entorno dev

```bash
# Arrancar EC2 (desde AWS CLI)
aws ec2 start-instances --instance-ids i-06cdd459dff85e0db --region us-east-1 --profile jbello

# Iniciar servicios Compose (EC2 ya debe estar running)
./infrastructure/scripts/start-ec2.sh

# Parar servicios Compose
./infrastructure/scripts/stop-ec2.sh

# Parar instancia EC2
aws ec2 stop-instances --instance-ids i-06cdd459dff85e0db --region us-east-1 --profile jbello
```

> La instancia tiene un Lambda de auto-stop que la para automáticamente tras 2 horas de ejecución.

---

## Variables de entorno necesarias

El archivo `.env.ec2` (no commiteado) se genera automáticamente por `deploy.sh` leyendo los outputs de Terraform. Variables clave:

```dotenv
AWS_REGION=us-east-1
APP_IMAGE=742671448563.dkr.ecr.us-east-1.amazonaws.com/dentis-dev-backend:latest
WEB_IMAGE=742671448563.dkr.ecr.us-east-1.amazonaws.com/dentis-dev-web:latest
S3_ATTACHMENTS_BUCKET=dentis-dev-attachments-742671448563

# IA (Amazon Bedrock)
IA_ENABLED=true
IA_GEN_MODEL=us.amazon.nova-pro-v1:0
IA_EMBED_MODEL=amazon.titan-embed-text-v2:0
IA_MIN_SCORE=0.72
IA_CHUNK_SIZE=800
```

El `.env.ec2` se copia al EC2 como `/opt/dentis/.env` durante el deploy.

---

## IAM — permisos del EC2 role

El role de EC2 tiene las siguientes políticas:

| Política / permiso | Para qué |
|---|---|
| `AmazonEC2ContainerRegistryReadOnly` | Pull de imágenes ECR |
| `AmazonSSMManagedInstanceCore` | Session Manager (SSH alternativo) |
| `CloudWatchAgentServerPolicy` | Métricas de memoria/disco |
| `logs:CreateLogGroup`, `logs:PutLogEvents` | Logs de contenedores a CloudWatch |
| `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` en `dentis-dev-attachments-*` | Adjuntos clínicos y documentos |
| `bedrock:InvokeModel` | Asistente IA (Nova Pro + Titan v2) |

---

## Requisitos de la base de datos

PostgreSQL 16 con extensiones:

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;       -- pgvector (embeddings IA)
CREATE EXTENSION IF NOT EXISTS pg_trgm;      -- búsqueda trigram (documentos)
CREATE EXTENSION IF NOT EXISTS unaccent;     -- búsqueda sin tildes
```

Estas extensiones se activan automáticamente mediante Liquibase (changelogs 001 y 006).

---

## Módulo landing (CloudFront)

Stack Terraform independiente en `modules/landing/`:

```bash
cd infrastructure/terraform/aws/modules/landing
terraform init
terraform apply
```

Recursos creados:
- S3 bucket `dentis-landing` (privado, versioning activado)
- CloudFront OAC `E1H3L0WSRER146`
- Distribución CloudFront `E2Y4ZFSUC6AGL2` — `https://d3tv842cpzfh1w.cloudfront.net`

Deploy de contenido:
```bash
./infrastructure/scripts/deploy-landing.sh
```

---

## SSH y Session Manager

```bash
# SSH directo
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@18.210.9.9

# Session Manager (sin puerto SSH abierto)
aws ssm start-session --target i-06cdd459dff85e0db --region us-east-1 --profile jbello
```

Logs en EC2:
```bash
cd /opt/dentis
sudo docker compose logs --tail=50 app
sudo docker compose logs --tail=50 web
sudo docker compose logs --tail=50 liquibase
```

---

## Monitoreo

CloudWatch Logs: `/dentis/dev/app`, `/dentis/dev/web`, `/dentis/dev/postgres`, `/dentis/dev/liquibase`, `/dentis/dev/mailhog`, `/dentis/dev/landing`

CloudWatch Alarms (SNS `dentis-dev-alerts`):
- CPU > 80% durante 10 min
- Status check failed
- Memoria > 85%
- Disco / > 80%

Grafana: `http://18.210.9.9:3000` (admin / dentis2026) — dashboards con métricas de Spring Boot y base de datos.
