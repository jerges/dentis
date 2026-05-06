# AWS Dev Deployment - Simplified

**Enfoque limpio:**
- ✅ ECR: Build/Push con scripts que funcionan
- ✅ EC2: Solo deploy de docker-compose
- ✅ Sin `user_data` complejo
- ✅ Fácil de iterar

## Flujo en 3 Pasos

### 1️⃣ Provisionar EC2 (primera vez)
```bash
cd infrastructure/terraform/aws/dev-ec2
terraform apply -auto-approve
```

### 2️⃣ Build & Push a ECR (cada cambio)
```bash
./infrastructure/scripts/build-backend-image.sh
./infrastructure/scripts/build-web-image.sh
./infrastructure/scripts/update-terraform-config.sh
```

`update-terraform-config.sh` automáticamente:
- Toma los últimos tags de ECR
- Aplica terraform para actualizar la config
- Muestra la IP de EC2

### 3️⃣ Deploy a EC2
```bash
./infrastructure/scripts/deploy-simple.sh
```

Eso es todo. El script:
1. Lee las imágenes desde `terraform.tfvars`
2. Copia `docker-compose.dev.yml` + `.env` a EC2
3. Levanta los servicios

## URLs de Acceso
```
Frontend:  http://<PUBLIC_IP>
Backend:   http://<PUBLIC_IP>:8080
MailHog:   http://<PUBLIC_IP>:8025
SSH:       ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@<PUBLIC_IP>
```

## Variables de Entorno (opcional)
```bash
export DB_PASSWORD=dentis
export JWT_SECRET=dentis-jwt-secret-key-minimum-256-bits-long-for-hs256
```

## Flujo Completo para Nueva Sesión

```bash
# 1. Build backend + web
./infrastructure/scripts/build-backend-image.sh
./infrastructure/scripts/build-web-image.sh

# 2. Actualizar EC2 con nuevas imágenes y terrafo apply
./infrastructure/scripts/update-terraform-config.sh

# 3. Deploy el compose
./infrastructure/scripts/deploy-simple.sh

# 4. Conectar
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@$(terraform output -raw public_ip)
```

## Iterar Rápido

**Solo cambios backend:**
```bash
./infrastructure/scripts/build-backend-image.sh
./infrastructure/scripts/update-terraform-config.sh
./infrastructure/scripts/deploy-simple.sh
```

**Solo cambios frontend:**
```bash
./infrastructure/scripts/build-web-image.sh
./infrastructure/scripts/update-terraform-config.sh
./infrastructure/scripts/deploy-simple.sh
```

## Docker Compose Local (sin EC2)

Para desarrollo en local con `docker-compose.dev.yml`:
```bash
export WEB_IMAGE=dentis-web:latest
export APP_IMAGE=dentis-backend:latest
docker compose -f docker-compose.dev.yml pull
docker compose -f docker-compose.dev.yml up
```

## Borrar Todo
```bash
./infrastructure/scripts/destroy-dev-ec2.sh
```

## Archivos Involucrados

```
dentis/
├── docker-compose.dev.yml          ← Config estándar
├── .env.ec2                         ← Generado por deploy-simple.sh
├── infrastructure/
│   ├── scripts/
│   │   ├── build-backend-image.sh   ← Build backend
│   │   ├── build-web-image.sh       ← Build web
│   │   ├── update-terraform-config.sh ← Update + apply terraform
│   │   ├── deploy-simple.sh         ← Deploy compose a EC2 ⭐
│   │   ├── update-ssh-access.sh     ← Fix SSH si IP cambió
│   │   └── destroy-dev-ec2.sh       ← Limpieza
│   ├── docker/
│   │   ├── Dockerfile
│   │   ├── Dockerfile.web
│   │   └── nginx.dev.conf
│   └── terraform/aws/dev-ec2/
│       └── ...simple provisioning
```

---
**Last Updated:** May 5, 2026

