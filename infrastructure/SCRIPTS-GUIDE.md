# Scripts de Deploy - Dentis Dev Environment

## 📋 Descripción

Cuatro scripts modulares para construir imágenes Docker, subirlas a ECR y desplegar la aplicación en EC2 con Terraform.

## 🚀 Scripts Disponibles

### 1. `build-backend-image.sh` - Solo Backend
Construye y sube la imagen Docker del backend a ECR.

```bash
# Con defaults
./infrastructure/scripts/build-backend-image.sh

# Con variables personalizadas
APP_IMAGE_TAG=v1.2.3 ./infrastructure/scripts/build-backend-image.sh
AWS_REGION=eu-west-1 ./infrastructure/scripts/build-backend-image.sh
```

**Variables de entorno:**
- `APP_IMAGE_TAG` (default: `latest`) - Tag de la imagen del backend
- `AWS_REGION` (default: `us-east-1`)
- `AWS_PROFILE_NAME` (default: `jbello`)
- `ENVIRONMENT` (default: `dev`)
- `APP_NAME` (default: `dentis`)

**Salida esperada:**
```
[INFO] Building backend image
[INFO] Pushing backend image to ECR
[INFO] ✓ Backend image built and pushed successfully
[INFO] Image URL: 742671448563.dkr.ecr.us-east-1.amazonaws.com/dentis-dev-backend:latest
```

---

### 2. `build-web-image.sh` - Solo Frontend
Construye y sube la imagen Docker del frontend (Angular + Nginx) a ECR.

```bash
# Con defaults
./infrastructure/scripts/build-web-image.sh

# Con variables personalizadas
WEB_IMAGE_TAG=v1.2.3 ./infrastructure/scripts/build-web-image.sh
```

**Variables de entorno:**
- `WEB_IMAGE_TAG` (default: `latest`) - Tag de la imagen del web
- `AWS_REGION` (default: `us-east-1`)
- `AWS_PROFILE_NAME` (default: `jbello`)
- `ENVIRONMENT` (default: `dev`)
- `APP_NAME` (default: `dentis`)

---

### 3. `update-terraform-config.sh` - Actualizar Configuración Terraform
Busca las **últimas imágenes en ECR** y actualiza `terraform.tfvars` automáticamente.

```bash
# Con defaults
./infrastructure/scripts/update-terraform-config.sh

# Con variables personalizadas
AWS_REGION=eu-west-1 ./infrastructure/scripts/update-terraform-config.sh
```

**¿Qué hace?**
1. Consulta ECR para obtener el tag más reciente del backend
2. Consulta ECR para obtener el tag más reciente del web
3. Actualiza `infrastructure/terraform/aws/dev-ec2/terraform.tfvars` con esos tags
4. Mantiene intactas todas las otras configuraciones (DB_PASSWORD, JWT_SECRET, etc)

**Variables de entorno:**
- `AWS_REGION` (default: `us-east-1`)
- `AWS_PROFILE_NAME` (default: `jbello`)
- `ENVIRONMENT` (default: `dev`)
- `APP_NAME` (default: `dentis`)

---

### 4. `deploy-dev-ec2.sh` - Deploy Completo (One-Shot)
Script todo-en-uno que cubre todo el ciclo: build, push, terraform apply.

```bash
./infrastructure/scripts/deploy-dev-ec2.sh
```

---

## 📝 Flujos de Uso Recomendados

### Flujo A: Primera vez (Deploy Completo)
```bash
./infrastructure/scripts/deploy-dev-ec2.sh
```
✅ Construye ambas imágenes → Sube a ECR → Provee infraestructura → Levanta servicios

---

### Flujo B: Actualizaciones Iterativas (Backend solamente)
```bash
# 1. Edita código del backend
# 2. Reconstruye y sube imagen
./infrastructure/scripts/build-backend-image.sh

# 3. Actualiza Terraform con la nueva imagen
./infrastructure/scripts/update-terraform-config.sh

# 4. Aplica cambios en EC2
cd infrastructure/terraform/aws/dev-ec2
terraform apply -auto-approve
```

---

### Flujo C: Actualizaciones Iterativas (Frontend solamente)
```bash
# 1. Edita código del frontend
# 2. Reconstruye y sube imagen
./infrastructure/scripts/build-web-image.sh

# 3. Actualiza Terraform con la nueva imagen
./infrastructure/scripts/update-terraform-config.sh

# 4. Aplica cambios en EC2
cd infrastructure/terraform/aws/dev-ec2
terraform apply -auto-approve
```

---

### Flujo D: Ambas imágenes en paralelo
```bash
# Opción 1: Secuencial
./infrastructure/scripts/build-backend-image.sh
./infrastructure/scripts/build-web-image.sh
./infrastructure/scripts/update-terraform-config.sh

# Opción 2: En paralelo (en dos terminales)
# Terminal 1:
./infrastructure/scripts/build-backend-image.sh

# Terminal 2:
./infrastructure/scripts/build-web-image.sh

# Luego:
./infrastructure/scripts/update-terraform-config.sh
```

