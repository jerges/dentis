# Clinics Admin Feature

Standalone Angular feature to manage clinics and clinic users.

## Implemented screens

- `ClinicsListComponent` (`/clinics`)
  - paginated clinic listing
  - deactivate clinic action
  - navigation to edit and users
- `ClinicFormComponent` (`/clinics/new`, `/clinics/:id/edit`)
  - create and update clinic
- `ClinicUsersComponent` (`/clinics/:clinicId/users`)
  - list clinic users
  - create `ADMIN` / `MEDICO`
  - deactivate user

## Services and models

- `core/services/clinic.service.ts`
- `core/models/clinic.model.ts`

## Access control

Routes are protected with `authGuard` and role metadata:

- `SUPER_ADMIN`, `ADMIN` can access clinics admin routes.
- Navigation item `Admin Clínicas` is shown only for `SUPER_ADMIN` / `ADMIN`.

## Quick local check

```bash
cd dentis-web
npm run build -- --configuration development
```

