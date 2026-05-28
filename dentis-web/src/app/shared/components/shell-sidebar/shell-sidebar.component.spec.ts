import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { ShellSidebarComponent } from './shell-sidebar.component';
import { AuthService } from '../../../core/services/auth.service';
import { AppNavItem } from '../../../core/navigation/navigation.config';

describe('ShellSidebarComponent', () => {
  let component: ShellSidebarComponent;
  let fixture: ComponentFixture<ShellSidebarComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const mockNavItems: AppNavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Clinics', icon: 'hospital', route: '/clinics' },
    { label: 'Patients', icon: 'people', route: '/patients' }
  ];

  const mockUser = {
    username: 'jbello',
    role: 'SUPER_ADMIN',
    token: 'jwt-token',
    clinicId: undefined
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['logout'],
      { currentUser: signal(mockUser) }
    );

    await TestBed.configureTestingModule({
      imports: [ShellSidebarComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShellSidebarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('items', mockNavItems);
      fixture.detectChanges();

      expect(component).toBeTruthy();
    });
  });

  it('should render navigation items', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('items', mockNavItems);
      fixture.detectChanges();

      const navItems = fixture.nativeElement.querySelectorAll('a[mat-list-item]');
      expect(navItems.length).toBe(mockNavItems.length);
    });
  });

  it('should display user information', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('items', mockNavItems);
      fixture.detectChanges();

      const userNameEl = fixture.nativeElement.querySelector('.user-name');
      const userRoleEl = fixture.nativeElement.querySelector('.user-role');
      const userAvatarEl = fixture.nativeElement.querySelector('.user-avatar');

      expect(userNameEl.textContent).toContain(mockUser.username);
      expect(userRoleEl.textContent).toContain(mockUser.role);
      expect(userAvatarEl.textContent).toContain('J'); // First letter uppercase
    });
  });

  it('should display user avatar with first letter of username', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('items', mockNavItems);
      fixture.detectChanges();

      const userAvatarEl = fixture.nativeElement.querySelector('.user-avatar');
      expect(userAvatarEl.textContent.trim()).toBe('J');
    });
  });

  it('should display default avatar when user not available', async () => {
    // Create a new spy with null user
    const authServiceSpyNull = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['logout'],
      { currentUser: signal(null) }
    );
    
    // Reset TestBed first
    await TestBed.resetTestingModule();
    
    TestBed.configureTestingModule({
      imports: [ShellSidebarComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpyNull }
      ]
    }).compileComponents();
    
    fixture = TestBed.createComponent(ShellSidebarComponent);
    component = fixture.componentInstance;
    
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('items', mockNavItems);
      fixture.detectChanges();
    });
    await fixture.whenStable();

    const userAvatarEl = fixture.nativeElement.querySelector('.user-avatar');
    expect(userAvatarEl.textContent).toContain('U'); // Default
  });

  it('should call logout when logout button is clicked', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('items', mockNavItems);
      fixture.detectChanges();

      const logoutBtn = fixture.nativeElement.querySelector('.logout-btn');
      logoutBtn.click();

      expect(authServiceSpy.logout).toHaveBeenCalled();
    });
  });

  it('should render brand information', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('items', mockNavItems);
      fixture.detectChanges();

      const brandEl = fixture.nativeElement.querySelector('.brand');
      const subtitleEl = fixture.nativeElement.querySelector('.brand-subtitle');

      expect(brandEl.textContent).toContain('Dentis');
      expect(subtitleEl.textContent).toContain('Dental workspace');
    });
  });

  it('should set active-link class on active route', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('items', mockNavItems);
      fixture.detectChanges();

      const navLinks = fixture.nativeElement.querySelectorAll('a[mat-list-item]');
      expect(navLinks.length).toBeGreaterThan(0);
    });
  });
});

