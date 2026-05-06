import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClinicService } from './clinic.service';

describe('ClinicService', () => {
  let service: ClinicService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ClinicService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ClinicService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should request paginated clinics', () => {
    service.getClinics(2, 15).subscribe();

    const req = httpMock.expectOne('/api/v1/clinics?page=2&size=15');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { content: [], page: 2, size: 15, totalElements: 0, totalPages: 0, last: true }, timestamp: new Date().toISOString() });
  });

  it('should update a clinic', () => {
    const payload = { name: 'Dental Norte', city: 'Madrid' };

    service.updateClinic('clinic-1', payload).subscribe();

    const req = httpMock.expectOne('/api/v1/clinics/clinic-1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: { id: 'clinic-1', active: true, ...payload }, timestamp: new Date().toISOString() });
  });

  it('should create a clinic user', () => {
    const payload = {
      username: 'doctor1',
      email: 'doctor1@dentis.dev',
      password: 'Admin1234',
      fullName: 'Doctor One',
      role: 'MEDICO' as const
    };

    service.createClinicUser('clinic-1', payload).subscribe();

    const req = httpMock.expectOne('/api/v1/clinics/clinic-1/users');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: { id: 'user-1', active: true, ...payload }, timestamp: new Date().toISOString() });
  });

  it('should delete a clinic user', () => {
    service.deactivateClinicUser('clinic-1', 'user-77').subscribe();

    const req = httpMock.expectOne('/api/v1/clinics/clinic-1/users/user-77');
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, data: null, timestamp: new Date().toISOString() });
  });
});

