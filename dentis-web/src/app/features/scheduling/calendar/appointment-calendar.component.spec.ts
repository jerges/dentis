import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AppointmentCalendarComponent } from './appointment-calendar.component';
import { AppointmentService } from '../../../core/services/appointment.service';
import { ClinicService } from '../../../core/services/clinic.service';
import { AuthService } from '../../../core/services/auth.service';
import { Appointment } from '../../../core/models/appointment.model';
import { ClinicUser } from '../../../core/models/clinic.model';

describe('AppointmentCalendarComponent', () => {
  let component: AppointmentCalendarComponent;
  let fixture: ComponentFixture<AppointmentCalendarComponent>;
  let appointmentSpy: jasmine.SpyObj<AppointmentService>;
  let clinicSpy: jasmine.SpyObj<ClinicService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const dentist: ClinicUser = {
    id: 'dentist-1',
    username: 'dr.house',
    email: 'house@test.com',
    fullName: 'Gregory House',
    role: 'USER',
    staffType: 'DENTIST',
    active: true
  };

  const appointment: Appointment = {
    id: 'apt-1',
    patientId: 'patient-1',
    patientName: 'Ana Lopez',
    dentistId: 'dentist-1',
    startDateTime: new Date().toISOString(),
    endDateTime: new Date().toISOString(),
    status: 'SCHEDULED',
    consultationReason: 'Checkup'
  } as unknown as Appointment;

  beforeEach(async () => {
    appointmentSpy = jasmine.createSpyObj<AppointmentService>('AppointmentService', ['getByDentist']);
    clinicSpy = jasmine.createSpyObj<ClinicService>('ClinicService', ['getClinicUsers', 'getActiveClinics']);
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getRole'], {
      currentUser: signal({ username: 'dr.house', role: 'USER', token: 'tok', clinicId: 'clinic-1' } as never)
    });
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    appointmentSpy.getByDentist.and.returnValue(of([appointment]));
    clinicSpy.getClinicUsers.and.returnValue(
      of({ success: true, data: [dentist], timestamp: new Date().toISOString() })
    );

    await TestBed.configureTestingModule({
      imports: [AppointmentCalendarComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: AppointmentService, useValue: appointmentSpy },
        { provide: ClinicService, useValue: clinicSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: MatSnackBar, useValue: snackSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppointmentCalendarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and build a 7-day week grid on init', () => {
    expect(component).toBeTruthy();
    expect(component.weekDays.length).toBe(7);
  });

  it('should load dentists from clinic on init and auto-select the first one', () => {
    expect(clinicSpy.getClinicUsers).toHaveBeenCalledWith('clinic-1');
    expect(component.dentists.length).toBe(1);
    expect(component.selectedDentistId).toBe('dentist-1');
  });

  it('should load appointments for selected dentist', () => {
    expect(appointmentSpy.getByDentist).toHaveBeenCalledWith(
      'dentist-1',
      jasmine.any(String),
      jasmine.any(String)
    );
    expect(component.appointments.length).toBe(1);
    expect(component.appointments[0].id).toBe('apt-1');
  });

  it('should navigate to the previous week', () => {
    const originalStart = new Date(component.currentWeekStart);
    component.prevWeek();
    const diff = originalStart.getTime() - component.currentWeekStart.getTime();
    expect(diff).toBe(7 * 24 * 60 * 60 * 1000);
  });

  it('should display patient name or fallback to patientId prefix', () => {
    expect(component.displayPatient(appointment)).toBe('Ana Lopez');

    const noName = { ...appointment, patientName: '', patientId: 'abcdefgh-1234' };
    expect(component.displayPatient(noName as Appointment)).toBe('Paciente abcdefgh');
  });
});
