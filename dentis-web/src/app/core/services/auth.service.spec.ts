import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    localStorage.clear();
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    localStorage.clear();
    httpMock.verify();
  });

  it('should persist session after successful login', () => {
    service.login({ username: 'jbello', password: 'Admin@2026!' }).subscribe();

    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({
      success: true,
      data: {
        token: 'jwt-token',
        expiresIn: 86400000,
        username: 'jbello',
        role: 'SUPER_ADMIN',
        clinicId: undefined
      },
      timestamp: new Date().toISOString()
    });

    expect(service.getToken()).toBe('jwt-token');
    expect(service.currentUser()?.username).toBe('jbello');
    expect(service.getRole()).toBe('SUPER_ADMIN');
  });

  it('should clear session and navigate to login on logout', () => {
    localStorage.setItem('dentis_token', 'jwt-token');
    localStorage.setItem('dentis_user', JSON.stringify({ username: 'jbello', role: 'ADMIN', token: 'jwt-token' }));
    service.currentUser.set({ username: 'jbello', role: 'ADMIN', token: 'jwt-token' });

    service.logout();

    expect(localStorage.getItem('dentis_token')).toBeNull();
    expect(localStorage.getItem('dentis_user')).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should validate role membership', () => {
    service.currentUser.set({ username: 'jbello', role: 'SUPER_ADMIN', token: 'jwt-token' });

    expect(service.hasAnyRole(['SUPER_ADMIN'])).toBeTrue();
    expect(service.hasAnyRole(['ADMIN', 'USER'])).toBeFalse();
  });
});

