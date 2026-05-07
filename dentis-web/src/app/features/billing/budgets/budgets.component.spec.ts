import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { BudgetsComponent } from './budgets.component';
import { BillingService } from '../../../core/services/billing.service';
import { PatientService } from '../../../core/services/patient.service';
import { ClinicService } from '../../../core/services/clinic.service';
import { AuthService } from '../../../core/services/auth.service';
import { Budget, Tariff } from '../../../core/models/billing.model';
import { ClinicUser } from '../../../core/models/clinic.model';
import { Patient } from '../../../core/models/patient.model';

describe('BudgetsComponent', () => {
  let component: BudgetsComponent;
  let fixture: ComponentFixture<BudgetsComponent>;

  let billingServiceSpy: jasmine.SpyObj<BillingService>;
  let patientServiceSpy: jasmine.SpyObj<PatientService>;
  let clinicServiceSpy: jasmine.SpyObj<ClinicService>;
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

  const dentist: ClinicUser = {
    id: 'dentist-1',
    username: 'dr.house',
    email: 'house@dentis.dev',
    fullName: 'Gregory House',
    role: 'USER',
    staffType: 'DENTIST',
    active: true
  };

  const tariff: Tariff = {
    id: 'tariff-1',
    code: 'CONS-001',
    name: 'Consulta general',
    category: 'GENERAL_DENTISTRY',
    basePrice: 40,
    discountAllowed: true,
    active: true
  };

  const createdBudget: Budget = {
    id: 'budget-1',
    patientId: 'patient-1',
    dentistId: 'dentist-1',
    status: 'DRAFT',
    createdAt: '2026-01-01T10:00:00',
    items: [
      {
        tariffId: 'tariff-1',
        quantity: 1,
        unitPrice: 40,
        discountPercentage: 0
      }
    ]
  };

  beforeEach(async () => {
    billingServiceSpy = jasmine.createSpyObj<BillingService>('BillingService', [
      'createBudget',
      'getBudgetsByPatient',
      'getTariffs',
      'approveBudget',
      'getBudgetSummary'
    ]);
    patientServiceSpy = jasmine.createSpyObj<PatientService>('PatientService', ['search']);
    clinicServiceSpy = jasmine.createSpyObj<ClinicService>('ClinicService', ['getClinicUsers', 'getActiveClinics']);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    billingServiceSpy.createBudget.and.returnValue(of(createdBudget));
    billingServiceSpy.getTariffs.and.returnValue(
      of({ content: [tariff], page: 0, size: 1, totalElements: 1, totalPages: 1, last: true })
    );
    billingServiceSpy.getBudgetsByPatient.and.returnValue(
      of({ content: [createdBudget], page: 0, size: 50, totalElements: 1, totalPages: 1, last: true })
    );
    billingServiceSpy.approveBudget.and.returnValue(of(createdBudget));
    billingServiceSpy.getBudgetSummary.and.returnValue(
      of({ budgetId: 'budget-1', grandTotal: 40, totalPaid: 0, balance: 40, status: 'PENDING' })
    );

    patientServiceSpy.search.and.returnValue(
      of({ content: [patient], page: 0, size: 10, totalElements: 1, totalPages: 1, last: true })
    );

    clinicServiceSpy.getClinicUsers.and.returnValue(
      of({ success: true, data: [dentist], timestamp: new Date().toISOString() })
    );
    clinicServiceSpy.getActiveClinics.and.returnValue(
      of({ success: true, data: [], timestamp: new Date().toISOString() })
    );

    await TestBed.configureTestingModule({
      imports: [BudgetsComponent, NoopAnimationsModule],
      providers: [
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: PatientService, useValue: patientServiceSpy },
        { provide: ClinicService, useValue: clinicServiceSpy },
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: AuthService, useValue: { currentUser: () => ({ clinicId: 'clinic-1' }) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BudgetsComponent);
    component = fixture.componentInstance;
    spyOn((component as never)['snack'] as MatSnackBar, 'open').and.callFake(snackSpy.open);
    fixture.detectChanges();
  });

  it('should load dentists and tariffs on init', () => {
    expect(clinicServiceSpy.getClinicUsers).toHaveBeenCalledWith('clinic-1');
    expect(billingServiceSpy.getTariffs).toHaveBeenCalledWith(0, 200);
    expect(component.dentists.length).toBe(1);
    expect(component.tariffs.length).toBe(1);
  });

  it('should not submit and should show feedback when form is invalid', () => {
    component.onSubmit();

    expect(billingServiceSpy.createBudget).not.toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalledWith('Revisa los campos obligatorios del formulario', 'OK', { duration: 3000 });
  });

  it('should submit budget and refresh list by patient on success', () => {
    component.onPatientSelected(patient);
    component.onDentistSelected(dentist);
    component.onTariffSelected(tariff);
    component.form.patchValue({
      quantity: 2,
      unitPrice: 40,
      discountPercentage: 10,
      notes: 'Presupuesto inicial'
    });

    component.onSubmit();

    expect(billingServiceSpy.createBudget).toHaveBeenCalledWith(
      jasmine.objectContaining({
        patientId: 'patient-1',
        dentistId: 'dentist-1',
        items: [
          jasmine.objectContaining({
            tariffId: 'tariff-1',
            quantity: 2,
            unitPrice: 40,
            discountPercentage: 10
          })
        ]
      })
    );
    expect(billingServiceSpy.getBudgetsByPatient).toHaveBeenCalledWith('patient-1', 0, 50);
    expect(snackSpy.open).toHaveBeenCalledWith('Presupuesto creado', 'OK', { duration: 3000 });
  });

  it('should show error message when submit fails', () => {
    billingServiceSpy.createBudget.and.returnValue(throwError(() => ({ status: 400 })));

    component.onPatientSelected(patient);
    component.onDentistSelected(dentist);
    component.onTariffSelected(tariff);
    component.form.patchValue({
      quantity: 1,
      unitPrice: 40,
      discountPercentage: 0,
      notes: 'Error case'
    });

    component.onSubmit();

    expect(snackSpy.open).toHaveBeenCalledWith('No se pudo crear el presupuesto', 'OK', { duration: 3000 });
    expect(component.loading).toBeFalse();
  });
});

