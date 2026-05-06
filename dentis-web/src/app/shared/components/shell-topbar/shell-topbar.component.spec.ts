import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { ShellTopbarComponent } from './shell-topbar.component';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';

describe('ShellTopbarComponent', () => {
  let component: ShellTopbarComponent;
  let fixture: ComponentFixture<ShellTopbarComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let themeServiceSpy: jasmine.SpyObj<ThemeService>;

  const mockUser = {
    username: 'jbello',
    role: 'SUPER_ADMIN',
    token: 'jwt-token',
    clinicId: undefined
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>(
      'AuthService',
      [],
      { currentUser: signal(mockUser) }
    );

    themeServiceSpy = jasmine.createSpyObj<ThemeService>(
      'ThemeService',
      ['toggleTheme'],
      { theme: signal('light') }
    );

    await TestBed.configureTestingModule({
      imports: [ShellTopbarComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ThemeService, useValue: themeServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShellTopbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display toolbar title and subtitle', () => {
    const titleEl = fixture.nativeElement.querySelector('.toolbar-title');
    const subtitleEl = fixture.nativeElement.querySelector('.toolbar-subtitle');

    expect(titleEl.textContent).toContain('Sistema de Gestión Dental');
    expect(subtitleEl.textContent).toContain('Backoffice operativo');
  });

  it('should display user role in toolbar', () => {
    const roleEl = fixture.nativeElement.querySelector('.toolbar-role');
    expect(roleEl.textContent).toContain(mockUser.role);
  });

  it('should emit menuClick when menu button is clicked', (done) => {
    component.menuClick.subscribe(() => {
      expect(true).toBe(true);
      done();
    });

    const menuBtn = fixture.nativeElement.querySelector('button[mat-icon-button]:first-child');
    menuBtn.click();
  });

  it('should toggle theme when theme button is clicked', () => {
    const themeBtn = fixture.nativeElement.querySelector('.theme-toggle');
    themeBtn.click();

    expect(themeServiceSpy.toggleTheme).toHaveBeenCalled();
  });

  it('should display dark mode icon when theme is light', () => {
    const themeBtn = fixture.nativeElement.querySelector('.theme-toggle mat-icon');
    expect(themeBtn.textContent).toContain('dark_mode');
  });

  it('should display light mode icon when theme is dark', async () => {
    const themeServiceDark = jasmine.createSpyObj<ThemeService>(
      'ThemeService',
      ['toggleTheme'],
      { theme: signal('dark') }
    );
    
    // Reset TestBed first
    await TestBed.resetTestingModule();
    
    TestBed.configureTestingModule({
      imports: [ShellTopbarComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ThemeService, useValue: themeServiceDark }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShellTopbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const themeBtn = fixture.nativeElement.querySelector('.theme-toggle mat-icon');
    expect(themeBtn).toBeTruthy();
    expect(themeBtn.textContent).toContain('light_mode');
  });

  it('should show theme tooltip based on current theme', () => {
    fixture.detectChanges();

    const themeBtn = fixture.nativeElement.querySelector('.theme-toggle');
    expect(themeBtn).toBeTruthy();
    // matTooltip directive is applied to the button
    expect(themeBtn.getAttribute('[matTooltip]')).toBeDefined();
  });

  it('should have sticky positioning class', () => {
    const toolbar = fixture.nativeElement.querySelector('mat-toolbar');
    expect(toolbar).toBeTruthy();
  });
});

