import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { PaymentsComponent } from './payments.component';
import { BillingService } from '../../../core/services/billing.service';
import { PatientService } from '../../../core/services/patient.service';
import { Budget, Payment } from '../../../core/models/billing.model';
import { Patient } from '../../../core/models/patient.model';

describe('PaymentsComponent', () => {
  let component: PaymentsComponent;
  let fixture: ComponentFixture<PaymentsComponent>;

  let billingServiceSpy: jasmine.SpyObj<BillingService>;
  let patientServiceSpy: jasmine.SpyObj<PatientService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const patient: Patient = {
    id: 'patient-1',
    firstName: 'Ana',
    lastName: 'Lopez',
    idDocument: 'V-12345678',
    active: true,
    birthDate: '1990-01-10',
    sex: 'FEMALE',
    gender: 'FEMALE',
    contactInfo: {
      email: 'ana@dentis.dev',
      phoneNumber: '600000000'
    }
  } as unknown as Patient;

  const approvedBudget: Budget = {
    id: 'budget-approved',
    patientId: 'patient-1',
    dentistId: 'dentist-1',
    status: 'APPROVED',
    createdAt: '2026-01-01T10:00:00',
    items: [{ tariffId: 'tariff-1', quantity: 1, unitPrice: 50 }]
  };

  const draftBudget: Budget = {
    id: 'budget-draft',
    patientId: 'patient-1',
    dentistId: 'dentist-1',
    status: 'DRAFT',
    createdAt: '2026-01-02T10:00:00',
    items: [{ tariffId: 'tariff-1', quantity: 1, unitPrice: 50 }]
  };

  const payment: Payment = {
    id: 'payment-1',
    patientId: 'patient-1',
    budgetId: 'budget-approved',
    amount: 25,
    paymentMethod: 'CASH',
    paidAt: '2026-01-03T10:00:00',
    invoiceReference: 'INV-001'
  };

  beforeEach(async () => {
    billingServiceSpy = jasmine.createSpyObj<BillingService>('BillingService', [
      'registerPayment',
      'getBudgetsByPatient',
      'getPaymentsByPatient'
    ]);
    patientServiceSpy = jasmine.createSpyObj<PatientService>('PatientService', ['search']);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    billingServiceSpy.registerPayment.and.returnValue(of(payment));
    billingServiceSpy.getBudgetsByPatient.and.returnValue(
      of({ content: [approvedBudget, draftBudget], page: 0, size: 20, totalElements: 2, totalPages: 1, last: true })
    );
    billingServiceSpy.getPaymentsByPatient.and.returnValue(of([payment]));
    patientServiceSpy.search.and.returnValue(
      of({ content: [patient], page: 0, size: 10, totalElements: 1, totalPages: 1, last: true })
    );

    await TestBed.configureTestingModule({
      imports: [PaymentsComponent, NoopAnimationsModule],
      providers: [
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: PatientService, useValue: patientServiceSpy },
        { provide: MatSnackBar, useValue: snackSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentsComponent);
    component = fixture.componentInstance;
    spyOn((component as never)['snack'] as MatSnackBar, 'open').and.callFake(snackSpy.open);
    fixture.detectChanges();
  });

  it('should not submit and should show feedback when form is invalid', () => {
    component.onSubmit();

    expect(billingServiceSpy.registerPayment).not.toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalledWith('Revisa los campos obligatorios del formulario', 'OK', { duration: 3000 });
  });

  it('should block submit when selected budget is not approved', () => {
    component.budgets = [draftBudget];
    component.form.controls.amount.setValue(10 as never);
    component.form.patchValue({
      patientId: 'patient-1',
      budgetId: 'budget-draft',
      paymentMethod: 'CASH'
    });

    component.onSubmit();

    expect(billingServiceSpy.registerPayment).not.toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalledWith('Selecciona un presupuesto aprobado para registrar el pago', 'OK', { duration: 4000 });
  });

  it('should submit payment and reload payments for selected patient', () => {
    component.budgets = [approvedBudget];
    component.showForm = true;
    component.form.controls.amount.setValue(25 as never);
    component.form.patchValue({
      patientId: 'patient-1',
      budgetId: 'budget-approved',
      paymentMethod: 'CASH',
      invoiceReference: 'INV-001'
    });

    component.onSubmit();

    expect(billingServiceSpy.registerPayment).toHaveBeenCalled();
    expect(billingServiceSpy.getPaymentsByPatient).toHaveBeenCalledWith('patient-1');
    expect(component.showForm).toBeFalse();
    expect(snackSpy.open).toHaveBeenCalledWith('Pago registrado', 'OK', { duration: 3000 });
  });

  it('should load approved budgets and payments when selecting a patient', () => {
    component.onPatientSelected(patient);

    expect(billingServiceSpy.getBudgetsByPatient).toHaveBeenCalledWith('patient-1', 0, 20);
    expect(billingServiceSpy.getPaymentsByPatient).toHaveBeenCalledWith('patient-1');
    expect(component.budgets.length).toBe(1);
    expect(component.budgets[0].status).toBe('APPROVED');
  });

  it('should toggle payment form when clicking header button', () => {
    const host = fixture.nativeElement as HTMLElement;
    const toggleButton = host.querySelector('button[type="button"]') as HTMLButtonElement;

    expect(toggleButton).withContext('Toggle button should exist').not.toBeNull();
    expect(component.showForm).toBeFalse();
    expect(host.textContent).toContain('Registrar Pago');

    toggleButton.click();
    fixture.detectChanges();

    expect(component.showForm).toBeTrue();
    expect(host.textContent).toContain('Nuevo Pago');
    expect(host.textContent).toContain('Cancelar');

    toggleButton.click();
    fixture.detectChanges();

    expect(component.showForm).toBeFalse();
    expect(host.textContent).not.toContain('Nuevo Pago');
    expect(host.textContent).toContain('Registrar Pago');
  });

  it('should show feedback when loading patient payments fails', () => {
    billingServiceSpy.getPaymentsByPatient.and.returnValue(throwError(() => ({ status: 500 })));

    component.onPatientSelected(patient);

    expect(component.dataSource.data).toEqual([]);
    expect(snackSpy.open).toHaveBeenCalledWith('No se pudieron cargar los pagos del paciente', 'OK', { duration: 3500 });
  });
});

