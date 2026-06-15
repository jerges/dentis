import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./app-shell.component').then((m) => m.AppShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      // Patients
      {
        path: 'patients',
        loadComponent: () =>
          import('./features/patients/list/patients-list.component').then((m) => m.PatientsListComponent)
      },
      {
        path: 'patients/new',
        loadComponent: () =>
          import('./features/patients/form/patient-form.component').then((m) => m.PatientFormComponent)
      },
      {
        path: 'patients/:id',
        loadComponent: () =>
          import('./features/patients/detail/patient-detail.component').then((m) => m.PatientDetailComponent)
      },
      {
        path: 'patients/:id/edit',
        loadComponent: () =>
          import('./features/patients/form/patient-form.component').then((m) => m.PatientFormComponent)
      },
      // Scheduling
      {
        path: 'scheduling',
        loadComponent: () =>
          import('./features/scheduling/calendar/appointment-calendar.component').then(
            (m) => m.AppointmentCalendarComponent
          )
      },
      {
        path: 'scheduling/new',
        loadComponent: () =>
          import('./features/scheduling/appointment-form/appointment-form.component').then(
            (m) => m.AppointmentFormComponent
          )
      },
      // Clinical
      {
        path: 'clinical/:patientId',
        loadComponent: () =>
          import('./features/clinical/record/clinical-record.component').then((m) => m.ClinicalRecordComponent)
      },
      {
        path: 'clinical/:patientId/odontogram',
        loadComponent: () =>
          import('./features/clinical/odontogram/odontogram.component').then((m) => m.OdontogramComponent)
      },
      // Billing
      {
        path: 'billing/budgets',
        loadComponent: () =>
          import('./features/billing/budgets/budgets.component').then((m) => m.BudgetsComponent)
      },
      {
        path: 'billing/tariffs',
        loadComponent: () =>
          import('./features/billing/tariffs/tariffs.component').then((m) => m.TariffsComponent)
      },
      {
        path: 'billing/payments',
        loadComponent: () =>
          import('./features/billing/payments/payments.component').then((m) => m.PaymentsComponent)
      },
      {
        path: 'clinics',
        canActivate: [authGuard],
        data: { roles: ['SUPER_ADMIN'] },
        loadComponent: () =>
          import('./features/clinics/list/clinics-list.component').then((m) => m.ClinicsListComponent)
      },
      {
        path: 'clinics/new',
        canActivate: [authGuard],
        data: { roles: ['SUPER_ADMIN'] },
        loadComponent: () =>
          import('./features/clinics/form/clinic-form.component').then((m) => m.ClinicFormComponent)
      },
      {
        path: 'clinics/new-super-admin',
        canActivate: [authGuard],
        data: { roles: ['SUPER_ADMIN'] },
        loadComponent: () =>
          import('./features/clinics/users/super-admin-form.component').then((m) => m.SuperAdminFormComponent)
      },
      {
        path: 'clinics/:id/edit',
        canActivate: [authGuard],
        data: { roles: ['SUPER_ADMIN'] },
        loadComponent: () =>
          import('./features/clinics/form/clinic-form.component').then((m) => m.ClinicFormComponent)
      },
      {
        path: 'clinics/:clinicId/users',
        canActivate: [authGuard],
        data: { roles: ['SUPER_ADMIN'] },
        loadComponent: () =>
          import('./features/clinics/users/clinic-users.component').then((m) => m.ClinicUsersComponent)
      },
      {
        path: 'my-clinic/users',
        canActivate: [authGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/clinics/users/clinic-users.component').then((m) => m.ClinicUsersComponent)
      },
      {
        path: 'ia/chat',
        canActivate: [authGuard],
        data: { roles: ['SUPER_ADMIN', 'ADMIN'], staffTypes: ['DENTIST'] },
        loadComponent: () =>
          import('./features/ia/chat/ia-chat.component').then((m) => m.IaChatComponent)
      },
      {
        path: 'ia/stats',
        canActivate: [authGuard],
        data: { roles: ['SUPER_ADMIN', 'ADMIN'] },
        loadComponent: () =>
          import('./features/ia/stats/ia-stats.component').then((m) => m.IaStatsComponent)
      }
    ]
  },
  { path: '**', redirectTo: '/dashboard' }
];
