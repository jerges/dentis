# 🚀 Quick Deploy Scripts for Dentis Dev Environment

## 4 Scripts Modulares para Desarrollo

```
infrastructure/scripts/
├── deploy-dev-ec2.sh              [Full deployment - One shot]
├── build-backend-image.sh         [Backend build + ECR push]
├── build-web-image.sh             [Frontend build + ECR push]
└── update-terraform-config.sh     [Sync latest images from ECR]
```

---

## 📖 Ejemplos de Uso

### 🎯 Escenario 1: Primera vez (Deploy completo)
```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis
./infrastructure/scripts/deploy-dev-ec2.sh
```

✅ **Qué hace:**
- Crea repositorios ECR (backend + web)
- Construye imagen del backend
- Construye imagen del frontend
- Sube ambas a ECR
- Ejecuta Terraform para provisionar EC2
- Levanta servicios automáticamente en EC2

📤 **Salida:**
```
[INFO] ✓ Backend image built and pushed successfully
[INFO] ✓ Web image built and pushed successfully
[INFO] Running Terraform apply
...
SSH command: ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@54.xxx.xxx.xxx
Frontend URL: http://54.xxx.xxx.xxx
Backend URL:  http://54.xxx.xxx.xxx:8080
MailHog URL:  http://54.xxx.xxx.xxx:8025
```

---

### 🔄 Escenario 2: Cambios solo en backend
```bash
# 1. Edita codigo backend
# 2. Reconstruir y subir
./infrastructure/scripts/build-backend-image.sh

# 3. Actualizar Terraform con nueva imagen
./infrastructure/scripts/update-terraform-config.sh

# 4. Aplicar cambios en EC2
cd infrastructure/terraform/aws/dev-ec2
terraform apply -auto-approve

# 5. Conectar y validar
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@<IP>
# Dentro del EC2:
sudo docker compose logs -f app
```

---

### 🎨 Escenario 3: Cambios solo en frontend
```bash
# 1. Edita codigo frontend (dentis-web/)
# 2. Reconstruir y subir
./infrastructure/scripts/build-web-image.sh

# 3. Actualizar Terraform
./infrastructure/scripts/update-terraform-config.sh

# 4. Aplicar cambios
cd infrastructure/terraform/aws/dev-ec2 && terraform apply -auto-approve

# 5. Verificar en el browser
# http://<EC2-IP>
```

---

### ⚡ Escenario 4: Tags personalizados
```bash
# Backend con tag v1.2.3
APP_IMAGE_TAG=v1.2.3 ./infrastructure/scripts/build-backend-image.sh

# Web con tag v2.0.1
WEB_IMAGE_TAG=v2.0.1 ./infrastructure/scripts/build-web-image.sh

# Actualizar Terraform (toma automaticamente los últimos)
./infrastructure/scripts/update-terraform-config.sh
```

---

### 🌍 Escenario 5: Región diferente
```bash
# Usar eu-west-1 en lugar de us-east-1
AWS_REGION=eu-west-1 ./infrastructure/scripts/build-backend-image.sh
AWS_REGION=eu-west-1 ./infrastructure/scripts/build-web-image.sh
AWS_REGION=eu-west-1 ./infrastructure/scripts/update-terraform-config.sh
```

---

### 🔄 Escenario 6: Build paralelo (ambas imágenes)
```bash
# Terminal 1
./infrastructure/scripts/build-backend-image.sh

# Terminal 2 (en paralelo)
./infrastructure/scripts/build-web-image.sh

# Después en Terminal 1 o 2:
./infrastructure/scripts/update-terraform-config.sh
```

---

## 📋 Referencia de Scripts

### `deploy-dev-ec2.sh` (Full Deploy)
**Cuándo usar:** Primera vez o reset completo

```bash
./infrastructure/scripts/deploy-dev-ec2.sh
```

**Variables personalizables:**
```bash
AWS_REGION=eu-west-1
AWS_PROFILE_NAME=jbello
ENVIRONMENT=dev
APP_NAME=dentis
INSTANCE_TYPE=t3.medium
APP_IMAGE_TAG=latest
WEB_IMAGE_TAG=latest
```

---

### `build-backend-image.sh` (Backend Only)
**Cuándo usar:** Cambios en backend Java

