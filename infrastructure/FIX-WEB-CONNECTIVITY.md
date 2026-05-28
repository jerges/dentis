# 🔧 Corrección de Conectividad Web - AWS EC2 Dev Stack

## Problema Identificado

La URL frontend generada por Terraform no era accesible. El contenedor Nginx no podía conectar con el backend porque:

1. **Puerto hardcodeado**: `nginx.dev.conf` tenía `proxy_pass http://app:8080;` sin variabilidad
2. **Falta de variables de entorno**: El contenedor `web` no recibía `app_port` ni `app_host`
3. **Red no definida**: Los servicios podían no resolver correctamente entre ellos
4. **Falta de entrypoint dinámico**: Nginx no podía leer variables de entorno en tiempo de ejecución

## Cambios Realizados

### 1. **docker-compose.ec2-dev.yml.tftpl** (Templates → Variables Dinámicas)

✅ **Agregado a contenedor `web`:**
```yaml
environment:
  APP_PORT: "${app_port}"
  NGINX_APP_HOST: "app"
```

✅ **Agregada red explícita a todos los servicios:**
```yaml
networks:
  - dentis-network
```

✅ **Agregada definición de red:**
```yaml
networks:
  dentis-network:
    driver: bridge
```

**Por qué:** Docker Compose con bridge network permite DNS interno y resolución de nombres entre contenedores.

---

### 2. **Dockerfile.web** (Runtime Dinámico)

✅ **Agregado entrypoint script:**
```dockerfile
RUN echo '#!/bin/bash\n\
export APP_PORT=${APP_PORT:-8080}\n\
export NGINX_APP_HOST=${NGINX_APP_HOST:-app}\n\
exec nginx -g "daemon off;"' > /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
```

**Por qué:** Los values `${VAR}` en Nginx deben estar disponibles en tiempo de ejecución, no de build.

---

### 3. **nginx.dev.conf** (Variables Dinámicas)

✅ **Cambio de hardcodeado a variables:**
```nginx
# Antes (hardcodeado):
proxy_pass http://app:8080;

# Después (dinámico):
set $app_port "${APP_PORT:-8080}";
set $app_host "${NGINX_APP_HOST:-app}";
proxy_pass http://$app_host:$app_port;
```

✅ **Agregado DNS resolver:**
```nginx
resolver 127.0.0.11 valid=10s;
```

✅ **Agregados timeouts:**
```nginx
proxy_connect_timeout 60s;
proxy_send_timeout 60s;
proxy_read_timeout 60s;
```

✅ **Agregada página de error:**
```nginx
error_page 502 503 504 /50x.html;
```

---

## 🔄 Flujo Ahora (Corregido)

```
1. EC2 inicia
   ↓
2. Docker Compose levanta servicios con BRIDGE NETWORK
   ├─ app (Spring Boot) en puerto 8080
   ├─ web (Nginx) en puerto 80
   └─ postgres, mailhog
   ↓
3. Contenedor web recibe env vars: APP_PORT=8080, NGINX_APP_HOST=app
   ↓
4. Entrypoint script exporta las variables
   ↓
5. Nginx lee variables desde el daemon running
   ↓
6. Nginx resuelve "app" → IP interna (127.0.0.11 DNS)
   ↓
7. Nginx proxy-pass a http://app:8080
   ↓
8. Cliente browser accede a http://<EC2-IP>/ → Nginx → app:8080
```

---

## ✅ Validación Local

Antes de desplegar, puedes validar locally:

```bash
cd /Users/pw-jbello/developer/workspace/adakadavra/dentis

# Build web image
APP_IMAGE_TAG=test WEB_IMAGE_TAG=test \
  docker-compose -f <(cat << 'EOF'
version: '3.8'
services:
  app:
    image: nginx:alpine
    ports:
      - "8080:80"
  web:
    build:
      context: .
      dockerfile: infrastructure/docker/Dockerfile.web
    environment:
      APP_PORT: "8080"
      NGINX_APP_HOST: "app"
    ports:
      - "80:80"
    depends_on:
      - app
    networks:
      - test-net
  
  app:
    image: nginx:alpine
    ports:
      - "8080:80"
    networks:
      - test-net

networks:
  test-net:
    driver: bridge
EOF
) up

# Test
curl http://localhost/
curl http://localhost/api/
```

---

## 🚀 Deploy Actualizado

Una vez que liberes espacio en tu Mac:

```bash
# Reconstruir imagen web con nuevos cambios
./infrastructure/scripts/build-web-image.sh

# Actualizar Terraform config
./infrastructure/scripts/update-terraform-config.sh

# Aplicar cambios en EC2
cd infrastructure/terraform/aws/dev-ec2
terraform apply -auto-approve

# Verificar
SSH_COMMAND=$(terraform output -raw ssh_command)
eval "$SSH_COMMAND"

# Dentro del EC2
sudo docker compose logs -f web

# En local
curl http://<EC2-IP>/
# Debe resolver correctamente y mostrar frontend
```

---

## 📋 Variables de Entorno

| Variable | Default | Container | Uso |
|----------|---------|-----------|-----|
| `APP_PORT` | `8080` | web | Puerto interno del backend |
| `NGINX_APP_HOST` | `app` | web | Hostname interno del backend |
| `app_port` | `8080` | all | Variable Terraform |
| `web_port` | `80` | all | Variable Terraform |

---

## 🔍 Diagnóstico si falla

**Paso 1: Ver logs Nginx en EC2**
```bash
ssh -i ~/.ssh/dentis-dev-ec2 ec2-user@<IP>
sudo docker compose logs web
```

**Paso 2: Validar DNS interno**
```bash
sudo docker compose exec web nslookup app
# Debe resolver a IP interna, e.g., 172.18.0.x
```

**Paso 3: Validar proxy**
```bash
sudo docker compose exec web curl http://app:8080/
# Debe conectar sin error
```

**Paso 4: Validar desde host**
```bash
curl -v http://<EC2-IP>/
# Buscar:
# HTTP/1.1 200 OK
# Content-Type: text/html
```

---

## 📝 Archivos Modificados

| Archivo | Cambios |
|---------|---------|
| `infrastructure/terraform/aws/dev-ec2/templates/docker-compose.ec2-dev.yml.tftpl` | ✅ Variables env web, networks |
| `infrastructure/docker/nginx.dev.conf` | ✅ Variables dinámicas, resolver, timeouts |
| `infrastructure/docker/Dockerfile.web` | ✅ Entrypoint script, env exports |

---

**Actualizado:** May 5, 2026

