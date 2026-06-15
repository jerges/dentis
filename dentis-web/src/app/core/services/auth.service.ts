import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '@environments/environment';
import { AuthResponse, AuthUser, LoginRequest, UserRole, UserStaffType } from '../models/auth.model';
import { ApiResponse } from '../models/api.model';

const TOKEN_KEY = 'dentis_token';
const USER_KEY = 'dentis_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/auth`;

  currentUser = signal<AuthUser | null>(this.loadUser());

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, request).pipe(
      tap((res) => {
        if (res.success && res.data) {
          this.saveSession(res.data);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    const user = this.currentUser();
    if (!user || !this.getToken()) return false;
    if (user.expiresAt && Date.now() > user.expiresAt) {
      this.logout();
      return false;
    }
    return true;
  }

  hasAnyRole(roles: UserRole[]): boolean {
    const user = this.currentUser();
    return !!user && roles.includes(user.role);
  }

  hasAnyStaffType(staffTypes: UserStaffType[]): boolean {
    const staffType = this.currentUser()?.staffType;
    return !!staffType && staffTypes.includes(staffType);
  }

  getRole(): UserRole | null {
    return this.currentUser()?.role ?? null;
  }

  private saveSession(data: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, data.token);
    const user: AuthUser = {
      username: data.username,
      role: data.role,
      staffType: data.staffType,
      token: data.token,
      clinicId: data.clinicId,
      expiresAt: data.expiresIn ? Date.now() + data.expiresIn : undefined,
    };
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private loadUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
