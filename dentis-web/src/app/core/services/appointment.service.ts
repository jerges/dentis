import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '@environments/environment';
import { Appointment, CreateAppointmentRequest } from '../models/appointment.model';
import { ApiResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/appointments`;

  constructor(private http: HttpClient) {}

  schedule(request: CreateAppointmentRequest): Observable<Appointment> {
    return this.http.post<ApiResponse<Appointment>>(this.apiUrl, request).pipe(map((r) => r.data));
  }

  getById(id: string): Observable<Appointment> {
    return this.http.get<ApiResponse<Appointment>>(`${this.apiUrl}/${id}`).pipe(map((r) => r.data));
  }

  getByDentist(dentistId: string, from: string, to: string): Observable<Appointment[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http
      .get<ApiResponse<Appointment[]>>(`${this.apiUrl}/dentist/${dentistId}`, { params })
      .pipe(map((r) => r.data));
  }

  getByPatient(patientId: string): Observable<Appointment[]> {
    return this.http
      .get<ApiResponse<Appointment[]>>(`${this.apiUrl}/patient/${patientId}`)
      .pipe(map((r) => r.data));
  }

  confirm(id: string): Observable<Appointment> {
    return this.http.patch<ApiResponse<Appointment>>(`${this.apiUrl}/${id}/confirm`, {}).pipe(map((r) => r.data));
  }

  cancel(id: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/cancel`, {});
  }

  reschedule(id: string, newStart: string, newEnd: string): Observable<Appointment> {
    const params = new HttpParams().set('newStart', newStart).set('newEnd', newEnd);
    return this.http
      .patch<ApiResponse<Appointment>>(`${this.apiUrl}/${id}/reschedule`, {}, { params })
      .pipe(map((r) => r.data));
  }
}

