import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService, PageResponse } from './api.service';
import { Appointment, AppointmentStatus, CreateAppointmentRequest } from '../models/appointment.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  constructor(private api: ApiService) {}

  create(request: CreateAppointmentRequest): Observable<Appointment> {
    return this.api.post<Appointment>('/appointments', request);
  }

  findById(id: string): Observable<Appointment> {
    return this.api.get<Appointment>(`/appointments/${id}`);
  }

  findByPatient(patientId: string, page = 0, size = 20): Observable<PageResponse<Appointment>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.api.get<PageResponse<Appointment>>(`/appointments/patient/${patientId}`, params);
  }

  getDentistCalendar(dentistId: string, from: string, to: string): Observable<Appointment[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.api.get<Appointment[]>(`/appointments/dentist/${dentistId}/calendar`, params);
  }

  updateStatus(id: string, status: AppointmentStatus): Observable<Appointment> {
    const params = new HttpParams().set('status', status);
    return this.api.patch<Appointment>(`/appointments/${id}/status?status=${status}`);
  }

  cancel(id: string): Observable<void> {
    return this.api.delete<void>(`/appointments/${id}`);
  }
}
