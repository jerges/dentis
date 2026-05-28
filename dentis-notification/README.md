# dentis-notification

Bounded context de notificaciones. Gestiona el envío de emails transaccionales asociados a eventos del ciclo de vida de una cita dental (confirmación, recordatorio, cancelación).

## Capas

```
dentis-notification/
└── src/main/java/.../notification/
    ├── domain/
    │   ├── model/          ← NotificationEvent, NotificationType
    │   ├── repository/     ← Contrato de repositorio de notificaciones
    │   └── service/        ← NotificationService — lógica de envío
    ├── application/
    │   ├── dto/            ← NotificationRequest
    │   └── event/          ← Listeners de eventos de dominio
    └── infrastructure/
        ├── email/          ← JavaMailSender adapter
        └── persistence/    ← Registro de notificaciones enviadas
```

## Responsabilidades

- Envío de **email de confirmación** cuando una cita pasa a `CONFIRMED`.
- Envío de **recordatorio** 24 horas antes de la cita.
- Envío de **notificación de cancelación** cuando una cita es cancelada.
- Registro de cada notificación enviada (para auditoría y reenvío).

## Configuración SMTP

Las credenciales se configuran a través de variables de entorno (ver `.env.example`):

```yaml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
```

Para desarrollo local, se usa **MailHog** como servidor SMTP falso:

```bash
docker-compose --profile dev up -d mailhog
# UI disponible en: http://localhost:8025
```

## Tipos de Notificación

| Tipo | Trigger | Destinatario |
|---|---|---|
| `APPOINTMENT_CONFIRMED` | Cita confirmada | Paciente |
| `APPOINTMENT_REMINDER` | 24h antes del turno | Paciente |
| `APPOINTMENT_CANCELLED` | Cita cancelada | Paciente |
| `APPOINTMENT_RESCHEDULED` | Cita reagendada | Paciente |

## Dependencias

- `dentis-common` — respuestas y excepciones base
- `spring-boot-starter-mail` — JavaMailSender
- `spring-boot-starter-data-jpa` — registro de notificaciones

