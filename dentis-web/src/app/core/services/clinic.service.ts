import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';
import { ApiResponse, PageResponse } from '../models/api.model';
import {
  Clinic,
  ClinicUser,
  CreateClinicRequest,
  CreateClinicUserRequest,
  UpdateClinicRequest
} from '../models/clinic.model';

@Injectable({ providedIn: 'root' })
export class ClinicService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/clinics`;

  constructor(private readonly http: HttpClient) {}

  getClinics(page = 0, size = 20): Observable<ApiResponse<PageResponse<Clinic>>> {
    return this.http.get<ApiResponse<PageResponse<Clinic>>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  getActiveClinics(): Observable<ApiResponse<Clinic[]>> {
    return this.http.get<ApiResponse<Clinic[]>>(`${this.apiUrl}/active`);
  }

  getClinic(id: string): Observable<ApiResponse<Clinic>> {
    return this.http.get<ApiResponse<Clinic>>(`${this.apiUrl}/${id}`);
  }

  createClinic(request: CreateClinicRequest): Observable<ApiResponse<Clinic>> {
    return this.http.post<ApiResponse<Clinic>>(this.apiUrl, request);
  }

  updateClinic(id: string, request: UpdateClinicRequest): Observable<ApiResponse<Clinic>> {
    return this.http.put<ApiResponse<Clinic>>(`${this.apiUrl}/${id}`, request);
  }

  deactivateClinic(id: string): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.apiUrl}/${id}`);
  }

  getClinicUsers(clinicId: string): Observable<ApiResponse<ClinicUser[]>> {
    return this.http.get<ApiResponse<ClinicUser[]>>(`${this.apiUrl}/${clinicId}/users`);
  }

  createClinicUser(clinicId: string, request: CreateClinicUserRequest): Observable<ApiResponse<ClinicUser>> {
    return this.http.post<ApiResponse<ClinicUser>>(`${this.apiUrl}/${clinicId}/users`, request);
  }

  deactivateClinicUser(clinicId: string, userId: string): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.apiUrl}/${clinicId}/users/${userId}`);
  }
}

