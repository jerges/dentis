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
- [Asistente IA — detalle técnico](#asistente-ia--detalle-técnico)
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
- **Streaming exclusivo vía SSE**: toda la comunicación con el agente es en tiempo real (no hay endpoint bloqueante).
- **Tool use (function calling)**: el agente puede invocar herramientas externas e internas en bucle multi-turno (hasta 8 iteraciones) con Bedrock Converse Stream API.
- **Trazabilidad de fuentes obligatoria**: el agente indica siempre de dónde proviene cada dato (PMID, RxNorm, FDA, calculadora clínica, documentos de la clínica o conocimiento propio).

**Herramientas del agente (todas con fuentes oficiales gratuitas):**

| Herramienta | Fuente | Tipo |
|---|---|---|
| `pubmed_search` | NCBI / NIH E-utilities | API externa |
| `drug_interaction_check` | RxNorm / NIH NLM | API externa |
| `icd10_lookup` | NIH Clinical Tables (ICD-10-CM) | API externa |
| `fda_drug_info` | OpenFDA / FDA (api.fda.gov) | API externa |
| `dosage_calculator` | ADA 2024 / Farmacopea (datos embebidos) | Cálculo interno |
| `lab_values_interpreter` | ADA / ACC-AHA / SEPA (datos embebidos) | Cálculo interno |

`dosage_calculator` cubre anestésicos locales (lidocaína, articaína, mepivacaína, bupivacaína) y antibióticos dentales (amoxicilina, metronidazol, clindamicina, azitromicina) con ajustes por peso, edad y comorbilidades.

`lab_values_interpreter` evalúa INR/TP/plaquetas, glucosa/HbA1c, creatinina, ALT/AST, hemoglobina, leucocitos/neutrófilos y tensión arterial frente a umbrales de seguridad para procedimientos dentales.

**Eventos SSE emitidos al frontend:**

| Evento | Cuándo |
|---|---|
| `{"t": "..."}` | Fragmento de texto generado por el modelo |
| `{"tool": "...", "status": "searching", "label": "..."}` | Herramienta externa iniciada (consulta a API) |
| `{"tool": "...", "status": "tooling",   "label": "..."}` | Herramienta interna iniciada (cálculo local) |
| `{"tool": "...", "status": "done",      "label": "..."}` | Herramienta completada |
| `{"done": true, "usage": {...}}` | Respuesta completa con métricas de tokens y coste |
| `{"err": "..."}` | Error durante la generación |

---

## Arquitectura técnica

### Stack

| Capa | Tecnología |
|------|-----------|
| Backend | Java 25 · Spring Boot 4.0.6 · Maven multi-módulo · Virtual Threads |
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

### Concurrencia: Virtual Threads (Java 25)

El proyecto adopta Virtual Threads en dos niveles:

- **HTTP requests** (`spring.threads.virtual.enabled=true`): cada petición REST se despacha en un hilo virtual, eliminando el cuello de botella del pool de hilos del servidor.
- **Envío de email** (`NotificationAsyncConfig`): el envío SMTP se ejecuta en un hilo virtual independiente vía `@Async("notificationExecutor")`, sin bloquear al llamador.
- **Streaming IA** (`IaController`): cada sesión SSE crea un hilo virtual con `Thread.ofVirtual()` que mantiene la conexión abierta mientras el agente genera la respuesta.

Esto permite manejar muchas conexiones SSE simultáneas (una por conversación activa) sin agotar el pool de Tomcat.

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

El envío es **asíncrono y no bloqueante**: se ejecuta en un hilo virtual dedicado (`SimpleAsyncTaskExecutor` con `virtualThreads = true`). Si el envío falla, el error queda trazado en el log sin interrumpir la operación que lo originó.

Para probar notificaciones en desarrollo, usar [MailHog](https://github.com/mailhog/MailHog):

```bash
docker-compose --profile dev up -d mailhog
# UI disponible en http://localhost:8025
```

---

## Asistente IA — detalle técnico

### Flujo de una conversación

```
POST /api/v1/ia/chat/sessions/{id}/stream
        │
        ▼ (hilo virtual)
IaChatService.streamMessage()
        │  guarda mensaje usuario
        │  carga historial
        ▼
DentalAgent.streamAsk()
        │  RelevanceGuard — rechaza preguntas off-topic
        ▼
BaseAgent.streamAsk()   [bucle multi-turno, máx. 8 iteraciones]
        │
        ├─► ConverseStream → texto → SSE {"t": "..."}
        │
        ├─► stop_reason = tool_use
        │       ├─► emite SSE {"tool":"...", "status":"searching"/"tooling", "label":"..."}
        │       ├─► ejecuta herramienta
        │       ├─► emite SSE {"tool":"...", "status":"done", "label":"..."}
        │       └─► envía tool_result a Bedrock → siguiente turno
        │
        └─► stop_reason = end_turn
                └─► SSE {"done": true, "usage": {...}}
```

### Añadir una nueva herramienta

Implementar la interfaz `AgentTool` en `dentis-ia` y anotarla con `@Component`:

```java
@Component
public class MiHerramienta implements AgentTool {
    @Override public String name()        { return "mi_herramienta"; }
    @Override public String description() { return "Descripción para el modelo..."; }
    @Override public String label()       { return "Ejecutando..."; }
    @Override public boolean isExternal() { return false; } // true si llama a una API externa
    @Override public Document inputSchema() { /* JSON Schema como AWS Document */ }
    @Override public String execute(Map<String, Object> input) { /* lógica */ }
}
```

Spring la inyecta automáticamente en `DentalAgent` vía `List<AgentTool>`. No hay ningún otro registro manual.

### Protocolo SSE

Todos los eventos llegan como `data:` en texto plano sobre una conexión `text/event-stream`:

```
data: {"t":"La caries dental"}
data: {"t":" es una enfermedad..."}
data: {"tool":"pubmed_search","status":"searching","label":"Buscando en PubMed..."}
data: {"tool":"pubmed_search","status":"done","label":"Buscando en PubMed..."}
data: {"t":"Según PMID:38291045..."}
data: {"done":true,"usage":{"input_tokens":312,"output_tokens":487,"cost_usd":0.00181}}
```

---

## Despliegue en AWS

La infraestructura está definida en Terraform bajo `infrastructure/terraform/aws/dev-ec2/`. Consulta [`infrastructure/DEPLOYMENT.md`](infrastructure/DEPLOYMENT.md) para el proceso completo.

> **Estrategia de despliegue actual:** EC2 `t4g.small` (ARM Graviton2) en `us-east-1` con Docker Compose.
> Todos los despliegues usan el perfil `dev` con contexto `demo`.
> El paso a producción real se hará pasando `SPRING_LIQUIBASE_CONTEXTS=pro`.

Resumen rápido:

```bash
# Primer deploy (provisiona EC2, construye imágenes y arranca servicios)
./infrastructure/scripts/deploy.sh

# Solo rebuild de imágenes (cuando hay cambios en el código)
DOCKER_PLATFORMS=linux/arm64 ./infrastructure/scripts/build-backend.sh
DOCKER_PLATFORMS=linux/arm64 ./infrastructure/scripts/build-web.sh

# Deploy de la landing page a CloudFront
./infrastructure/scripts/deploy-landing.sh
```

Los secretos en dev se gestionan mediante el archivo `.env.ec2` (generado automáticamente por `deploy.sh` a partir de los outputs de Terraform). En producción deben usarse **AWS Secrets Manager**.

### Recursos AWS activos

| Recurso | Identificador | URL |
|---------|--------------|-----|
| EC2 t4g.small | `i-06cdd459dff85e0db` — `18.210.9.9` | — |
| ECR backend | `dentis-dev-backend:latest` | — |
| ECR frontend | `dentis-dev-web:latest` | — |
| S3 adjuntos | `dentis-dev-attachments-742671448563` | — |
| Landing CloudFront | `E2Y4ZFSUC6AGL2` | `https://d3tv842cpzfh1w.cloudfront.net` |

### Variables de entorno IA (Bedrock)

| Variable | Valor actual |
|----------|-------------|
| `IA_ENABLED` | `true` |
| `IA_GEN_MODEL` | `us.amazon.nova-pro-v1:0` |
| `IA_EMBED_MODEL` | `amazon.titan-embed-text-v2:0` |
| `IA_MIN_SCORE` | `0.72` |
| `IA_CHUNK_SIZE` | `800` |
| `S3_ATTACHMENTS_BUCKET` | `dentis-dev-attachments-742671448563` |

### Requisitos de la cuenta AWS

- Acceso habilitado en Amazon Bedrock para **Nova Pro** (generación) y **Titan Embeddings v2** (embeddings).
- El bucket S3 sin ACL pública; presigned URLs generadas por la aplicación.
- Extensiones `pgvector` y `pg_trgm` activas en PostgreSQL — se instalan automáticamente por Liquibase (changelogs 006 y 008).
