import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '@environments/environment';
import { Budget, BudgetSummary, CreateBudgetRequest, CreateTariffRequest, Payment, Tariff } from '../models/billing.model';
import { ApiResponse, PageResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly baseUrl = `${environment.apiUrl}/api/v1/billing`;

  constructor(private readonly http: HttpClient) {}

  // Budgets
  createBudget(budget: CreateBudgetRequest): Observable<Budget> {
    return this.http.post<ApiResponse<Budget>>(`${this.baseUrl}/budgets`, budget).pipe(map((r) => r.data));
  }

  getBudget(id: string): Observable<Budget> {
    return this.http.get<ApiResponse<Budget>>(`${this.baseUrl}/budgets/${id}`).pipe(map((r) => r.data));
  }

  getBudgetsByPatient(patientId: string, page = 0, size = 20): Observable<PageResponse<Budget>> {
    return this.http
      .get<ApiResponse<PageResponse<Budget>>>(`${this.baseUrl}/budgets/patient/${patientId}?page=${page}&size=${size}`)
      .pipe(map((r) => r.data));
  }

  approveBudget(id: string): Observable<Budget> {
    return this.http.patch<ApiResponse<Budget>>(`${this.baseUrl}/budgets/${id}/approve`, {}).pipe(map((r) => r.data));
  }

  markItemPerformed(budgetId: string, itemId: string): Observable<Budget> {
    return this.http.patch<ApiResponse<Budget>>(
      `${this.baseUrl}/budgets/${budgetId}/items/${itemId}/performed`, {}
    ).pipe(map((r) => r.data));
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

  // Tariffs
  getTariffs(page = 0, size = 100): Observable<PageResponse<Tariff>> {
    return this.http
      .get<ApiResponse<PageResponse<Tariff>>>(`${this.baseUrl}/tariffs?page=${page}&size=${size}`)
      .pipe(map((r) => r.data));
  }

  createTariff(request: CreateTariffRequest): Observable<Tariff> {
    return this.http.post<ApiResponse<Tariff>>(`${this.baseUrl}/tariffs`, request).pipe(map((r) => r.data));
  }

  deactivateTariff(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tariffs/${id}`);
  }
}

