import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BillingService } from './billing.service';
import { Budget, Payment, Tariff } from '../models/billing.model';

const BASE = '/api/v1/billing';

const mockBudget: Budget = {
  id: 'budget-1',
  patientId: 'patient-1',
  clinicId: 'clinic-1',
  status: 'DRAFT',
  items: [],
  totalAmount: 0
} as unknown as Budget;

const mockPayment: Payment = {
  id: 'payment-1',
  budgetId: 'budget-1',
  patientId: 'patient-1',
  amount: 100,
  method: 'CASH'
} as unknown as Payment;

const mockTariff: Tariff = {
  id: 'tariff-1',
  name: 'Consultation',
  price: 50,
  active: true
} as unknown as Tariff;

describe('BillingService', () => {
  let service: BillingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BillingService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(BillingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create a budget', () => {
    let result: Budget | undefined;

    service.createBudget({ patientId: 'patient-1', items: [] } as any).subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/budgets`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: mockBudget, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockBudget);
  });

  it('should get a budget by id', () => {
    let result: Budget | undefined;

    service.getBudget('budget-1').subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/budgets/budget-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: mockBudget, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockBudget);
  });

  it('should get budgets by patient with pagination', () => {
    service.getBudgetsByPatient('patient-1', 0, 20).subscribe();

    const req = httpMock.expectOne(`${BASE}/budgets/patient/patient-1?page=0&size=20`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { content: [], totalElements: 0, page: 0, size: 20, totalPages: 0, last: true }, timestamp: new Date().toISOString() });
  });

  it('should approve a budget', () => {
    const approved = { ...mockBudget, status: 'APPROVED' } as Budget;
    let result: Budget | undefined;

    service.approveBudget('budget-1').subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/budgets/budget-1/approve`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ success: true, data: approved, timestamp: new Date().toISOString() });
    expect(result?.status).toBe('APPROVED');
  });

  it('should mark a budget item as performed', () => {
    service.markItemPerformed('budget-1', 'item-1').subscribe();

    const req = httpMock.expectOne(`${BASE}/budgets/budget-1/items/item-1/performed`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ success: true, data: mockBudget, timestamp: new Date().toISOString() });
  });

  it('should register a payment', () => {
    let result: Payment | undefined;

    service.registerPayment({ budgetId: 'budget-1', amount: 100, method: 'CASH' }).subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/payments`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: mockPayment, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockPayment);
  });

  it('should get payments by budget', () => {
    service.getPaymentsByBudget('budget-1').subscribe();

    const req = httpMock.expectOne(`${BASE}/payments/budget/budget-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockPayment], timestamp: new Date().toISOString() });
  });

  it('should get tariffs with default pagination', () => {
    service.getTariffs().subscribe();

    const req = httpMock.expectOne(`${BASE}/tariffs?page=0&size=100`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { content: [mockTariff], totalElements: 1, page: 0, size: 100, totalPages: 1, last: true }, timestamp: new Date().toISOString() });
  });

  it('should create a tariff', () => {
    let result: Tariff | undefined;

    service.createTariff({ name: 'Consultation', price: 50 } as any).subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/tariffs`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: mockTariff, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockTariff);
  });

  it('should deactivate a tariff', () => {
    service.deactivateTariff('tariff-1').subscribe();

    const req = httpMock.expectOne(`${BASE}/tariffs/tariff-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
