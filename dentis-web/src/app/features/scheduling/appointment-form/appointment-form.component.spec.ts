import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AppointmentFormComponent } from './appointment-form.component';
import { AppointmentService } from '../../../core/services/appointment.service';
import { PatientService } from '../../../core/services/patient.service';
import { ClinicService } from '../../../core/services/clinic.service';
import { AuthService } from '../../../core/services/auth.service';
import { Appointment } from '../../../core/models/appointment.model';
import { ClinicUser } from '../../../core/models/clinic.model';
import { Patient } from '../../../core/models/patient.model';

describe('AppointmentFormComponent', () => {
  let component: AppointmentFormComponent;
  let fixture: ComponentFixture<AppointmentFormComponent>;

  let appointmentServiceSpy: jasmine.SpyObj<AppointmentService>;
  let patientServiceSpy: jasmine.SpyObj<PatientService>;
  let clinicServiceSpy: jasmine.SpyObj<ClinicService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const dentist: ClinicUser = {
    id: 'dentist-1',
    username: 'dr.house',
    email: 'house@dentis.dev',
    fullName: 'Gregory House',
    role: 'USER',
    staffType: 'DENTIST',
    active: true
  };

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

  const appointmentResponse: Appointment = {
    id: 'apt-1',
    patientId: 'patient-1',
    dentistId: 'dentist-1',
    startDateTime: '2026-12-01T10:00:00',
    endDateTime: '2026-12-01T11:00:00',
    status: 'SCHEDULED',
    consultationReason: 'Revisión'
  };

  beforeEach(async () => {
    appointmentServiceSpy = jasmine.createSpyObj<AppointmentService>('AppointmentService', ['schedule']);
    patientServiceSpy = jasmine.createSpyObj<PatientService>('PatientService', ['search']);
    clinicServiceSpy = jasmine.createSpyObj<ClinicService>('ClinicService', ['getClinicUsers', 'getActiveClinics']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    appointmentServiceSpy.schedule.and.returnValue(of(appointmentResponse));
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
      imports: [AppointmentFormComponent, NoopAnimationsModule],
      providers: [
        { provide: AppointmentService, useValue: appointmentServiceSpy },
        { provide: PatientService, useValue: patientServiceSpy },
        { provide: ClinicService, useValue: clinicServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: {} } },
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: AuthService, useValue: { currentUser: () => ({ clinicId: 'clinic-1' }) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppointmentFormComponent);
    component = fixture.componentInstance;
    spyOn((component as never)['snack'] as MatSnackBar, 'open').and.callFake(snackSpy.open);
    fixture.detectChanges();
  });

  it('should load dentists on init', () => {
    expect(clinicServiceSpy.getClinicUsers).toHaveBeenCalledWith('clinic-1');
    expect(component.dentists.length).toBe(1);
    expect(component.dentists[0].id).toBe('dentist-1');
  });

  it('should navigate to scheduling on cancel', () => {
    component.onCancel();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/scheduling']);
  });

  it('should not submit and should show feedback when form is invalid', () => {
    component.onSubmit();

    expect(appointmentServiceSpy.schedule).not.toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalledWith('Revisa los campos obligatorios del formulario', 'OK', { duration: 3000 });
  });

  it('should block submit when end date is before start date', () => {
    component.form.patchValue({
      patientId: 'patient-1',
      dentistId: 'dentist-1',
      consultationReason: 'Control',
      startDateTime: '2099-01-01T10:00',
      endDateTime: '2099-01-01T09:00'
    });

    component.onSubmit();

    expect(component.form.hasError('invalidDateRange')).toBeTrue();
    expect(appointmentServiceSpy.schedule).not.toHaveBeenCalled();
  });

  it('should submit with normalized datetime payload and navigate on success', () => {
    component.onPatientSelected(patient);
    component.onDentistSelected(dentist);
    component.form.patchValue({
      consultationReason: 'Revisión anual',
      startDateTime: '2099-01-01T10:00',
      endDateTime: '2099-01-01T10:30',
      notes: 'Sin incidencias'
    });

    component.onSubmit();

    expect(appointmentServiceSpy.schedule).toHaveBeenCalledWith(
      jasmine.objectContaining({
        patientId: 'patient-1',
        dentistId: 'dentist-1',
        consultationReason: 'Revisión anual',
        startDateTime: '2099-01-01T10:00:00',
        endDateTime: '2099-01-01T10:30:00'
      })
    );
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/scheduling']);
  });

  it('should stop loading on submit error and let global handling show the message', () => {
    appointmentServiceSpy.schedule.and.returnValue(
      throwError(() => new HttpErrorResponse({
        status: 400,
        statusText: 'Bad Request',
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Request validation failed',
          fieldErrors: [
            {
              field: 'startDateTime',
              message: 'debe ser una fecha futura'
            }
          ]
        }
      }))
    );

    component.onPatientSelected(patient);
    component.onDentistSelected(dentist);
    component.form.patchValue({
      consultationReason: 'Control',
      startDateTime: '2099-01-01T10:00',
      endDateTime: '2099-01-01T10:30'
    });

    component.onSubmit();

    expect(snackSpy.open).not.toHaveBeenCalled();
    expect(component.loading).toBeFalse();
  });
});

