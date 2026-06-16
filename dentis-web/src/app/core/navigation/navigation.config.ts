import { UserRole, UserStaffType } from '../models/auth.model';

export interface AppNavItem {
  label: string;
  icon: string;
  route: string;
  externalUrl?: string;
  roles?: UserRole[];
  staffTypes?: UserStaffType[];
}

export const APP_NAV_ITEMS: AppNavItem[] = [
  {
    label: 'Admin Clínicas',
    icon: 'domain',
    route: '/clinics',
    roles: ['SUPER_ADMIN']
  },
  {
    label: 'Mi Clínica',
    icon: 'groups',
    route: '/my-clinic/users',
    roles: ['ADMIN']
  },
  { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
  { label: 'Pacientes', icon: 'people', route: '/patients' },
  { label: 'Agenda', icon: 'calendar_month', route: '/scheduling' },
  { label: 'Presupuestos', icon: 'description', route: '/billing/budgets' },
  { label: 'Aranceles', icon: 'price_check', route: '/billing/tariffs' },
  { label: 'Pagos', icon: 'payments', route: '/billing/payments' },
  {
    label: 'Documentos',
    icon: 'folder',
    route: '/documents'
  },
  {
    label: 'Asistente IA',
    icon: 'smart_toy',
    route: '/ia/chat',
    roles: ['SUPER_ADMIN', 'ADMIN'],
    staffTypes: ['DENTIST']
  },
  {
    label: 'Métricas IA',
    icon: 'bar_chart',
    route: '/ia/stats',
    roles: ['SUPER_ADMIN', 'ADMIN']
  },
  {
    label: 'Monitoring',
    icon: 'monitor_heart',
    route: '',
    externalUrl: `${window.location.protocol}//${window.location.hostname}:3000`,
    roles: ['SUPER_ADMIN']
  }
];

