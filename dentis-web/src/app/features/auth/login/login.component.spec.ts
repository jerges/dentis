import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { ApiResponse } from '../../../core/models/api.model';
import { AuthResponse } from '../../../core/models/auth.model';

const mockAuthResponse: ApiResponse<AuthResponse> = {
  success: true,
  timestamp: new Date().toISOString(),
  data: {
    token: 'jwt-token',
    username: 'admin',
    role: 'ADMIN',
    clinicId: 'clinic-1',
    expiresIn: 3600000,
  },
};

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  let authSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and render the form with empty controls', () => {
    expect(component).toBeTruthy();
    expect(component.form.get('username')?.value).toBe('');
    expect(component.form.get('password')?.value).toBe('');
    expect(component.loading).toBeFalse();
    expect(component.errorMessage).toBe('');
  });

  it('should mark form touched and not call auth.login when form is invalid', () => {
    component.form.setValue({ username: '', password: '' });
    component.onSubmit();

    expect(authSpy.login).not.toHaveBeenCalled();
    expect(component.form.get('username')?.touched).toBeTrue();
    expect(component.form.get('password')?.touched).toBeTrue();
  });

  it('should call auth.login and navigate to /dashboard on success', () => {
    authSpy.login.and.returnValue(of(mockAuthResponse));
    component.form.setValue({ username: 'admin', password: 'secret' });

    component.onSubmit();

    expect(authSpy.login).toHaveBeenCalledWith({ username: 'admin', password: 'secret' });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should set errorMessage and stop loading on login failure', () => {
    authSpy.login.and.returnValue(throwError(() => new Error('Unauthorized')));
    component.form.setValue({ username: 'wrong', password: 'wrong' });

    component.onSubmit();

    expect(component.loading).toBeFalse();
    expect(component.errorMessage).toBe('Credenciales incorrectas. Intente de nuevo.');
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('should toggle hidePassword state', () => {
    expect(component.hidePassword).toBeTrue();
    component.hidePassword = !component.hidePassword;
    expect(component.hidePassword).toBeFalse();
  });

  it('should clear errorMessage and set loading true before auth call', () => {
    let loadingDuringCall = false;
    authSpy.login.and.callFake(() => {
      loadingDuringCall = component.loading;
      return of(mockAuthResponse);
    });
    component.errorMessage = 'previous error';
    component.form.setValue({ username: 'admin', password: 'pass' });

    component.onSubmit();

    expect(loadingDuringCall).toBeTrue();
    expect(component.errorMessage).toBe('');
  });
});
