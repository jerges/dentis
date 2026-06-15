import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PatientService } from './patient.service';
import { Patient } from '../models/patient.model';

const BASE = '/api/v1/patients';

const mockPatient: Patient = {
  id: 'patient-1',
  firstName: 'Ana',
  lastName: 'García',
  email: 'ana.garcia@example.com',
  phone: '+34600000001',
  active: true
} as unknown as Patient;

describe('PatientService', () => {
  let service: PatientService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PatientService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(PatientService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get all patients with default pagination sorted by lastName', () => {
    service.getAll().subscribe();

    const req = httpMock.expectOne(`${BASE}?page=0&size=20&sort=lastName`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { content: [mockPatient], totalElements: 1, page: 0, size: 20, totalPages: 1, last: true }, timestamp: new Date().toISOString() });
  });

  it('should search patients by name', () => {
    service.search('García', 0, 20).subscribe();

    const req = httpMock.expectOne(`${BASE}/search?name=Garc%C3%ADa&page=0&size=20`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { content: [mockPatient], totalElements: 1, page: 0, size: 20, totalPages: 1, last: true }, timestamp: new Date().toISOString() });
  });

  it('should get a patient by id', () => {
    let result: Patient | undefined;

    service.getById('patient-1').subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/patient-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: mockPatient, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockPatient);
  });

  it('should create a patient', () => {
    const payload = { firstName: 'Ana', lastName: 'García', email: 'ana.garcia@example.com', phone: '+34600000001' };
    let result: Patient | undefined;

    service.create(payload as any).subscribe((r) => (result = r));

    const req = httpMock.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: mockPatient, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockPatient);
  });

  it('should update a patient', () => {
    const payload = { phone: '+34600000002' };
    const updated = { ...mockPatient, phone: '+34600000002' };
    let result: Patient | undefined;

    service.update('patient-1', payload as any).subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/patient-1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: updated, timestamp: new Date().toISOString() });
    expect(result?.phone).toBe('+34600000002');
  });

  it('should deactivate a patient', () => {
    service.deactivate('patient-1').subscribe();

    const req = httpMock.expectOne(`${BASE}/patient-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
