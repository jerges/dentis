# dentis-scheduling

Bounded context de agenda clínica. Gestiona citas por profesional con detección de conflictos de horario, visualización por rango de fechas y ciclo de vida completo de la cita.

## Modelo de Dominio

```
Appointment
├── id: UUID
├── patientId: UUID
├── dentistId: UUID
├── startDateTime / endDateTime: LocalDateTime
├── status: AppointmentStatus
├── consultationReason: String
└── notes: String

AppointmentStatus: SCHEDULED → CONFIRMED → IN_PROGRESS → COMPLETED
                                        └→ CANCELLED
                                        └→ NO_SHOW

TimeBlock (bloqueos de agenda)
├── dentistId: UUID
├── startDateTime / endDateTime: LocalDateTime
├── reason: String
└── type: TimeBlockType (LUNCH_BREAK, VACATION, PERSONAL, ADMINISTRATIVE, OTHER)
```

## Reglas de Negocio

- No se pueden crear citas con `endDateTime <= startDateTime`.
- No se pueden crear citas que se solapen con otra cita activa del mismo dentista.
- Solo citas en estado `SCHEDULED` pueden ser `CONFIRMED`.
- Una cita `COMPLETED` no puede cancelarse.
- Al reprogramar, se verifica de nuevo la disponibilidad (excluyendo la cita actual del chequeo de conflictos).

## API (expuesta por dentis-api)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/appointments` | Agendar cita |
| `GET` | `/api/v1/appointments/{id}` | Obtener cita |
| `GET` | `/api/v1/appointments/dentist/{id}?from=&to=` | Agenda del dentista por rango |
| `GET` | `/api/v1/appointments/patient/{id}` | Citas del paciente |
| `PATCH` | `/api/v1/appointments/{id}/confirm` | Confirmar cita |
| `PATCH` | `/api/v1/appointments/{id}/cancel` | Cancelar cita |
| `PATCH` | `/api/v1/appointments/{id}/reschedule?newStart=&newEnd=` | Reprogramar |

## Integración con Notificaciones

Al crear o modificar una cita, el módulo `dentis-notification` envía automáticamente emails de confirmación y recordatorio al paciente (usando el email registrado en su ficha).
