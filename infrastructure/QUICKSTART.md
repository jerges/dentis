# 🚀 Dentis AWS EC2 - Guía Rápida de Despliegue

## Pre-requisitos

- AWS CLI configurado con profile `jbello` (o ajusta `AWS_PROFILE_NAME` en los scripts)
- SSH keys en `~/.ssh/dentis-dev-ec2` (se crean automáticamente)
- Docker instalado localmente (para compilar imágenes)

---

## Opción 1: Despliegue Completo (Recomendado para Primer Uso)

Este approach construye imágenes, crea ECR repos y despliega todo:

```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis

# Ejecutar deploy completo (crea ECR repos, construye y pushea imágenes, levanta EC2)
./infrastructure/scripts/deploy-dev-ec2.sh
```

**Qué hace:**
1. ✓ Crea repositorios ECR si no existen
2. ✓ Construye imágenes Docker (backend + web) y las pushea a ECR
3. ✓ Aplica Terraform para crear la EC2
4. ✓ Despliega docker-compose en la EC2
5. ✓ Verifica automáticamente el deployment

**Salida esperada:**
```
Frontend URL: http://52.x.x.x
Backend URL:  http://52.x.x.x:8080
MailHog URL:  http://52.x.x.x:8025
```

---

## Opción 2: Despliegue Rápido (Para Redeploys)

Si ya tienes imágenes en ECR y solo necesitas actualizar/redeplegar:

### Paso 1: Obtener URLs de ECR

```bash
./infrastructure/scripts/prepare-ecr-urls.sh
```

Verás algo como:
```
ecr_repository_url     = "123456789012.dkr.ecr.us-east-1.amazonaws.com/dentis-dev-backend"
web_ecr_repository_url = "123456789012.dkr.ecr.us-east-1.amazonaws.com/dentis-dev-web"
```

### Paso 2: Ejecutar despliegue rápido

```bash
./infrastructure/scripts/deploy-simple.sh
```

El script te preguntará por:
- ECR backend URL (paste el valor del paso 1)
- ECR web URL (paste el valor del paso 1)

Luego generará automáticamente `terraform.tfvars` y desplegará.

---

## Operaciones Comunes

### Resetear base de datos (guardar configuración)

```bash
RESET_DB_VOLUME=true ./infrastructure/scripts/deploy-simple.sh
```

### Forzar reset completo (limpiar todos los volúmenes)

```bash
RESET_ALL_VOLUMES=true ./infrastructure/scripts/deploy-simple.sh
```

### Saltarse verificación post-deploy

```bash
VERIFY_DEPLOYMENT=false ./infrastructure/scripts/deploy-simple.sh
```

### Usar AWS profile diferente

```bash
AWS_PROFILE_NAME=otro-profile ./infrastructure/scripts/deploy-simple.sh
```

---

## Solucionar Problemas

### "Missing terraform.tfvars" 

Ejecuta `deploy-simple.sh` y será generado automáticamente con tus valores.

### "Frontend no carga / Backend no responde"

El script de despliegue te mostrará automáticamente los problemas:
- Estado de contenedores
- Logs de Liquibase (migraciones DB)
- Health check del backend
- Logs de Nginx

**Para debugging manual:**

```bash
# SSH a la instancia
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@<IP_PUBLICA>

# Ver estado de contenedores
cd /opt/dentis
sudo docker compose ps

# Ver logs
sudo docker compose logs --tail=100 liquibase
sudo docker compose logs --tail=100 app
sudo docker compose logs --tail=100 web

# Verificar conectividad local
curl http://localhost
curl http://localhost:8080/actuator/health
```

### "ECR repositories not found"

Ejecuta `deploy-dev-ec2.sh` que crea los repos automáticamente, o crearlos manualmente:

```bash
aws ecr create-repository --region us-east-1 --repository-name dentis-dev-backend
aws ecr create-repository --region us-east-1 --repository-name dentis-dev-web
```

---

## Costes Aproximados (USD/mes en us-east-1)

| Recurso | Tipo | Coste |
|---------|------|-------|
| **EC2** | t3.medium | ~$30 |
| **EBS** | 30 GB gp3 | ~$3 |
| **IP elástica** | Si no se usa | $0 (no aplican charges) |
| **ECR** | Almacenamiento | <$1 (almacenamiento bajo) |
| **Tráfico** | Data transfer | Bajo (~$1) |
| | **Total** | ~**$35/mes** |

> 💡 **Tip:** Para ahorrar aún más, usa `t2.micro` (tier gratuito si aplica) o destruye la instancia cuando no la necesites: `terraform destroy`

---

## Destruir Recursos

```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/terraform/aws/dev-ec2
terraform destroy
```

---

## Recursos Adicionales

- [Terraform Config](infrastructure/terraform/aws/dev-ec2/)
- [Docker Compose](docker-compose.dev.yml)
- [Deployment Docs](infrastructure/DEPLOYMENT-DEV-EC2.md)
- [Connectivity Troubleshooting](infrastructure/FIX-WEB-CONNECTIVITY.md)


