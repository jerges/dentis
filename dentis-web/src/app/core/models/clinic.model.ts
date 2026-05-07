export interface Clinic {
  id: string;
  name: string;
  nif?: string;
  address?: string;
  city?: string;
  province?: string;
  zipCode?: string;
  phone?: string;
  email?: string;
  active: boolean;
  createdAt?: string;
}

export interface CreateClinicRequest {
  name: string;
  nif?: string;
  address?: string;
  city?: string;
  province?: string;
  zipCode?: string;
  phone?: string;
  email?: string;
}

export interface UpdateClinicRequest extends CreateClinicRequest {}

export type ClinicUserRole = 'SUPER_ADMIN' | 'ADMIN' | 'USER';

export type ClinicUserStaffType = 'DENTIST' | 'ADMINISTRATIVE';

export interface ClinicUser {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: ClinicUserRole;
  staffType?: ClinicUserStaffType;
  active: boolean;
}

export interface CreateClinicUserRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  role: Extract<ClinicUserRole, 'ADMIN' | 'USER'>;
  staffType: ClinicUserStaffType;
}

export interface CreateGlobalClinicUserRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  role: Extract<ClinicUserRole, 'SUPER_ADMIN' | 'ADMIN' | 'USER'>;
  staffType: ClinicUserStaffType;
  clinicId?: string;
}

export interface UpdateClinicUserRequest {
  username: string;
  email: string;
  password?: string;
  fullName: string;
  role: Extract<ClinicUserRole, 'ADMIN' | 'USER'>;
  staffType: ClinicUserStaffType;
}

export {};

