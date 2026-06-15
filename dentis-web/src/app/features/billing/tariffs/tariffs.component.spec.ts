import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { TariffsComponent } from './tariffs.component';
import { BillingService } from '../../../core/services/billing.service';
import { Tariff } from '../../../core/models/billing.model';

describe('TariffsComponent', () => {
  let component: TariffsComponent;
  let fixture: ComponentFixture<TariffsComponent>;

  let billingServiceSpy: jasmine.SpyObj<BillingService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const tariff: Tariff = {
    id: 'tariff-1',
    code: 'CONS-001',
    name: 'Consulta general',
    category: 'GENERAL_DENTISTRY',
    basePrice: 40,
    discountAllowed: true,
    active: true
  };

  const pageResponse = {
    content: [tariff],
    page: 0,
    size: 200,
    totalElements: 1,
    totalPages: 1,
    last: true
  };

  beforeEach(async () => {
    billingServiceSpy = jasmine.createSpyObj<BillingService>('BillingService', [
      'getTariffs',
      'createTariff',
      'deactivateTariff'
    ]);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    billingServiceSpy.getTariffs.and.returnValue(of(pageResponse));
    billingServiceSpy.createTariff.and.returnValue(of(tariff));
    billingServiceSpy.deactivateTariff.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [TariffsComponent, NoopAnimationsModule],
      providers: [
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: MatSnackBar, useValue: snackSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TariffsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load tariffs on init', () => {
    expect(billingServiceSpy.getTariffs).toHaveBeenCalledWith(0, 200);
    expect(component.dataSource.data.length).toBe(1);
    expect(component.dataSource.data[0].id).toBe('tariff-1');
  });

  it('should not submit and mark form touched when form is invalid', () => {
    component.form.reset();
    component.onSubmit();

    expect(billingServiceSpy.createTariff).not.toHaveBeenCalled();
    expect(component.form.touched).toBeTrue();
  });

  it('should create tariff and prepend to dataSource on success', () => {
    const newTariff: Tariff = { ...tariff, id: 'tariff-2', code: 'ENDO-001', name: 'Endodoncia' };
    billingServiceSpy.createTariff.and.returnValue(of(newTariff));

    component.form.patchValue({
      code: 'ENDO-001',
      name: 'Endodoncia',
      category: 'ENDODONTICS',
      basePrice: 120
    });
    component.onSubmit();

    expect(billingServiceSpy.createTariff).toHaveBeenCalledWith(
      jasmine.objectContaining({ code: 'ENDO-001', name: 'Endodoncia', basePrice: 120 })
    );
    expect(component.dataSource.data[0].id).toBe('tariff-2');
    expect(snackSpy.open).toHaveBeenCalledWith('Arancel creado', 'OK', { duration: 3000 });
    expect(component.showForm).toBeFalse();
    expect(component.loading).toBeFalse();
  });

  it('should show error snack when createTariff fails', () => {
    billingServiceSpy.createTariff.and.returnValue(throwError(() => ({ status: 500 })));

    component.form.patchValue({
      code: 'CONS-002',
      name: 'Limpieza',
      category: 'GENERAL_DENTISTRY',
      basePrice: 30
    });
    component.onSubmit();

    expect(snackSpy.open).toHaveBeenCalledWith('No se pudo crear el arancel', 'OK', { duration: 3000 });
    expect(component.loading).toBeFalse();
  });

  it('should deactivate a tariff and update dataSource', () => {
    component.deactivate('tariff-1');

    expect(billingServiceSpy.deactivateTariff).toHaveBeenCalledWith('tariff-1');
    expect(component.dataSource.data[0].active).toBeFalse();
    expect(snackSpy.open).toHaveBeenCalledWith('Arancel desactivado', 'OK', { duration: 3000 });
  });

  it('should show error snack when deactivate fails', () => {
    billingServiceSpy.deactivateTariff.and.returnValue(throwError(() => ({ status: 500 })));

    component.deactivate('tariff-1');

    expect(snackSpy.open).toHaveBeenCalledWith('No se pudo desactivar el arancel', 'OK', { duration: 3000 });
  });
});
