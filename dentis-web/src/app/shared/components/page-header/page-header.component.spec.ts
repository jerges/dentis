import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { PageHeaderComponent } from './page-header.component';

describe('PageHeaderComponent', () => {
  let component: PageHeaderComponent;
  let fixture: ComponentFixture<PageHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageHeaderComponent, NoopAnimationsModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(PageHeaderComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.componentRef.setInput('title', 'Test Title');
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render title', () => {
    fixture.componentRef.setInput('title', 'Test Title');
    fixture.detectChanges();

    const titleEl = fixture.nativeElement.querySelector('.page-title');
    expect(titleEl.textContent).toContain('Test Title');
  });

  it('should render subtitle when provided', () => {
    fixture.componentRef.setInput('title', 'Test Title');
    fixture.componentRef.setInput('subtitle', 'Test Subtitle');
    fixture.detectChanges();

    const subtitleEl = fixture.nativeElement.querySelector('.subtitle');
    expect(subtitleEl).toBeTruthy();
    expect(subtitleEl.textContent).toContain('Test Subtitle');
  });

  it('should not render subtitle when not provided', () => {
    fixture.componentRef.setInput('title', 'Test Title');
    fixture.detectChanges();

    const subtitleEl = fixture.nativeElement.querySelector('.subtitle');
    expect(subtitleEl).toBeFalsy();
  });

  it('should render back button when backRoute is provided', () => {
    fixture.componentRef.setInput('title', 'Test Title');
    fixture.componentRef.setInput('backRoute', '/previous');
    fixture.detectChanges();

    const backBtn = fixture.nativeElement.querySelector('.back-btn');
    expect(backBtn).toBeTruthy();
  });

  it('should not render back button when backRoute is empty', () => {
    fixture.componentRef.setInput('title', 'Test Title');
    fixture.detectChanges();

    const backBtn = fixture.nativeElement.querySelector('.back-btn');
    expect(backBtn).toBeFalsy();
  });

  it('should have correct routerLink on back button', () => {
    fixture.componentRef.setInput('title', 'Test Title');
    fixture.componentRef.setInput('backRoute', '/patients');
    fixture.detectChanges();
    fixture.debugElement.nativeElement.querySelectorAll('[routerLink]').forEach((el: any) => {
      if (el.classList.contains('back-btn')) {
        expect(el.getAttribute('routerLink')).toBe('/patients');
      }
    });
  });

  it('should project content into header-actions', () => {
    fixture.componentRef.setInput('title', 'Test Title');
    fixture.detectChanges();

    const actionsContainer = fixture.nativeElement.querySelector('.header-actions');
    expect(actionsContainer).toBeTruthy();
  });

  it('should have proper header bar structure', () => {
    fixture.componentRef.setInput('title', 'Test Title');
    fixture.detectChanges();

    const headerBar = fixture.nativeElement.querySelector('.page-header');
    expect(headerBar).toBeTruthy();

    const titleBlock = fixture.nativeElement.querySelector('.header-title-block');
    expect(titleBlock).toBeTruthy();

    const spacer = fixture.nativeElement.querySelector('.spacer');
    expect(spacer).toBeTruthy();

    const actionsContainer = fixture.nativeElement.querySelector('.header-actions');
    expect(actionsContainer).toBeTruthy();
  });

  it('should render empty title by default', () => {
    fixture.detectChanges();

    const titleEl = fixture.nativeElement.querySelector('.page-title');
    expect(titleEl).toBeTruthy();
    expect(titleEl.textContent.trim()).toBe('');
  });

  it('should render with all inputs', () => {
    fixture.componentRef.setInput('title', 'Clinics Management');
    fixture.componentRef.setInput('subtitle', 'Manage all dental clinics');
    fixture.componentRef.setInput('backRoute', '/dashboard');
    fixture.detectChanges();

    const titleEl = fixture.nativeElement.querySelector('.page-title');
    const subtitleEl = fixture.nativeElement.querySelector('.subtitle');
    const backBtn = fixture.nativeElement.querySelector('.back-btn');

    expect(titleEl.textContent).toContain('Clinics Management');
    expect(subtitleEl.textContent).toContain('Manage all dental clinics');
    expect(backBtn).toBeTruthy();
  });
});

