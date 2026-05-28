import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { BackendHealthService } from './backend-health.service';

describe('BackendHealthService', () => {
  let service: BackendHealthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(BackendHealthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should return true when backend health status is UP', () => {
    let result = false;

    service.check().subscribe((value) => {
      result = value;
    });

    const request = httpMock.expectOne('/actuator/health');
    expect(request.request.responseType).toBe('text');
    request.flush('{"status":"UP"}');

    expect(result).toBeTrue();
  });

  it('should return true when backend healthcheck responds with an empty 200 body', () => {
    let result = false;

    service.check().subscribe((value) => {
      result = value;
    });

    const request = httpMock.expectOne('/actuator/health');
    request.flush('');

    expect(result).toBeTrue();
  });

  it('should return false when backend healthcheck returns an unexpected response body', () => {
    let result = true;

    service.check().subscribe((value) => {
      result = value;
    });

    const request = httpMock.expectOne('/actuator/health');
    request.flush('<!doctype html><html></html>');

    expect(result).toBeFalse();
  });

  it('should return false when backend healthcheck fails', () => {
    let result = true;

    service.check().subscribe((value) => {
      result = value;
    });

    const request = httpMock.expectOne('/actuator/health');
    request.flush({}, { status: 503, statusText: 'Service Unavailable' });

    expect(result).toBeFalse();
  });
});

