import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { PatientDetailComponent } from './patient-detail.component';
import { PatientService } from '../../../core/services/patient.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { Patient } from '../../../core/models/patient.model';
import { Appointment } from '../../../core/models/appointment.model';

describe('PatientDetailComponent', () => {
  let component: PatientDetailComponent;
  let fixture: ComponentFixture<PatientDetailComponent>;

  let patientServiceSpy: jasmine.SpyObj<PatientService>;
  let appointmentServiceSpy: jasmine.SpyObj<AppointmentService>;

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
  };

  const appointment: Appointment = {
    id: 'apt-1',
    patientId: 'patient-1',
    dentistId: 'dentist-1',
    dentistName: 'House',
    startDateTime: '2026-06-01T10:00:00',
    endDateTime: '2026-06-01T10:30:00',
    status: 'CONFIRMED',
    consultationReason: 'Consulta general'
  };

  beforeEach(async () => {
    patientServiceSpy = jasmine.createSpyObj<PatientService>('PatientService', ['getById']);
    appointmentServiceSpy = jasmine.createSpyObj<AppointmentService>('AppointmentService', ['getByPatient']);

    patientServiceSpy.getById.and.returnValue(of(patient));
    appointmentServiceSpy.getByPatient.and.returnValue(of([appointment]));

    await TestBed.configureTestingModule({
      imports: [PatientDetailComponent, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: PatientService, useValue: patientServiceSpy },
        { provide: AppointmentService, useValue: appointmentServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PatientDetailComponent);
    component = fixture.componentInstance;
  });

  it('should not load data when id input is empty', () => {
    fixture.detectChanges();

    expect(patientServiceSpy.getById).not.toHaveBeenCalled();
    expect(appointmentServiceSpy.getByPatient).not.toHaveBeenCalled();
    expect(component.patient).toBeNull();
    expect(component.appointments.length).toBe(0);
  });

  it('should load patient and appointments when id is provided', () => {
    TestBed.runInInjectionContext(() => {
      Object.defineProperty(component, 'id', { value: () => 'patient-1' });
    });

    component.ngOnInit();

    expect(patientServiceSpy.getById).toHaveBeenCalledWith('patient-1');
    expect(appointmentServiceSpy.getByPatient).toHaveBeenCalledWith('patient-1');
    expect(component.patient).toEqual(patient);
    expect(component.appointments.length).toBe(1);
  });

  it('should set patient data from service response', () => {
    Object.defineProperty(component, 'id', { value: () => 'patient-1' });
    component.ngOnInit();

    expect(component.patient?.firstName).toBe('Ana');
    expect(component.patient?.lastName).toBe('Lopez');
    expect(component.patient?.active).toBeTrue();
  });

  it('should set appointments from service response', () => {
    Object.defineProperty(component, 'id', { value: () => 'patient-1' });
    component.ngOnInit();

    expect(component.appointments[0].id).toBe('apt-1');
    expect(component.appointments[0].status).toBe('CONFIRMED');
  });

  it('should show empty appointments list when service returns none', () => {
    appointmentServiceSpy.getByPatient.and.returnValue(of([]));
    Object.defineProperty(component, 'id', { value: () => 'patient-1' });
    component.ngOnInit();

    expect(component.appointments.length).toBe(0);
  });
});
