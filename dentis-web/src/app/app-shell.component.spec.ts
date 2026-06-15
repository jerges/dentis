import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { AppShellComponent } from './app-shell.component';
import { AuthService } from './core/services/auth.service';

describe('AppShellComponent', () => {
  let component: AppShellComponent;
  let fixture: ComponentFixture<AppShellComponent>;
  let authSpy: jasmine.SpyObj<AuthService>;

  const mockUser = { username: 'jbello', role: 'USER', token: 'tok', clinicId: 'c-1', staffType: 'DENTIST' };

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getRole', 'logout'], {
      currentUser: signal(mockUser as never)
    });
    authSpy.getRole.and.returnValue('USER');

    await TestBed.configureTestingModule({
      imports: [AppShellComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle sidenav open state', () => {
    const initial = component.sidenavOpen();
    component.toggleMenu();
    expect(component.sidenavOpen()).toBe(!initial);
  });

  it('should close sidenav on mobile when closeMenuOnMobile is called', () => {
    component.isMobile.set(true);
    component.sidenavOpen.set(true);
    component.closeMenuOnMobile();
    expect(component.sidenavOpen()).toBeFalse();
  });

  it('should not close sidenav when not mobile', () => {
    component.isMobile.set(false);
    component.sidenavOpen.set(true);
    component.closeMenuOnMobile();
    expect(component.sidenavOpen()).toBeTrue();
  });
});
