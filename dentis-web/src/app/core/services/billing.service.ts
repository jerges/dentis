import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '@environments/environment';
import { Budget, BudgetSummary, Payment } from '../models/billing.model';
import { ApiResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly baseUrl = `${environment.apiUrl}/api/v1/billing`;

  constructor(private http: HttpClient) {}

  // Budgets
  createBudget(budget: Partial<Budget>): Observable<Budget> {
    return this.http.post<ApiResponse<Budget>>(`${this.baseUrl}/budgets`, budget).pipe(map((r) => r.data));
  }

  getBudget(id: string): Observable<Budget> {
    return this.http.get<ApiResponse<Budget>>(`${this.baseUrl}/budgets/${id}`).pipe(map((r) => r.data));
  }

  approveBudget(id: string): Observable<Budget> {
    return this.http.patch<ApiResponse<Budget>>(`${this.baseUrl}/budgets/${id}/approve`, {}).pipe(map((r) => r.data));
  }

  getBudgetSummary(id: string): Observable<BudgetSummary> {
    return this.http.get<ApiResponse<BudgetSummary>>(`${this.baseUrl}/budgets/${id}/summary`).pipe(map((r) => r.data));
  }

  // Payments
  registerPayment(payment: Partial<Payment>): Observable<Payment> {
    return this.http.post<ApiResponse<Payment>>(`${this.baseUrl}/payments`, payment).pipe(map((r) => r.data));
  }

  getPaymentsByBudget(budgetId: string): Observable<Payment[]> {
    return this.http.get<ApiResponse<Payment[]>>(`${this.baseUrl}/payments/budget/${budgetId}`).pipe(map((r) => r.data));
  }

  getPaymentsByPatient(patientId: string): Observable<Payment[]> {
    return this.http.get<ApiResponse<Payment[]>>(`${this.baseUrl}/payments/patient/${patientId}`).pipe(map((r) => r.data));
  }
}

