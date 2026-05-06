# Dentis — Guía de Despliegue en la Nube

## Estructura

```
infrastructure/
├── docker/
│   └── Dockerfile              ← Multi-stage build (JDK 21 → JRE slim)
└── terraform/
    ├── aws/                    ← ECS Fargate + RDS + ALB + ECR + Secrets Manager
    └── azure/                  ← Container Apps + PostgreSQL + ACR + Key Vault
```

---

## Opción A — AWS

### Entorno de desarrollo simple en EC2

Para trabajar en dev con una sola EC2 (Docker Compose + PostgreSQL local + imagen privada en ECR), usa el stack independiente en:

- `infrastructure/terraform/aws/dev-ec2`
- Guía de llaves SSH: `infrastructure/DEPLOYMENT-DEV-EC2.md`

### Servicios creados
| Recurso | Servicio AWS |
|---|---|
| Contenedor | ECS Fargate |
| Registro Docker | ECR |
| Base de datos | RDS PostgreSQL 16 |
| Load Balancer | ALB (Application Load Balancer) |
| Secretos | Secrets Manager |
| Red | VPC + subnets públicas/privadas + NAT |
| Logs | CloudWatch Logs |

### Pre-requisitos
```bash
# 1. Instalar herramientas
brew install terraform awscli

# 2. Autenticarte en AWS
aws configure
# AWS Access Key ID:      REPLACE_WITH_YOUR_KEY_ID
# AWS Secret Access Key:  REPLACE_WITH_YOUR_SECRET
# Default region:         us-east-1

# 3. Crear bucket S3 para estado de Terraform
aws s3 mb s3://dentis-terraform-state-UNIQUE_SUFFIX --region us-east-1
aws s3api put-bucket-versioning \
  --bucket dentis-terraform-state-UNIQUE_SUFFIX \
  --versioning-configuration Status=Enabled

# 4. Crear tabla DynamoDB para lock
aws dynamodb create-table \
  --table-name dentis-terraform-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### Despliegue
```bash
cd infrastructure/terraform/aws

# Actualizar backend.tf con tu bucket y tabla
# Luego:
terraform init

cp terraform.tfvars.example terraform.tfvars
# Editar terraform.tfvars con tus valores

# Pasar secrets de forma segura (sin escribirlos en archivos)
export TF_VAR_db_password="TU_PASSWORD_AQUI"
export TF_VAR_jwt_secret="TU_JWT_SECRET_MIN_32_CHARS"
export TF_VAR_mail_username="tu-email@gmail.com"
export TF_VAR_mail_password="TU_APP_PASSWORD"

terraform plan
terraform apply
```

### GitHub Actions Secrets (para CI/CD en AWS)
Configurar en Settings → Secrets → Actions:

| Secret | Descripción |
|---|---|
| `AWS_ACCESS_KEY_ID` | IAM Access Key |
| `AWS_SECRET_ACCESS_KEY` | IAM Secret Key |
| `AWS_REGION` | Región (ej: us-east-1) |
| `ECR_REPOSITORY_URL` | URL del repositorio ECR (output de Terraform) |
| `ECS_CLUSTER_NAME` | Nombre del cluster ECS (output de Terraform) |
| `ECS_SERVICE_NAME` | Nombre del servicio ECS (output de Terraform) |

---

## Opción B — Azure

### Servicios creados
| Recurso | Servicio Azure |
|---|---|
| Contenedor | Azure Container Apps |
| Registro Docker | Azure Container Registry (ACR) |
| Base de datos | Azure Database for PostgreSQL Flexible Server |
| Secretos | Azure Key Vault |
| Red | VNet + subnets con delegation |
| Logs | Log Analytics Workspace |

### Pre-requisitos
```bash
# 1. Instalar herramientas
brew install terraform azure-cli

# 2. Autenticarte en Azure
az login

# 3. Crear Service Principal para Terraform (anotar el output)
az ad sp create-for-rbac \
  --name "dentis-terraform-sp" \
  --role Contributor \
  --scopes /subscriptions/REPLACE_WITH_SUBSCRIPTION_ID \
  --sdk-auth
# Guarda el JSON completo — lo usarás como AZURE_CREDENTIALS en GitHub

# 4. Crear Storage Account para el estado de Terraform
az group create --name rg-dentis-tfstate --location eastus
az storage account create \
  --name dentistfstateXXXX \
  --resource-group rg-dentis-tfstate \
  --location eastus \
  --sku Standard_LRS
az storage container create \
  --name tfstate \
  --account-name dentistfstateXXXX
```

### Despliegue
```bash
cd infrastructure/terraform/azure

# Actualizar backend.tf con tu storage account
# Luego:

# Variables de autenticación del Service Principal
export ARM_SUBSCRIPTION_ID="REPLACE_WITH_SUBSCRIPTION_ID"
export ARM_TENANT_ID="REPLACE_WITH_TENANT_ID"
export ARM_CLIENT_ID="REPLACE_WITH_CLIENT_ID"
export ARM_CLIENT_SECRET="REPLACE_WITH_CLIENT_SECRET"

terraform init

cp terraform.tfvars.example terraform.tfvars
# Editar terraform.tfvars con tus valores

# Pasar secrets de forma segura
export TF_VAR_db_admin_password="TU_PASSWORD_AQUI"
export TF_VAR_jwt_secret="TU_JWT_SECRET_MIN_32_CHARS"
export TF_VAR_mail_username="tu-email@gmail.com"
export TF_VAR_mail_password="TU_APP_PASSWORD"

terraform plan
terraform apply
```

### GitHub Actions Secrets (para CI/CD en Azure)
Configurar en Settings → Secrets → Actions:

| Secret | Descripción |
|---|---|
| `AZURE_CREDENTIALS` | JSON completo del Service Principal (az ad sp create-for-rbac) |
| `ACR_LOGIN_SERVER` | Login server del ACR (output de Terraform) |
| `ACR_USERNAME` | Admin username del ACR (output de Terraform) |
| `ACR_PASSWORD` | Admin password del ACR (output de Terraform) |
| `ACR_NAME` | Nombre del ACR |
| `CONTAINER_APP_NAME` | Nombre del Container App (output de Terraform) |
| `AZURE_RESOURCE_GROUP` | Nombre del Resource Group (output de Terraform) |

---

## Push manual de imagen Docker

### AWS
```bash
# Obtener los comandos exactos del output de Terraform
terraform output docker_push_commands
```

### Azure
```bash
terraform output docker_push_commands
```

---

## Regiones recomendadas para Venezuela (menor latencia)

| Nube | Región | Ubicación |
|---|---|---|
| AWS | `us-east-1` | Virginia (recomendada) |
| AWS | `sa-east-1` | São Paulo (más cercana) |
| Azure | `eastus` | Virginia |
| Azure | `brazilsouth` | São Paulo (más cercana) |
