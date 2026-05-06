# Dentis Dev EC2 - SSH Key, Session Manager y acceso seguro

Este documento explica **cómo meter la clave de acceso** para conectarte por SSH al servidor EC2 de desarrollo y cómo usar **AWS Systems Manager Session Manager** como acceso alternativo.

## Objetivo

- Crear un par de llaves SSH local
- Registrar la llave pública en Terraform (o usar una existente)
- Conectarte con la llave privada

## Despliegue automático desde local (backend + web)

```bash
chmod +x /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/scripts/deploy-dev-ec2.sh
DB_PASSWORD='dentis-dev-password' JWT_SECRET='change-me-min-32-chars' /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/scripts/deploy-dev-ec2.sh
```

El script:

- crea/valida repos ECR privados de backend y frontend,
- hace build y push de ambas imágenes,
- aplica Terraform en `infrastructure/terraform/aws/dev-ec2`,
- y refresca `docker compose` en la EC2.

Por defecto usa el profile AWS `jbello`. Si quieres otro profile:

```bash
AWS_PROFILE_NAME='otro-profile' DB_PASSWORD='dentis-dev-password' JWT_SECRET='change-me-min-32-chars' /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/scripts/deploy-dev-ec2.sh
```

## Opción recomendada: llave gestionada por Terraform

### 1) Generar llave SSH local

```bash
ssh-keygen -t ed25519 -C "dentis-dev-ec2" -f ~/.ssh/dentis-dev-ec2
```

Esto crea:

- privada: `~/.ssh/dentis-dev-ec2`
- pública: `~/.ssh/dentis-dev-ec2.pub`

### 2) Proteger permisos de la privada

```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/dentis-dev-ec2
chmod 644 ~/.ssh/dentis-dev-ec2.pub
```

### 3) Configurar Terraform

En `infrastructure/terraform/aws/dev-ec2/terraform.tfvars`:

```hcl
create_key_pair     = true
ssh_public_key_path = "~/.ssh/dentis-dev-ec2.pub"
ssh_allowed_cidr    = "TU_IP_PUBLICA/32"
```

> `ssh_allowed_cidr` debe ser tu IP pública con máscara `/32`, por ejemplo `186.12.34.56/32`.

### 4) Aplicar Terraform

```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/terraform/aws/dev-ec2
terraform init
terraform apply
```

### 5) Conectarte al servidor

```bash
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@<PUBLIC_IP>
```

Puedes sacar el comando exacto con:

```bash
terraform output ssh_command
```

## Opción alternativa: usar key pair existente

Si ya tienes un key pair en AWS:

```hcl
create_key_pair        = false
existing_key_pair_name = "dentisdev"
ssh_allowed_cidr       = "TU_IP_PUBLICA/32"
```

Y conectas con la llave privada asociada a ese key pair.

## Acceso alternativo: AWS Systems Manager Session Manager

El stack `dev-ec2` queda preparado para acceder también por **Session Manager**, sin depender de abrir una sesión SSH manual.

Comando directo:

```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis/infrastructure/terraform/aws/dev-ec2
terraform output ssm_start_session_command
```

O ejecutándolo directamente:

```bash
aws ssm start-session --target <INSTANCE_ID> --region us-east-1
```

Requisitos en tu máquina local:

- AWS CLI configurado con un profile con permisos sobre SSM
- `session-manager-plugin` instalado si tu AWS CLI no lo incorpora

Comprobación rápida:

```bash
aws ssm describe-instance-information --region us-east-1
```

> Importante: de momento **SSH se mantiene** porque el flujo de despliegue actual sigue usándolo.

## ¿Tiene coste usar Systems Manager?

Para este entorno de desarrollo tal y como está montado ahora (EC2 en subred pública con salida a Internet), **Session Manager no tiene un coste directo adicional**.

Los costes que seguirás teniendo son los habituales del entorno:

- instancia EC2
- volumen EBS
- tráfico de red normal

Solo podrías ver costes extra si habilitas más adelante:

- logs de sesión en CloudWatch Logs
- logs en S3
- cifrado KMS
- VPC endpoints privados para SSM

## Buenas prácticas de seguridad

- No subas la llave privada a Git
- No compartas la llave privada por chat/email
- Mantén `ssh_allowed_cidr` en `/32` (IP fija tuya)
- Si cambia tu IP, actualiza `terraform.tfvars` y aplica de nuevo
- Rota la llave periódicamente (crear nueva + `terraform apply`)

## Troubleshooting rápido

### Permission denied (publickey)

- Verifica ruta de llave privada:

```bash
ls -l ~/.ssh/dentis-dev-ec2
```

- Verifica permisos:

```bash
chmod 600 ~/.ssh/dentis-dev-ec2
```

- Fuerza el uso de esa llave:

```bash
ssh -i ~/.ssh/dentis-dev-ec2 -o IdentitiesOnly=yes ec2-user@<PUBLIC_IP>
```

### Timeout al conectar

- Confirma que tu IP actual coincide con `ssh_allowed_cidr`
- Confirma que la instancia está `running`
- Revisa el Security Group (ingress TCP/22)

