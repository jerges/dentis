# dentis-patient

Bounded context de gestión de pacientes. Centraliza toda la información demográfica, de contacto y legal del paciente.

## Capas

```
dentis-patient/
└── src/main/java/.../patient/
    ├── domain/
    │   ├── model/          ← Entidades de dominio puras (no JPA)
    │   ├── repository/     ← Interfaces de repositorio (contratos)
    │   └── service/        ← PatientService — lógica de negocio
    ├── application/
    │   ├── dto/            ← Request/Response DTOs con validaciones
    │   ├── mapper/         ← PatientMapper (MapStruct)
    │   └── usecase/        ← Interfaz PatientUseCase
    └── infrastructure/
        └── persistence/
            ├── entity/     ← PatientEntity (JPA)
            ├── repository/ ← JPA repo + Adapter (implementa dominio)
            └── mapper/     ← PatientEntityMapper (MapStruct)
```

## Modelo de Dominio

```
Patient
├── id: UUID
├── firstName / lastName: String
├── idDocument: String          (único, ej: V-12345678)
├── birthDate: LocalDate
├── sex: Sex                    (MALE, FEMALE, INTERSEX, NOT_SPECIFIED)
├── gender: Gender              (MALE, FEMALE, NON_BINARY, OTHER, NOT_SPECIFIED)
├── socialName: String          (nombre social opcional)
├── contactInfo: ContactInfo    (email, phoneNumber, alternativePhone)
├── address: Address            (street, city, state, zipCode)
├── representative: Representative  (para pacientes menores)
└── active: boolean
```

## Reglas de Negocio

- El `idDocument` debe ser único en el sistema.
- Si el paciente es menor de edad (`isMinor() == true`), se recomienda registrar un `Representative`.
- La desactivación es lógica (soft delete) — el registro se conserva para auditoría.

## API (expuesta por dentis-api)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/patients` | Registrar nuevo paciente |
| `GET` | `/api/v1/patients/{id}` | Obtener paciente por ID |
| `GET` | `/api/v1/patients` | Listar pacientes (paginado) |
| `GET` | `/api/v1/patients/search?name=` | Buscar por nombre |
| `PUT` | `/api/v1/patients/{id}` | Actualizar datos |
| `DELETE` | `/api/v1/patients/{id}` | Desactivar paciente |

## Tests

```bash
mvn test -pl dentis-patient
```

Cobertura: `PatientService` — creación, búsqueda, duplicados, desactivación.
