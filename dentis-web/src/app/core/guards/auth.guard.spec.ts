import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let router: Router;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'isAuthenticated',
      'hasAnyRole'
    ]);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    router = TestBed.inject(Router);
  });

  it('should allow navigation when authenticated and route has no role restriction', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({ data: {} } as never, {} as never)
    );

    expect(result).toBeTrue();
  });

  it('should redirect to /login when user is not authenticated', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({ data: {} } as never, {} as never)
    ) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/login');
  });

  it('should redirect to /dashboard when user lacks required role', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.hasAnyRole.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({ data: { roles: ['SUPER_ADMIN'] } } as never, {} as never)
    ) as UrlTree;

    expect(authServiceSpy.hasAnyRole).toHaveBeenCalledWith(['SUPER_ADMIN']);
    expect(router.serializeUrl(result)).toBe('/dashboard');
  });
});

