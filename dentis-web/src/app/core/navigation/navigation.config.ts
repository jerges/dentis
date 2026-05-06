import { UserRole } from '../models/auth.model';

export interface AppNavItem {
  label: string;
  icon: string;
  route: string;
  roles?: UserRole[];
}

export const APP_NAV_ITEMS: AppNavItem[] = [
  {
    label: 'Admin Clínicas',
    icon: 'domain',
    route: '/clinics',
    roles: ['SUPER_ADMIN', 'ADMIN']
  },
  { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
  { label: 'Pacientes', icon: 'people', route: '/patients' },
  { label: 'Agenda', icon: 'calendar_month', route: '/scheduling' },
  { label: 'Presupuestos', icon: 'description', route: '/billing/budgets' },
  { label: 'Aranceles', icon: 'price_check', route: '/billing/tariffs' },
  { label: 'Pagos', icon: 'payments', route: '/billing/payments' }
];

