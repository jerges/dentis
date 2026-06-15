import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
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

  const createdPatient: Patient = {
    id: 'new-patient-1',
    firstName: 'Carlos',
    lastName: 'Perez',
    idDocument: 'V-98765432',
    active: true,
    birthDate: '1985-05-20',
    sex: 'MALE',
    gender: 'MALE',
    contactInfo: {
      email: 'carlos@dentis.dev',
      phoneNumber: '412000000'
    }
  };

  beforeEach(async () => {
    patientServiceSpy = jasmine.createSpyObj<PatientService>('PatientService', ['getById', 'create', 'update']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    patientServiceSpy.create.and.returnValue(of(createdPatient));
    patientServiceSpy.update.and.returnValue(of(createdPatient));
    patientServiceSpy.getById.and.returnValue(of(createdPatient));

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

  it('should not call create when forms are invalid', () => {
    component.onSubmit();

    expect(patientServiceSpy.create).not.toHaveBeenCalled();
    expect(component.personalForm.touched).toBeTrue();
  });

  it('should call patientService.create and navigate on valid new patient form', () => {
    component.personalForm.patchValue({
      firstName: 'Carlos',
      lastName: 'Perez',
      docType: 'V',
      idDocument: '98765432',
      birthDate: '1985-05-20',
      sex: 'MALE',
      gender: 'MALE'
    });
    component.contactForm.patchValue({
      email: 'carlos@dentis.dev',
      phoneNumber: '412000000'
    });

    component.onSubmit();

    expect(patientServiceSpy.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        firstName: 'Carlos',
        lastName: 'Perez',
        idDocument: 'V-98765432'
      })
    );
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/patients', 'new-patient-1']);
    expect(snackSpy.open).toHaveBeenCalledWith('Paciente registrado', 'OK', { duration: 3000 });
  });

  it('should show error snack when create fails', () => {
    patientServiceSpy.create.and.returnValue(throwError(() => ({ status: 400 })));

    component.personalForm.patchValue({
      firstName: 'Carlos',
      lastName: 'Perez',
      docType: 'V',
      idDocument: '98765432',
      birthDate: '1985-05-20',
      sex: 'MALE',
      gender: 'MALE'
    });
    component.contactForm.patchValue({
      email: 'carlos@dentis.dev',
      phoneNumber: '412000000'
    });

    component.onSubmit();

    expect(component.loading).toBeFalse();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should not load patient data when id input is empty (create mode)', () => {
    expect(patientServiceSpy.getById).not.toHaveBeenCalled();
    expect(component.isEdit).toBeFalse();
  });

  it('should mark forms as touched on invalid submit', () => {
    component.onSubmit();

    expect(component.contactForm.touched).toBeTrue();
    expect(component.personalForm.touched).toBeTrue();
  });

  it('should not set loading to true when form is invalid', () => {
    component.onSubmit();
    expect(component.loading).toBeFalse();
  });
});
