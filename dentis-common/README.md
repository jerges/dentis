# dentis-common

Módulo de utilidades compartidas. No contiene lógica de negocio — solo infraestructura transversal reutilizable por todos los bounded contexts.

## Contenido

### Excepciones

| Clase | HTTP | Uso |
|---|---|---|
| `DentisException` | configurable | Base de todas las excepciones del dominio |
| `ResourceNotFoundException` | 404 | Entidad no encontrada por ID |
| `BusinessRuleException` | 422 | Violación de regla de negocio |

### Respuestas API

| Clase | Descripción |
|---|---|
| `ApiResponse<T>` | Wrapper estándar `{ success, data, error, timestamp }` |
| `ApiError` | Detalle de error con `code`, `message` y `fieldErrors` |
| `PageResponse<T>` | Wrapper de paginación construido desde `Page<T>` de Spring |

### Utilidades

| Clase | Descripción |
|---|---|
| `DateUtils` | `calculateAge(birthDate)`, `isAdult(birthDate)` |

## Uso

```java
// Lanzar error de negocio
throw new BusinessRuleException("Paciente ya existe", "DUPLICATE_ID_DOCUMENT");

// Lanzar not found
throw new ResourceNotFoundException("Patient", patientId);

// Construir respuesta exitosa
return ApiResponse.ok(patientResponse);

// Construir respuesta paginada
return ApiResponse.ok(PageResponse.from(page));
```

## Dependencias

Este módulo solo depende de `spring-boot-starter-validation` y `spring-boot-starter-web` (opcional). No debe agregar dependencias de persistencia ni de negocio.
