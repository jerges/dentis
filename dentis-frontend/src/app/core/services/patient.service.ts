import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService, PageResponse } from './api.service';
import { Patient, PatientSummary, CreatePatientRequest } from '../models/patient.model';

@Injectable({ providedIn: 'root' })
export class PatientService {
  constructor(private api: ApiService) {}

  create(request: CreatePatientRequest): Observable<Patient> {
    return this.api.post<Patient>('/patients', request);
  }

  update(id: string, request: CreatePatientRequest): Observable<Patient> {
    return this.api.put<Patient>(`/patients/${id}`, request);
  }

  findById(id: string): Observable<Patient> {
    return this.api.get<Patient>(`/patients/${id}`);
  }

  findAll(page = 0, size = 20): Observable<PageResponse<PatientSummary>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.api.get<PageResponse<PatientSummary>>('/patients', params);
  }

  search(query: string, page = 0, size = 20): Observable<PageResponse<PatientSummary>> {
    const params = new HttpParams().set('q', query).set('page', page).set('size', size);
    return this.api.get<PageResponse<PatientSummary>>('/patients/search', params);
  }

  deactivate(id: string): Observable<void> {
    return this.api.delete<void>(`/patients/${id}`);
  }
}
