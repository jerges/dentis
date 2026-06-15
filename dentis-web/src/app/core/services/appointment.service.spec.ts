import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AppointmentService } from './appointment.service';
import { Appointment } from '../models/appointment.model';

const BASE = '/api/v1/appointments';

const mockAppointment: Appointment = {
  id: 'appt-1',
  patientId: 'patient-1',
  dentistId: 'dentist-1',
  clinicId: 'clinic-1',
  startTime: '2026-06-15T10:00:00',
  endTime: '2026-06-15T10:30:00',
  status: 'SCHEDULED',
  reason: 'Checkup'
} as Appointment;

describe('AppointmentService', () => {
  let service: AppointmentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AppointmentService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(AppointmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should schedule an appointment', () => {
    const payload = { patientId: 'patient-1', dentistId: 'dentist-1', startTime: '2026-06-15T10:00:00', endTime: '2026-06-15T10:30:00', reason: 'Checkup' };
    let result: Appointment | undefined;

    service.schedule(payload as any).subscribe((r) => (result = r));

    const req = httpMock.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: mockAppointment, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockAppointment);
  });

  it('should get an appointment by id', () => {
    let result: Appointment | undefined;

    service.getById('appt-1').subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/appt-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: mockAppointment, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockAppointment);
  });

  it('should get appointments by dentist with date range params', () => {
    let result: Appointment[] | undefined;

    service.getByDentist('dentist-1', '2026-06-01', '2026-06-30').subscribe((r) => (result = r));

    const req = httpMock.expectOne(
      `${BASE}/dentist/dentist-1?from=2026-06-01&to=2026-06-30`
    );
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockAppointment], timestamp: new Date().toISOString() });
    expect(result).toEqual([mockAppointment]);
  });

  it('should get appointments by patient', () => {
    let result: Appointment[] | undefined;

    service.getByPatient('patient-1').subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/patient/patient-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockAppointment], timestamp: new Date().toISOString() });
    expect(result).toEqual([mockAppointment]);
  });

  it('should confirm an appointment', () => {
    const confirmed = { ...mockAppointment, status: 'CONFIRMED' } as Appointment;
    let result: Appointment | undefined;

    service.confirm('appt-1').subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/appt-1/confirm`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ success: true, data: confirmed, timestamp: new Date().toISOString() });
    expect(result?.status).toBe('CONFIRMED');
  });

  it('should cancel an appointment', () => {
    service.cancel('appt-1').subscribe();

    const req = httpMock.expectOne(`${BASE}/appt-1/cancel`);
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });

  it('should reschedule an appointment with new time params', () => {
    let result: Appointment | undefined;

    service.reschedule('appt-1', '2026-06-16T10:00:00', '2026-06-16T10:30:00').subscribe((r) => (result = r));

    const req = httpMock.expectOne(
      `${BASE}/appt-1/reschedule?newStart=2026-06-16T10%3A00%3A00&newEnd=2026-06-16T10%3A30%3A00`
    );
    expect(req.request.method).toBe('PATCH');
    req.flush({ success: true, data: mockAppointment, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockAppointment);
  });
});
