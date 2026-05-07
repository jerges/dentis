export interface LoginRequest {
  username: string;
  password: string;
}

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'USER' | string;

export interface AuthResponse {
  token: string;
  expiresIn: number;
  username: string;
  role: UserRole;
  clinicId?: string;
}

export interface AuthUser {
  username: string;
  role: UserRole;
  token: string;
  clinicId?: string;
}
