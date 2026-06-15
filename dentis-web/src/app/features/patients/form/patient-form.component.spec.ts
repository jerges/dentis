import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { PatientFormComponent } from './patient-form.component';
import { PatientService } from '../../../core/services/patient.service';
import { Patient } from '../../../core/models/patient.model';

describe('PatientFormComponent', () => {
  let component: PatientFormComponent;
  let fixture: ComponentFixture<PatientFormComponent>;
  let patientServiceSpy: jasmine.SpyObj<PatientService>;
  let routerSpy: jasmine.SpyObj<Router>;
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
    socialName: undefined,
    contactInfo: {
      email: 'ana@dentis.dev',
      phoneNumber: '600000000',
      alternativePhone: undefined
    },
    address: {
      street: 'Calle 1',
      city: 'Caracas',
      state: 'Miranda',
      zipCode: '1010'
    },
    representative: undefined
  } as unknown as Patient;

  beforeEach(async () => {
    patientServiceSpy = jasmine.createSpyObj<PatientService>('PatientService', ['getById', 'create', 'update']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    patientServiceSpy.create.and.returnValue(of(patient));
    patientServiceSpy.update.and.returnValue(of(patient));
    patientServiceSpy.getById.and.returnValue(of(patient));

    await TestBed.configureTestingModule({
      imports: [PatientFormComponent, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: PatientService, useValue: patientServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PatientFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should start with isEdit false and forms invalid when no id', () => {
    expect(component.isEdit).toBeFalse();
    expect(component.personalForm.invalid).toBeTrue();
  });

  it('should not call create and should mark forms as touched when forms are invalid', () => {
    component.onSubmit();
    expect(patientServiceSpy.create).not.toHaveBeenCalled();
    expect(component.personalForm.touched).toBeTrue();
  });

  it('should create a patient when all required fields are filled and submit succeeds', () => {
    component.personalForm.patchValue({
      firstName: 'Ana',
      lastName: 'Lopez',
      docType: 'V',
      idDocument: '12345678',
      birthDate: '1990-01-10',
      sex: 'FEMALE'
    });
    component.contactForm.patchValue({
      email: 'ana@dentis.dev',
      phoneNumber: '600000000'
    });

    component.onSubmit();

    expect(patientServiceSpy.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        firstName: 'Ana',
        lastName: 'Lopez',
        idDocument: 'V-12345678'
      })
    );
    expect(snackSpy.open).toHaveBeenCalledWith('Paciente registrado', 'OK', { duration: 3000 });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/patients', 'patient-1']);
  });

  it('should set loading to false when create fails', () => {
    patientServiceSpy.create.and.returnValue(throwError(() => ({ status: 500 })));

    component.personalForm.patchValue({
      firstName: 'Ana',
      lastName: 'Lopez',
      docType: 'V',
      idDocument: '12345678',
      birthDate: '1990-01-10',
      sex: 'FEMALE'
    });
    component.contactForm.patchValue({
      email: 'ana@dentis.dev',
      phoneNumber: '600000000'
    });

    component.onSubmit();

    expect(component.loading).toBeFalse();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should load patient data and set isEdit when id input is set', async () => {
    // Re-create with an id input
    fixture.destroy();
    const fixture2 = TestBed.createComponent(PatientFormComponent);
    const comp2 = fixture2.componentInstance;
    fixture2.componentRef.setInput('id', 'patient-1');
    fixture2.detectChanges();

    expect(patientServiceSpy.getById).toHaveBeenCalledWith('patient-1');
    expect(comp2.isEdit).toBeTrue();
    expect(comp2.personalForm.value.firstName).toBe('Ana');
    fixture2.destroy();
  });

  it('should call update instead of create when isEdit is true', () => {
    component.isEdit = true;
    fixture.componentRef.setInput('id', 'patient-1');

    component.personalForm.patchValue({
      firstName: 'Ana',
      lastName: 'Lopez',
      docType: 'V',
      idDocument: '12345678',
      birthDate: '1990-01-10',
      sex: 'FEMALE'
    });
    component.contactForm.patchValue({
      email: 'ana@dentis.dev',
      phoneNumber: '600000000'
    });

    component.onSubmit();

    expect(patientServiceSpy.update).toHaveBeenCalled();
    expect(patientServiceSpy.create).not.toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalledWith('Paciente actualizado', 'OK', { duration: 3000 });
  });
});