---

## 🔧 Configuración Inicial (Una sola vez)

1. **Configura tu perfil AWS** (si aún no está hecho):
   ```bash
   aws configure --profile jbello
   # Ingresa: Access Key ID, Secret Access Key, Region, Output format
   ```

2. **Valida acceso AWS:**
   ```bash
   AWS_PROFILE_NAME=jbello aws sts get-caller-identity
   ```

3. **Ejecuta deploy inicial:**
   ```bash
   ./infrastructure/scripts/deploy-dev-ec2.sh
   ```

4. **Guarda la salida:**
   ```
   SSH command: ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@<PUBLIC_IP>
   Frontend URL: http://<PUBLIC_IP>
   Backend URL:  http://<PUBLIC_IP>:8080
   MailHog URL:  http://<PUBLIC_IP>:8025
   ```

---

## 🌍 Variables de Entorno Globales

Válidas para **todos** los scripts:

```bash
export AWS_REGION=us-east-1                    # AWS region
export AWS_PROFILE_NAME=jbello                 # AWS CLI profile
export ENVIRONMENT=dev                         # dev/staging/prod
export APP_NAME=dentis                         # App name
export INSTANCE_TYPE=t3.medium                 # EC2 instance type (deploy-dev-ec2.sh solo)
```

**Ejemplo - Usar región EU:**
```bash
AWS_REGION=eu-west-1 ./infrastructure/scripts/build-backend-image.sh
AWS_REGION=eu-west-1 ./infrastructure/scripts/update-terraform-config.sh
```

---

## 📊 Casos de Uso

| Escenario | Script |
|-----------|--------|
| Deployar por primera vez | `deploy-dev-ec2.sh` |
| Cambios solo en backend | `build-backend-image.sh` → `update-terraform-config.sh` |
| Cambios solo en frontend | `build-web-image.sh` → `update-terraform-config.sh` |
| Ver última imagen en ECR | `update-terraform-config.sh` |
| Iterar sin terraform apply | `build-backend-image.sh` (no ejecutar update-terraform-config.sh) |

---

## 🐛 Troubleshooting

### Error: "AWS account ID" 
```
[ERROR] Could not get AWS account ID. Check aws credentials.
```
**Solución:**
```bash
aws configure --profile jbello
AWS_PROFILE_NAME=jbello aws sts get-caller-identity
```

### Error: Docker login failed
```
[ERROR] docker login failed
```
**Solución:**
```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 742671448563.dkr.ecr.us-east-1.amazonaws.com
```

### Error: "terraform.tfvars not found"
```
[ERROR] terraform.tfvars not found
```
**Solución:** Ejecuta primero `deploy-dev-ec2.sh` para generar el archivo:
```bash
./infrastructure/scripts/deploy-dev-ec2.sh
```

### Error: Disco lleno (Docker build fails with I/O error)
```
ERROR: failed to solve: write /var/lib/desktop-containerd: input/output error
```
**Solución:** Libera espacio en tu Mac:
```bash
rm -rf ~/Downloads/*.dmg ~/Downloads/*.zip
docker system prune -a --volumes
```

---

## 📌 Notas Importantes

1. **Las imágenes se etiquetan por defecto como `latest`**. Para versionar:
   ```bash
   APP_IMAGE_TAG=v1.2.3 ./infrastructure/scripts/build-backend-image.sh
   WEB_IMAGE_TAG=v1.2.3 ./infrastructure/scripts/build-web-image.sh
   ```

2. **El script `update-terraform-config.sh` toma automáticamente el tag más reciente del ECR**, incluso si no lo construiste ahora.

3. **`terraform.tfvars` no se regenera automáticamente** entre ejecuciones. Usa `update-terraform-config.sh` para sincronizar.

4. **Los valores de DB_PASSWORD y JWT_SECRET vienen de `application.yml`** por defecto (no los sobrescribe `update-terraform-config.sh`).

5. **Para destruir la infraestructura:**
   ```bash
   cd infrastructure/terraform/aws/dev-ec2
   terraform destroy -auto-approve
   ```

---

## 📞 Resumen Rápido

```bash
# Primera vez
./infrastructure/scripts/deploy-dev-ec2.sh

# Iterar en backend
./infrastructure/scripts/build-backend-image.sh
./infrastructure/scripts/update-terraform-config.sh
cd infrastructure/terraform/aws/dev-ec2 && terraform apply -auto-approve

# Iterar en frontend
./infrastructure/scripts/build-web-image.sh
./infrastructure/scripts/update-terraform-config.sh
cd infrastructure/terraform/aws/dev-ec2 && terraform apply -auto-approve
```

---

**Última actualización:** Mayo 5, 2026