```bash
./infrastructure/scripts/build-backend-image.sh

# Con tag personalizado
APP_IMAGE_TAG=v1.0.0 ./infrastructure/scripts/build-backend-image.sh
```

**Qué construye:**
- Dockerfile: Maven builder + JRE runtime
- Resultado: imagen Spring Boot lista para producción

---

### `build-web-image.sh` (Frontend Only)
**Cuándo usar:** Cambios en frontend Angular

```bash
./infrastructure/scripts/build-web-image.sh

# Con tag personalizado
WEB_IMAGE_TAG=v1.0.0 ./infrastructure/scripts/build-web-image.sh
```

**Qué construye:**
- Dockerfile.web: Node builder + Nginx runtime
- Resultado: imagen Angular + SPA routing lista

---

### `update-terraform-config.sh` (Sync ECR → Terraform)
**Cuándo usar:** Después de build-backend-image.sh o build-web-image.sh

```bash
./infrastructure/scripts/update-terraform-config.sh
```

**Qué hace:**
1. Lee últimos tags de ECR (backend)
2. Lee últimos tags de ECR (web)
3. Actualiza `terraform.tfvars` con esos tags
4. Mantiene intactas otras configs (DB, JWT, etc)

**Equivalente manual:**
```bash
cd infrastructure/terraform/aws/dev-ec2
# Editar terraform.tfvars y cambiar:
# app_image_tag = "latest"
# web_image_tag = "latest"
```

---

## 🎯 Flujos Recomendados

### Opción A: Quick Start (One Command)
```bash
./infrastructure/scripts/deploy-dev-ec2.sh
```
✅ Todo automatizado | ❌ Construye ambas imágenes (puede ser lento)

### Opción B: Iteración Backend
```bash
./infrastructure/scripts/build-backend-image.sh
./infrastructure/scripts/update-terraform-config.sh
cd infrastructure/terraform/aws/dev-ec2 && terraform apply -auto-approve
```
✅ Rápido para cambios backend | ✅ Frontend no se toca

### Opción C: Iteración Frontend
```bash
./infrastructure/scripts/build-web-image.sh
./infrastructure/scripts/update-terraform-config.sh
cd infrastructure/terraform/aws/dev-ec2 && terraform apply -auto-approve
```
✅ Rápido para cambios frontend | ✅ Backend no se toca

---

## 🔐 Configuración Inicial (Una vez)

```bash
# 1. Configura AWS profile
aws configure --profile jbello
# Input: Access Key, Secret Key, Region (us-east-1), Output (json)

# 2. Valida acceso
AWS_PROFILE_NAME=jbello aws sts get-caller-identity

# 3. Deploy inicial
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis
./infrastructure/scripts/deploy-dev-ec2.sh

# 4. Guarda las URLs de salida
```

---

## 📊 Tiempo Estimado

| Script | Tiempo | Notas |
|--------|--------|-------|
| `deploy-dev-ec2.sh` | ~20-25 min | Primera vez, incluye builds |
| `build-backend-image.sh` | ~5-8 min | Recompila backend |
| `build-web-image.sh` | ~3-5 min | Recompila frontend |
| `update-terraform-config.sh` | ~10 seg | Solo actualiza config |
| `terraform apply` | ~5-7 min | Redeploy en EC2 |

---

## 🆘 Troubleshooting

**Error: Docker disk full**
```bash
docker system prune -a --volumes
rm -rf ~/Downloads/*.dmg ~/Downloads/*.zip
```

**Error: AWS credentials not found**
```bash
aws configure --profile jbello
AWS_PROFILE_NAME=jbello aws sts get-caller-identity
```

**Error: terraform.tfvars not found**
```bash
# Primero ejecutar:
./infrastructure/scripts/deploy-dev-ec2.sh
```

**Error: EC2 no levanta**
```bash
# Ver logs en EC2:
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@<IP>
sudo docker compose logs
```

---

## 📚 Documentación Completa

Ver `SCRIPTS-GUIDE.md` para documentación detallada, todas las variables de entorno, y casos de uso avanzados.

```bash
cat infrastructure/SCRIPTS-GUIDE.md
```

---

**Last Updated:** May 5, 2026

