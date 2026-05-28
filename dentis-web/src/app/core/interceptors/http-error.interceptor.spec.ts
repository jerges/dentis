import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpContext } from '@angular/common/http';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { httpErrorInterceptor, SKIP_GLOBAL_ERROR_HANDLER } from './http-error.interceptor';

describe('httpErrorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(() => {
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting()
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should show backend error message in snackbar', () => {
    http.get('/api/test').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/test');
    req.flush({ error: { message: 'Clinic already exists' } }, { status: 409, statusText: 'Conflict' });

    expect(snackSpy.open).toHaveBeenCalledWith('Clinic already exists', 'Cerrar', { duration: 5500 });
  });

  it('should show formatted validation field errors in snackbar', () => {
    http.post('/api/patients', {}).subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/patients');
    req.flush(
      {
        code: 'VALIDATION_ERROR',
        message: 'Request validation failed',
        fieldErrors: [
          {
            field: 'birthDate',
            message: 'debe ser una fecha pasada'
          }
        ]
      },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(snackSpy.open).toHaveBeenCalledWith('birthDate: debe ser una fecha pasada', 'Cerrar', { duration: 5500 });
  });

  it('should not show snackbar when request skips global error handling', () => {
    const context = new HttpContext().set(SKIP_GLOBAL_ERROR_HANDLER, true);

    http.get('/api/silent-check', { context }).subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/silent-check');
    req.flush({}, { status: 500, statusText: 'Server Error' });

    expect(snackSpy.open).not.toHaveBeenCalled();
  });
});

