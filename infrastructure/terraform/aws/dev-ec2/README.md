# Dentis AWS Dev EC2 (Docker Compose + PostgreSQL local)

## Fast path (one command from local)

```bash
chmod +x /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/scripts/deploy-dev-ec2.sh
DB_PASSWORD='dentis-dev-password' JWT_SECRET='change-me-min-32-chars' /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/scripts/deploy-dev-ec2.sh
```

El script crea/actualiza repos ECR de backend y frontend, construye/pushea imágenes y aplica Terraform para EC2 dev.

Este stack crea un entorno **simple e independiente** para desarrollo:

- 1 EC2 pública (Amazon Linux 2023)
- Docker + Docker Compose instalados por `user_data`
- `docker compose` con `web + app + postgres + mailhog`
- Imágenes de backend y frontend desde **ECR privado**
- Seguridad con acceso SSH restringido por CIDR
- Acceso adicional por **AWS Systems Manager Session Manager**

## 1) Preparar imágenes en ECR

Puedes usar el stack ECS actual para obtener el repo ECR y subir la imagen.

```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/terraform/aws
terraform output ecr_repository_url
```

Desde la raíz del proyecto:

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ECR_REPOSITORY_URL>
docker build -f infrastructure/docker/Dockerfile -t <ECR_REPOSITORY_URL>:latest .
docker push <ECR_REPOSITORY_URL>:latest

aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <WEB_ECR_REPOSITORY_URL>
docker build -f infrastructure/docker/Dockerfile.web -t <WEB_ECR_REPOSITORY_URL>:latest .
docker push <WEB_ECR_REPOSITORY_URL>:latest
```

## 2) Crear/usar llave SSH

Sigue la guía completa en `infrastructure/DEPLOYMENT-DEV-EC2.md`.

## 3) Desplegar stack dev-ec2

```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/terraform/aws/dev-ec2
cp terraform.tfvars.example terraform.tfvars
```

Edita `terraform.tfvars` con:

- `ssh_allowed_cidr` = tu IP pública en formato `/32`
- `ecr_repository_url` = repo privado ECR backend
- `web_ecr_repository_url` = repo privado ECR frontend
- `db_password`, `jwt_secret`

Inicializa y aplica:

```bash
terraform init
terraform plan
terraform apply
```

## 4) Conectarte y validar

```bash
terraform output ssh_command
terraform output ssm_start_session_command
terraform output app_url
terraform output web_url
terraform output mailhog_url
```

En EC2:

```bash
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@<PUBLIC_IP>
sudo docker ps
sudo docker compose -f /opt/dentis/docker-compose.yml logs -f app
```

O con Session Manager, sin abrir una sesión SSH manual:

```bash
aws ssm start-session --target $(terraform output -raw instance_id) --region us-east-1
```

> Requisito local: tener AWS CLI configurado y, si tu instalación no lo trae integrado, el plugin `session-manager-plugin` instalado.

## Coste de usar Session Manager

En este escenario actual (instancia en subred pública con salida a Internet), **Session Manager no añade un coste directo adicional** por habilitar el acceso interactivo.

Seguirás pagando lo normal de tu entorno:

- EC2
- EBS
- transferencia de datos estándar

Podrían aparecer costes extra solo si más adelante activas alguno de estos complementos:

- logging de sesión en CloudWatch Logs
- logging en S3
- cifrado con KMS
- VPC endpoints privados para SSM/EC2 Messages/SSM Messages

## 5) Destruir entorno

```bash
terraform destroy
```

