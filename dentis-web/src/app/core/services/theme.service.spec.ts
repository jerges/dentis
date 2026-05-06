import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.clear();
    document.body.classList.remove('theme-light', 'theme-dark');

    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    localStorage.clear();
    document.body.classList.remove('theme-light', 'theme-dark');
  });

  it('should initialize with light theme by default', () => {
    expect(service.theme()).toBe('light');
    expect(document.body.classList.contains('theme-light')).toBeTrue();
  });

  it('should restore dark theme from localStorage', () => {
    localStorage.setItem('dentis_theme', 'dark');

    const restored = TestBed.runInInjectionContext(() => new ThemeService());

    expect(restored.theme()).toBe('dark');
    expect(document.body.classList.contains('theme-dark')).toBeTrue();
  });

  it('should toggle theme and persist it', () => {
    service.toggleTheme();

    expect(service.theme()).toBe('dark');
    expect(localStorage.getItem('dentis_theme')).toBe('dark');
    expect(document.body.classList.contains('theme-dark')).toBeTrue();
  });
});

