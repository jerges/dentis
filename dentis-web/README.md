# dentis-web

Frontend SPA del backoffice de Dentis. Desarrollado con **Angular 18** (standalone components), **Angular Material 18** y **RxJS 7**.

## Stack

| Tecnología | Versión |
|---|---|
| Angular | 18 |
| Angular Material | 18 |
| TypeScript | 5.4 |
| RxJS | 7.8 |
| Node.js (mínimo) | 20 |

## Estructura

```
dentis-web/
└── src/
    ├── app/
    │   ├── app.config.ts           ← Providers (router, HTTP, animations)
    │   ├── app.routes.ts           ← Rutas raíz con lazy loading
    │   ├── app.component.ts        ← Shell con <router-outlet>
    │   ├── core/
    │   │   ├── models/             ← Interfaces TypeScript (Patient, Appointment…)
    │   │   ├── services/           ← AuthService, PatientService, etc.
    │   │   ├── interceptors/       ← JwtInterceptor (añade Bearer token)
    │   │   └── guards/             ← AuthGuard (protege rutas privadas)
    │   ├── features/
    │   │   ├── auth/login/         ← Formulario de login
    │   │   ├── dashboard/          ← Métricas y resumen ejecutivo
    │   │   ├── patients/           ← Lista, detalle y formulario de pacientes
    │   │   ├── scheduling/         ← Calendario de citas y formulario
    │   │   ├── clinical/           ← Historia clínica y odontograma
    │   │   └── billing/            ← Presupuestos, aranceles y pagos
    │   └── shared/
    │       ├── components/         ← PageHeader, LoadingSpinner, ConfirmDialog
    │       ├── directives/         ← Directivas reutilizables
    │       └── pipes/              ← Pipes de formato
    ├── assets/
    │   └── icons/
    └── environments/
        ├── environment.ts          ← Dev (apiUrl: localhost:8080)
        └── environment.prod.ts     ← Prod (apiUrl: variable de entorno)
```

## Inicio Rápido

```bash
# Instalar dependencias
npm install

# Servidor de desarrollo (proxy → backend :8080)
npm start
# Disponible en: http://localhost:4200

# Build de producción
npm run build
# Output: dist/dentis-web/
```

## Rutas de la Aplicación

| Ruta | Componente | Acceso |
|---|---|---|
| `/login` | `LoginComponent` | Público |
| `/dashboard` | `DashboardComponent` | Autenticado |
| `/patients` | `PatientsListComponent` | Autenticado |
| `/patients/new` | `PatientFormComponent` | Autenticado |
| `/patients/:id` | `PatientDetailComponent` | Autenticado |
| `/patients/:id/edit` | `PatientFormComponent` | Autenticado |
| `/scheduling` | `AppointmentCalendarComponent` | Autenticado |
| `/scheduling/new` | `AppointmentFormComponent` | Autenticado |
| `/clinical/:patientId` | `ClinicalRecordComponent` | Autenticado |
| `/clinical/:patientId/odontogram` | `OdontogramComponent` | Autenticado |
| `/billing/budgets` | `BudgetsComponent` | Autenticado |
| `/billing/tariffs` | `TariffsComponent` | Autenticado |
| `/billing/payments` | `PaymentsComponent` | Autenticado |

## Autenticación

El `JwtInterceptor` inyecta automáticamente el header `Authorization: Bearer <token>` en todas las peticiones HTTP. El token se almacena en `localStorage`. El `AuthGuard` redirige a `/login` si no hay sesión activa.

## Variables de Entorno Angular

Configurar en `src/environments/environment.ts` (dev) y `environment.prod.ts` (prod):

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

## Build Docker

```bash
# Desde la raíz del proyecto
docker build -f infrastructure/docker/Dockerfile.web -t dentis-web .
```

