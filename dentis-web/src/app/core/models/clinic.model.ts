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

export interface ClinicUser {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'MEDICO';
  active: boolean;
}

export interface CreateClinicUserRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
  role: 'ADMIN' | 'MEDICO';
}

export {};

