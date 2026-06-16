# Dentis — Plataforma de Gestión Dental

Software de gestión dental integral orientado a optimizar los procesos clínicos, administrativos y financieros de una consulta o clínica dental, centralizando la información en una sola plataforma.

**Mercado objetivo:** Venezuela

---

## Tabla de contenido

- [Requisitos del producto](#requisitos-del-producto)
- [Arquitectura técnica](#arquitectura-técnica)
- [Inicio rápido (desarrollo)](#inicio-rápido-desarrollo)
- [Migraciones de base de datos](#migraciones-de-base-de-datos)
- [Cuentas de usuario](#cuentas-de-usuario)
- [Modo demo](#modo-demo)
- [Variables de entorno](#variables-de-entorno)
- [Seguridad multi-tenant](#seguridad-multi-tenant)
- [Notificaciones por email](#notificaciones-por-email)
- [Despliegue en AWS](#despliegue-en-aws)

---

## Requisitos del producto

### Objetivos del proyecto

- Centralizar la agenda clínica por dentista.
- Digitalizar y organizar las historias clínicas de pacientes.
- Automatizar recordatorios y notificaciones de citas.
- Optimizar la gestión de presupuestos, aranceles y pagos.
- Mejorar el control financiero y seguimiento de tratamientos.
- Integrar funciones administrativas y clínicas en una sola herramienta.
- Gestionar documentos clínicos y administrativos con control de acceso por usuario.
- Ofrecer un asistente IA clínico basado en la base de conocimiento de la clínica.

### Módulos y funcionalidades

**1. Agenda Clínica por Profesional**
- Gestión de citas por dentista (vista semanal y diaria).
- Bloqueo y administración de horarios.
- Registro del motivo de consulta.
- Historial de citas por paciente.

**2. Notificaciones y Recordatorios Automáticos**
- Confirmación automática por correo al agendar.
- Recordatorios previos a la cita.
- Notificación al presentar presupuesto y al recibir pago.
- Extensión futura: WhatsApp, SMS.

**3. Historia Clínica Digital**
- Datos personales: nombre, documento de identidad (con opción extranjero alfanumérico), fecha de nacimiento, contacto, dirección.
- Representante: campo con opción "No aplica" que oculta los campos asociados.
- Odontodiagrama interactivo con condiciones por diente y por cara:
  - Condiciones: ausente, caries, restauración, malformación, implante, resto radicular, fractura, obturación defectuosa.
  - Diente ausente: selector de estado de espacio (abierto / parcialmente cerrado / cerrado).
  - Texto resumen asociable a cada diente.
  - Dentición permanente, primaria y mixta (`dentitionType`).
  - Denominación correcta: "maxilar inferior" (no mandíbula).
- Evoluciones clínicas, diagnósticos, planes de tratamiento.

**4. Gestión de Aranceles y Presupuestos**
- Catálogo de aranceles por categoría (ortodoncia, cirugía, general, laboratorio, etc.).
- Descuentos por prestación o convenio.
- Presupuestos vinculados a planes de tratamiento.
- Marcado de procedimientos como realizados.

**5. Gestión de Pagos y Control Financiero**
- Registro de pagos (monto, método, referencia de factura manual).
- Abonos parciales; seguimiento de saldo pendiente.
- Estados: pagado / parcialmente pagado / pendiente.

**6. Valor agregado (futuras integraciones)**
- Reportes financieros e indicadores por profesional.
- Firma electrónica y consentimientos digitales.
- Imágenes radiográficas e integración de laboratorio.
- Módulo de inventario e insumos.
- Dashboard con indicadores de morosidad, tratamientos activos y ocupación de agenda.

**7. Gestión Documental**
- Carpetas y ficheros por clínica almacenados en AWS S3.
- Visibilidad configurable por recurso: `PUBLIC` (toda la clínica), `PRIVATE` (solo el creador), `SHARED` (creador + usuarios explícitos).
- Compartir ficheros y carpetas entre usuarios de la misma clínica.
- Búsqueda rápida por nombre (trigrama `pg_trgm`) y contenido (full-text `tsvector` en español).
- Carpeta especial **Base de Conocimiento** por clínica: protegida, no borrable, solo accesible por el ADMIN.

**8. Asistente IA Clínico**
- RAG (Retrieval-Augmented Generation) sobre la Base de Conocimiento de la clínica.
- Generación con Amazon Bedrock Nova Pro; embeddings Titan v2 (1024 dimensiones).
- Búsqueda semántica en pgvector, acotada a documentos con metadato `kb=true`.
- Respuestas con citas a la fuente del documento.
- Guardia de relevancia: el asistente solo responde temática odontológica.
- Historial de conversaciones por sesión y dentista.

---

## Arquitectura técnica

### Stack

| Capa | Tecnología |
|------|-----------|
| Backend | Java 25 · Spring Boot 4.0.6 · Maven multi-módulo |
| Base de datos | PostgreSQL 16 · Liquibase (migraciones) · pgvector · pg_trgm |
| Frontend | Angular 18 (standalone) · Angular Material · Signals |
| Autenticación | JWT (HS256) |
| IA / RAG | Amazon Bedrock (Nova Pro + Titan v2) · Spring AI · pgvector |
| Almacenamiento | AWS S3 (presigned URLs · documentos + adjuntos clínicos) |
| Email | JavaMailSender · Thymeleaf templates |
| Documentación API | SpringDoc / Swagger UI |
| Infraestructura | AWS EC2 · RDS · S3 · Secrets Manager · Bedrock |
| Dev local | Docker Compose |

### Módulos Maven

```
dentis/
├── dentis-common        # DTOs compartidos, ApiResponse, excepciones
├── dentis-patient       # Dominio y repositorio de pacientes
├── dentis-scheduling    # Agenda y citas
├── dentis-clinical      # Historia clínica y odontodiagrama
├── dentis-billing       # Aranceles, presupuestos y pagos
├── dentis-notification  # Envío de emails (Thymeleaf)
├── dentis-clinic        # Clínicas y usuarios del sistema
├── dentis-ia            # Asistente IA (RAG, Bedrock, pgvector, ingesta)
├── dentis-documents     # Gestión documental y Base de Conocimiento
└── dentis-api           # Controllers REST, seguridad JWT, entry point
```

La arquitectura sigue el patrón hexagonal: los módulos de dominio (`dentis-patient`, `dentis-billing`, `dentis-documents`, etc.) no dependen de `dentis-api`. Los controllers en `dentis-api` actúan como adaptadores de entrada.

---

## Inicio rápido (desarrollo)

**Requisitos:** Docker, Java 25, Node 20+, Maven 3.9+

```bash
# 1. Levantar PostgreSQL
docker-compose up -d postgres

# 2. Backend (perfil dev — aplica migraciones automáticamente al iniciar)
cd dentis-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend
cd dentis-web
npm install
npx ng serve
```

La aplicación queda disponible en:
- Frontend: http://localhost:4200
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

---

## Migraciones de base de datos

El proyecto usa **Liquibase** (integrado en Spring Boot). Las migraciones se aplican automáticamente al arrancar la aplicación.

### Estructura de changelogs

```
dentis-api/src/main/resources/db/changelog/
├── db.changelog-master.yaml              # Índice principal
└── changes/
    ├── 001-baseline.sql                  # Esquema completo + usuarios iniciales
    ├── 002-clinical-enhancements.sql     # dentitionType, spaceStatus en odontodiagrama
    ├── 003-demo-data.sql                 # Clínica y usuario demo (context: demo, qa)
    ├── 004-odontogram-root-and-tooth-dx.sql  # Diagnósticos por diente y raíz
    ├── 005-clinical-attachments.sql      # Adjuntos clínicos en S3
    ├── 006-ia-vector.sql                 # Extensión pgvector + tabla ia_documents
    ├── 007-ia-spring-ai-vector.sql       # Columnas compatibles Spring AI VectorStore
    └── 008-documents.sql                 # Gestión documental (pg_trgm, carpetas, ficheros, KB)
```

### Entornos y contextos

> **Estrategia actual:** el entorno `dev` (local y AWS) siempre carga los datos demo.
> Cuando se quiera desplegar producción real en AWS, se pasará el parámetro `pro` para omitirlos.
> Hasta entonces, todos los despliegues usan el contexto `demo`.

| Contexto Liquibase | Changelogs aplicados | Cuándo se usa |
|-------------------|---------------------|---------------|
| `demo` *(por defecto en dev)* | `001`, `002`, `003` | Local + AWS mientras se desarrolla |
| `pro` | `001`, `002` | AWS producción real (a configurar más adelante) |
| `qa` | `001`, `002`, `003` | Pruebas QA automatizadas |

El perfil `dev` ya tiene el contexto configurado en `application-dev.yml`:

```yaml
spring:
  liquibase:
    contexts: demo   # carga 001 + 002 + 003-demo-data.sql
```

Para desplegar en **producción real en AWS** cuando llegue el momento, pasar:

```bash
# Variable de entorno en el task definition de ECS
SPRING_LIQUIBASE_CONTEXTS=pro   # omite 003-demo-data.sql
```

### Recrear la base de datos desde cero (local)

```bash
docker-compose down -v          # destruye el volumen con datos
docker-compose up -d postgres   # recrea PostgreSQL vacío
# Reiniciar la aplicación — Liquibase aplica todo desde cero
```

---

## Cuentas de usuario

### Usuarios predefinidos (001-baseline.sql)

Estos usuarios se crean en **todos los entornos** (incluyendo producción) porque forman parte del changelog base.

| Usuario | Contraseña | Rol | Descripción |
|---------|-----------|-----|-------------|
| `admin` | `Admin@2024!` | `SUPER_ADMIN` | Administrador global del sistema, acceso a todas las clínicas |
| `jbello` | `Admin@2026!` | `SUPER_ADMIN` | Cuenta de administrador del desarrollador |

> **SUPER_ADMIN** no está vinculado a ninguna clínica (`clinicId = null`). Tiene acceso irrestricto a todos los datos.

### Usuario demo (003-demo-data.sql)

Creado únicamente cuando se activa el contexto `demo` o `qa`.

| Campo | Valor |
|-------|-------|
| Usuario | `demo` |
| Contraseña | `Demo@2026!` |
| Rol | `ADMIN` |
| Clínica | Clínica Dental Demo |
| Ciudad | Caracas, Venezuela |
| Clínica ID | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |

El usuario `demo` solo puede ver y operar los datos de su propia clínica (restricción multi-tenant). Es la cuenta ideal para demos con clientes o para pruebas en AWS sin exponer las cuentas de administrador.

---

## Modo demo

El modo demo carga una clínica preconfigurada con datos realistas para mostrar todas las funcionalidades del sistema. **Está activo por defecto en el perfil `dev`**, tanto en local como en AWS mientras se desarrolla.

### Datos incluidos en el contexto `demo`

- **Clínica:** Clínica Dental Demo (Caracas, Venezuela)
- **Dentista:** `dra.garcia` / `Demo@2026!` — Dra. María García (rol USER, DENTIST)
- **Paciente:** Carlos Rodríguez (V-18450321) con historia clínica completa:
  - Odontograma con 3 dientes (pieza 16 restaurada, pieza 36 con caries, pieza 18 extraída)
  - Evolución clínica, diagnóstico K04.0 (pulpitis irreversible)
  - Plan de tratamiento en progreso
- **Citas:** 1 completada (hace 10 días) + 1 programada (en 7 días)
- **Presupuesto:** APPROVED por $400 con abono de $200 en efectivo
- **Aranceles:** Limpieza ($45), Restauración resina ($120), Conducto unirradicular ($280)

### Credenciales de acceso para demo

```
URL:        https://<tu-dominio>/
Admin demo: demo / Demo@2026!        (ADMIN, solo ve su clínica)
Dentista:   dra.garcia / Demo@2026!  (USER/DENTIST, solo ve su clínica)
Super admin: jbello / Admin@2026!    (SUPER_ADMIN, acceso total)
```

### Activar / desactivar según entorno

El perfil `dev` ya activa el contexto automáticamente (`application-dev.yml`).
Para AWS el task definition debe incluir el perfil correcto:

```bash
# AWS dev / demo (datos demo incluidos) — configuración actual
SPRING_PROFILES_ACTIVE=dev
# o explícitamente:
SPRING_LIQUIBASE_CONTEXTS=demo

# AWS producción real (sin datos demo) — a configurar más adelante
SPRING_LIQUIBASE_CONTEXTS=pro
```

> ⚠️ **Antes de pasar a producción real:** cambiar las contraseñas de `demo`, `dra.garcia`, `admin` y `jbello`, o eliminar esos usuarios del changelog base.

---

## Variables de entorno

### Obligatorias en producción

| Variable | Descripción | Default (solo dev) |
|----------|-------------|-------------------|
| `DB_HOST` | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `dentis_db` |
| `DB_USER` | Usuario de PostgreSQL | `dentis` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `dentis` |
| `JWT_SECRET` | Secreto para firmar tokens JWT (mín. 256 bits) | *insecuro — bloqueado en prod* |
| `JWT_EXPIRATION_MS` | Duración del token en ms | `86400000` (24 h) |
| `MAIL_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Puerto SMTP | `587` |
| `MAIL_USERNAME` | Cuenta de correo remitente | — |
| `MAIL_PASSWORD` | Contraseña / App password del correo | — |
| `SPRING_LIQUIBASE_CONTEXTS` | Contextos de migración (`demo`, `qa`, o vacío) | — |

### JWT_SECRET obligatorio fuera de dev

Al arrancar en cualquier perfil que no sea `dev`, la aplicación **falla con error** si `JWT_SECRET` tiene el valor por defecto inseguro. Esto previene deployments accidentales sin secreto configurado.

```
IllegalStateException: JWT_SECRET environment variable must be set to a secure value
in non-dev environments.
```

Generar un secreto seguro:

```bash
openssl rand -base64 64
```

---

## Seguridad multi-tenant

Cada usuario (salvo `SUPER_ADMIN`) está vinculado a una clínica. El backend aplica aislamiento de datos automáticamente en todos los endpoints:

- **Pacientes**: un usuario solo puede buscar, listar y acceder a pacientes de su clínica.
- **Historia clínica**: se valida que el paciente pertenezca a la clínica del usuario antes de cualquier operación.
- **Facturación**: los presupuestos y pagos solo son accesibles si el paciente asociado pertenece a la clínica.
- **SUPER_ADMIN**: sin restricción de clínica, accede a todos los datos.

El aislamiento se implementa en los controllers mediante `TenantSecurityService`, que extrae el `clinicId` del token JWT y lo aplica a cada consulta.

---

## Notificaciones por email

Los siguientes eventos disparan un correo automático al paciente:

| Evento | Template |
|--------|---------|
| Cita agendada | `APPOINTMENT_CONFIRMATION` |
| Cita cancelada | `APPOINTMENT_CANCELLATION` |
| Presupuesto presentado | `BUDGET_PRESENTED` |
| Pago recibido | `PAYMENT_RECEIVED` |

Los templates HTML están en `dentis-notification/src/main/resources/templates/email/`.

Para probar notificaciones en desarrollo, usar [MailHog](https://github.com/mailhog/MailHog):

```bash
docker-compose --profile mail up -d   # si está configurado en docker-compose.yml
# UI disponible en http://localhost:8025
```

---

## Despliegue en AWS

La infraestructura está definida en Terraform bajo `infrastructure/terraform/aws/`. Consulta [`infrastructure/DEPLOYMENT.md`](infrastructure/DEPLOYMENT.md) para el proceso completo.

> **Estrategia de despliegue actual:** todos los despliegues en AWS usan el perfil `dev` con contexto `demo`.
> El paso a producción real se hará más adelante pasando `SPRING_LIQUIBASE_CONTEXTS=pro` como parámetro.

Resumen rápido:

```bash
cd infrastructure/terraform/aws
terraform init
terraform plan -var-file="terraform.tfvars"
terraform apply

# Deploy de la imagen
cd infrastructure/scripts
./build-backend.sh
./deploy.sh
```

Los secretos (`DB_PASSWORD`, `JWT_SECRET`, credenciales SMTP, claves AWS Bedrock/S3) se gestionan con **AWS Secrets Manager**.

### Parámetros de entorno clave

| Variable | Dev / Demo (actual) | Producción (futuro) |
|----------|--------------------|--------------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` (a crear) |
| `SPRING_LIQUIBASE_CONTEXTS` | `demo` | `pro` |
| `JWT_SECRET` | Secrets Manager | Secrets Manager |
| `AWS_S3_BUCKET` | bucket S3 dev | bucket S3 prod |
| `AWS_BEDROCK_REGION` | `us-east-1` | `us-east-1` |

### Asistente IA — requisitos adicionales

- La cuenta AWS debe tener acceso habilitado en Amazon Bedrock para los modelos **Nova Pro** (generación) y **Titan Embeddings v2** (embeddings).
- El bucket S3 debe permitir presigned URLs (sin ACL pública). La política de bucket debe denegar acceso público directo.
- La extensión `pgvector` debe estar habilitada en la instancia RDS (`CREATE EXTENSION IF NOT EXISTS vector`).
- La extensión `pg_trgm` debe estar habilitada (`CREATE EXTENSION IF NOT EXISTS pg_trgm`) — aplicada automáticamente por la migración `008-documents.sql`.
