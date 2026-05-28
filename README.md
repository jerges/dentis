# Dentis — Sistema de Gestión Dental Integral

> Plataforma backoffice para optimizar los procesos clínicos, administrativos y financieros de consultas y clínicas dentales en Venezuela.

## Módulos

| Módulo | Descripción |
|---|---|
| [`dentis-common`](./dentis-common/README.md) | Utilidades compartidas, excepciones base y respuestas API |
| [`dentis-patient`](./dentis-patient/README.md) | Gestión de pacientes e historias demográficas |
| [`dentis-scheduling`](./dentis-scheduling/README.md) | Agenda clínica por profesional |
| [`dentis-clinical`](./dentis-clinical/README.md) | Historia clínica digital y odontograma |
| [`dentis-billing`](./dentis-billing/README.md) | Aranceles, presupuestos y pagos |
| [`dentis-notification`](./dentis-notification/README.md) | Notificaciones y recordatorios por email |
| [`dentis-api`](./dentis-api/README.md) | API REST principal, seguridad JWT, punto de entrada |
| [`dentis-web`](./dentis-web/README.md) | Frontend Angular 18 — Backoffice SPA |

## Stack Tecnológico

**Backend**
- Java 21 + Spring Boot 4.0.6
- Spring Security (JWT stateless)
- Spring Data JPA + PostgreSQL 16
- MapStruct 1.6 + Lombok
- Liquibase (migraciones versionadas)
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5 + Mockito (tests unitarios)

**Frontend**
- Angular 18 (standalone components)
- Angular Material 18
- RxJS 7
- TypeScript 5

**Infraestructura**
- Docker (multi-stage build, JRE Alpine)
- Terraform (AWS ECS Fargate o Azure Container Apps)
- GitHub Actions (CI/CD)

## Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                     dentis-api                          │
│  REST Controllers · JWT Security · GlobalExceptionHandler│
└──────────┬──────────┬──────────┬──────────┬────────────┘
           │          │          │          │
    ┌──────┴──┐ ┌─────┴───┐ ┌───┴────┐ ┌───┴──────┐
    │ patient │ │schedule │ │clinical│ │ billing  │
    │         │ │         │ │        │ │          │
    │ domain  │ │ domain  │ │ domain │ │  domain  │
    │ app     │ │  app    │ │  app   │ │   app    │
    │ infra   │ │  infra  │ │  infra │ │  infra   │
    └────┬────┘ └────┬────┘ └───┬────┘ └────┬─────┘
         └───────────┴──────────┴────────────┘
                         │
                  ┌──────┴──────┐
                  │  PostgreSQL │
                  └─────────────┘
```

Monolito modular diseñado para extraerse en microservicios por bounded context.

## Inicio Rápido

### Requisitos
- Java 21+
- Maven 3.9+
- Docker + Docker Compose
- Node.js 20+ (para el frontend)

### Backend

```bash
# 1. Preparar variables de entorno
cp .env.example .env

# 2. Levantar PostgreSQL y MailHog
docker compose --profile dev up -d postgres mailhog

# 3. Crear/actualizar tablas con Liquibase usando Docker Compose
docker compose --profile dev run --rm liquibase

# 4. Compilar todos los módulos
mvn clean install -DskipTests

# 5. Ejecutar la API con perfil de desarrollo
mvn -pl dentis-api spring-boot:run -Dspring-boot.run.profiles=dev

# API disponible en: http://localhost:8080
# Swagger UI en:     http://localhost:8080/swagger-ui.html
# Login inicial:     admin / Admin@2024!
# MailHog UI en:     http://localhost:8025
```

### Frontend

```bash
cd dentis-web
npm install
npm start
# Disponible en: http://localhost:4200
```

### Variables de Entorno

```bash
cp .env.example .env
# Editar .env con tus valores
```

Ver `.env.example` para la lista completa de variables.

### Probar en local con las imágenes ya publicadas

Si quieres arrancar el frontend y backend con las imágenes ya construidas (por ejemplo, las publicadas en ECR), usa `.env.local` junto con `docker-compose.dev.yml`.

Antes de arrancar, si las imágenes están en ECR, autentícate:

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 742671448563.dkr.ecr.us-east-1.amazonaws.com
```

O usando el helper que he dejado en el repo:

```bash
make ecr-login
```

Opciones rápidas:

```bash
# Wrapper directo
bash ./scripts/dev-compose.sh ecr-login
bash ./scripts/dev-compose.sh up

# O con Makefile
make ecr-login
make up
```

Comandos útiles:

```bash
make ps
make logs
make logs SERVICE=app
make reset-db
make down
```

Puertos locales por defecto en este modo:

- Frontend: `http://localhost:8081`
- Backend: `http://localhost:8080`
- MailHog: `http://localhost:8025`
- PostgreSQL: `localhost:5432`

### Base de datos de desarrollo

La base de datos de desarrollo se apoya en `docker-compose.yml` y en los changelogs Liquibase del módulo `dentis-api`.

- `postgres`: expone PostgreSQL 16 en `${DB_PORT}`
- `mailhog`: SMTP local para pruebas de correo en `localhost:1025`
- `liquibase`: aplica automáticamente los cambios versionados y crea las tablas del sistema

Liquibase usa el changelog maestro `dentis-api/src/main/resources/db/changelog/db.changelog-master.yaml`, que reutiliza los scripts SQL existentes:

- `V1__create_patients_table.sql`
- `V2__create_appointments_table.sql`
- `V3__create_clinical_tables.sql`
- `V4__create_billing_tables.sql`
- `V5__create_users_table.sql`

Una vez aplicados los cambios, la API arranca con `spring.jpa.hibernate.ddl-auto=validate`, de forma que Hibernate valida el esquema en lugar de regenerarlo. En esta implementación, Liquibase se ejecuta desde Docker Compose y mantiene sus tablas de control `DATABASECHANGELOG` y `DATABASECHANGELOGLOCK`.

Si necesitas reiniciar completamente el entorno local:

```bash
docker compose down -v
docker compose --profile dev up -d postgres mailhog
docker compose --profile dev run --rm liquibase
```

## Despliegue en la Nube

Ver [`infrastructure/DEPLOYMENT.md`](./infrastructure/DEPLOYMENT.md) para instrucciones detalladas de despliegue en AWS (ECS Fargate) y Azure (Container Apps) con Terraform.

## Tests

```bash
# Todos los módulos
mvn test

# Módulo específico
mvn test -pl dentis-patient
```

## Contribución

- Todo el código en inglés
- Principios SOLID y Clean Code
- Tests unitarios con JUnit 5 + Mockito para toda lógica de negocio
- Sin comentarios triviales — solo cuando el *por qué* no es obvio
