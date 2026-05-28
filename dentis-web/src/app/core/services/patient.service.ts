import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '@environments/environment';
import { CreatePatientRequest, Patient, UpdatePatientRequest } from '../models/patient.model';
import { ApiResponse, PageResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/patients`;

  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 20): Observable<PageResponse<Patient>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'lastName');
    return this.http.get<ApiResponse<PageResponse<Patient>>>(this.apiUrl, { params }).pipe(map((r) => r.data));
  }

  search(name: string, page = 0, size = 20): Observable<PageResponse<Patient>> {
    const params = new HttpParams().set('name', name).set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<Patient>>>(`${this.apiUrl}/search`, { params }).pipe(map((r) => r.data));
  }

  getById(id: string): Observable<Patient> {
    return this.http.get<ApiResponse<Patient>>(`${this.apiUrl}/${id}`).pipe(map((r) => r.data));
  }

  create(request: CreatePatientRequest): Observable<Patient> {
    return this.http.post<ApiResponse<Patient>>(this.apiUrl, request).pipe(map((r) => r.data));
  }

  update(id: string, request: UpdatePatientRequest): Observable<Patient> {
    return this.http.put<ApiResponse<Patient>>(`${this.apiUrl}/${id}`, request).pipe(map((r) => r.data));
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

