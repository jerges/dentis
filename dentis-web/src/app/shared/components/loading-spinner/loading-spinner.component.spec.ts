import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { LoadingSpinnerComponent } from './loading-spinner.component';

describe('LoadingSpinnerComponent', () => {
  let component: LoadingSpinnerComponent;
  let fixture: ComponentFixture<LoadingSpinnerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingSpinnerComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingSpinnerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with default inputs', () => {
    expect(component).toBeTruthy();
    expect(component.diameter()).toBe(40);
    expect(component.message()).toBe('');
  });

  it('should reflect custom diameter input', () => {
    fixture.componentRef.setInput('diameter', 64);
    fixture.detectChanges();
    expect(component.diameter()).toBe(64);
  });

  it('should render message paragraph when message is provided', () => {
    fixture.componentRef.setInput('message', 'Cargando datos...');
    fixture.detectChanges();

    const msgEl: HTMLElement = fixture.nativeElement.querySelector('.msg');
    expect(msgEl).toBeTruthy();
    expect(msgEl.textContent).toContain('Cargando datos...');
  });

  it('should not render message paragraph when message is empty', () => {
    fixture.componentRef.setInput('message', '');
    fixture.detectChanges();

    const msgEl = fixture.nativeElement.querySelector('.msg');
    expect(msgEl).toBeNull();
  });
});
