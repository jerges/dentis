import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService, PageResponse } from './api.service';
import {
  Budget, CreateBudgetRequest, CreatePaymentRequest, Payment
} from '../models/budget.model';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  constructor(private api: ApiService) {}

  create(request: CreateBudgetRequest): Observable<Budget> {
    return this.api.post<Budget>('/budgets', request);
  }

  findById(id: string): Observable<Budget> {
    return this.api.get<Budget>(`/budgets/${id}`);
  }

  findByPatient(patientId: string, page = 0, size = 20): Observable<PageResponse<Budget>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.api.get<PageResponse<Budget>>(`/budgets/patient/${patientId}`, params);
  }

  approve(id: string): Observable<Budget> {
    return this.api.post<Budget>(`/budgets/${id}/approve`, {});
  }

  reject(id: string): Observable<Budget> {
    return this.api.post<Budget>(`/budgets/${id}/reject`, {});
  }

  markItemPerformed(budgetId: string, itemId: string): Observable<void> {
    return this.api.patch<void>(`/budgets/${budgetId}/items/${itemId}/performed`);
  }

  registerPayment(request: CreatePaymentRequest): Observable<Payment> {
    return this.api.post<Payment>('/payments', request);
  }

  getPayments(budgetId: string): Observable<Payment[]> {
    return this.api.get<Payment[]>(`/payments/budget/${budgetId}`);
  }
}
