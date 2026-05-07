# Clinics and My Clinic Feature

Standalone Angular feature to manage clinics (super admin) and clinic users (clinic admin).

## Implemented screens

- `ClinicsListComponent` (`/clinics`)
    - paginated clinic listing
    - deactivate clinic action
    - navigation to edit
- `ClinicFormComponent` (`/clinics/new`, `/clinics/:id/edit`)
    - create and update clinic
- `SuperAdminFormComponent` (`/clinics/new-super-admin`)
    - create global `SUPER_ADMIN` without clinic association
- `ClinicUsersComponent` (`/clinics/:clinicId/users`, `/my-clinic/users`)
    - list clinic users
    - create `ADMIN` / `USER`
    - assign `DENTIST` or `ADMINISTRATIVE` staff type
    - deactivate user

## Services and models

- `core/services/clinic.service.ts`
- `core/models/clinic.model.ts`

## Access control

Routes are protected with `authGuard` and role metadata:

- `SUPER_ADMIN` can access clinics admin routes (`/clinics`, `/clinics/new`, `/clinics/new-super-admin`, `/clinics/:id/edit`) and clinic users per clinic (`/clinics/:clinicId/users`).
- `ADMIN` can access `Mi Clínica` route (`/my-clinic/users`) for users in their own clinic.

## Quick local check

```bash
cd dentis-web
npm run build -- --configuration development
```

