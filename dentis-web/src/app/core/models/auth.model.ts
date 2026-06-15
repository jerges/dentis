export interface LoginRequest {
  username: string;
  password: string;
}

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'USER' | string;
export type UserStaffType = 'DENTIST' | 'ADMINISTRATIVE' | string;

export interface AuthResponse {
  token: string;
  expiresIn: number;
  username: string;
  role: UserRole;
  staffType?: UserStaffType;
  clinicId?: string;
}

export interface AuthUser {
  username: string;
  role: UserRole;
  staffType?: UserStaffType;
  token: string;
  clinicId?: string;
  expiresAt?: number;
}
