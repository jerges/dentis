# dentis-api

Punto de entrada de la aplicación. Orquesta todos los bounded contexts, expone la API REST, gestiona la seguridad JWT y el manejo global de errores.

## Responsabilidades

- Arranque de Spring Boot (`DentisApplication`)
- Seguridad: autenticación JWT stateless con Spring Security
- Controllers REST que delegan en los servicios de dominio
- Manejo global de excepciones (`@ControllerAdvice`)
- Configuración de CORS, OpenAPI/Swagger
- Migraciones de base de datos (Liquibase)

## Estructura

```
dentis-api/
└── src/main/java/.../api/
    ├── DentisApplication.java
    ├── config/
    │   ├── SecurityConfig.java      ← Spring Security + JWT filter chain
    │   ├── OpenApiConfig.java       ← Springdoc / Swagger
    │   └── WebConfig.java           ← CORS
    ├── controller/
    │   ├── AuthController.java      ← POST /auth/login + /auth/refresh
    │   ├── PatientController.java   ← /api/v1/patients
    │   ├── AppointmentController.java ← /api/v1/appointments
    │   └── BillingController.java   ← /api/v1/billing
    ├── exception/
    │   └── GlobalExceptionHandler.java
    └── security/
        ├── config/      ← SecurityConfig
        ├── entity/      ← UserEntity (Spring Security UserDetails)
        ├── filter/      ← JwtAuthenticationFilter
        ├── repository/  ← UserRepository
        └── service/     ← JwtService, UserDetailsServiceImpl
```

## Seguridad (JWT Stateless)

```
POST /api/v1/auth/login
  Body: { "username": "...", "password": "..." }
  Response: { "token": "<JWT>", "expiresIn": 86400000 }

Header en requests protegidos:
  Authorization: Bearer <JWT>
```

- Tokens firmados con HS256 (mínimo 256 bits)
- Expiración configurable via `JWT_EXPIRATION_MS` (default: 24h)
- Rutas públicas: `POST /api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`

## Endpoints REST

| Módulo | Base Path |
|---|---|
| Auth | `POST /api/v1/auth/login` |
| Patients | `/api/v1/patients` |
| Appointments | `/api/v1/appointments` |
| Billing | `/api/v1/billing` |

Ver [Swagger UI](http://localhost:8080/swagger-ui.html) para la documentación interactiva completa.

## Migraciones Liquibase

Liquibase usa el changelog maestro `src/main/resources/db/changelog/db.changelog-master.yaml` y reutiliza los scripts SQL versionados del directorio `src/main/resources/db/migration`.

| ChangeSet | Descripción |
|---|---|
| `001` | Tabla `patients` |
| `002` | Tabla `appointments` |
| `003` | Tablas clínicas (`clinical_records`, `odontogram_teeth`, etc.) |
| `004` | Tablas de facturación (`tariffs`, `budgets`, `payments`) |
| `005` | Tabla `users` (seguridad) |

## Inicio

```bash
# Levantar base de datos
docker compose --profile dev up -d postgres

# Aplicar cambios Liquibase
docker compose --profile dev run --rm liquibase

# Ejecutar con perfil dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# Login: admin / Admin@2024!
```

## Variables de Entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host de PostgreSQL |
| `DB_PORT` | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | `dentis_db` | Nombre de la base de datos |
| `DB_USER` | `dentis` | Usuario de PostgreSQL |
| `DB_PASSWORD` | `dentis` | Contraseña de PostgreSQL |
| `JWT_SECRET` | *(ver .env.example)* | Clave secreta para firmar tokens |
| `JWT_EXPIRATION_MS` | `86400000` | Expiración del token (ms) |
| `MAIL_HOST` | `localhost` | Host SMTP (MailHog en desarrollo) |
| `MAIL_PORT` | `1025` | Puerto SMTP |
| `MAIL_USERNAME` | — | Usuario SMTP |
| `MAIL_PASSWORD` | — | Contraseña SMTP |

