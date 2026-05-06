import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadgeComponent, StatusTone } from './status-badge.component';

describe('StatusBadgeComponent', () => {
  let component: StatusBadgeComponent;
  let fixture: ComponentFixture<StatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(StatusBadgeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render provided label', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Active');
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.textContent).toContain('Active');
    });
  });

  it('should apply correct CSS class for default tone (active)', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Active');
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-active')).toBe(true);
    });
  });

  it('should apply correct CSS class for inactive tone', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Inactive');
      fixture.componentRef.setInput('tone', 'inactive' as StatusTone);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-inactive')).toBe(true);
    });
  });

  it('should apply correct CSS class for pending tone', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Pending');
      fixture.componentRef.setInput('tone', 'pending' as StatusTone);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-pending')).toBe(true);
    });
  });

  it('should apply correct CSS class for scheduled tone', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Scheduled');
      fixture.componentRef.setInput('tone', 'scheduled' as StatusTone);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-scheduled')).toBe(true);
    });
  });

  it('should apply correct CSS class for completed tone', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Completed');
      fixture.componentRef.setInput('tone', 'completed' as StatusTone);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-completed')).toBe(true);
    });
  });

  it('should apply correct CSS class for cancelled tone', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Cancelled');
      fixture.componentRef.setInput('tone', 'cancelled' as StatusTone);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-cancelled')).toBe(true);
    });
  });

  it('should apply correct CSS class for approved tone', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Approved');
      fixture.componentRef.setInput('tone', 'approved' as StatusTone);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-approved')).toBe(true);
    });
  });

  it('should apply correct CSS class for confirmed tone', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Confirmed');
      fixture.componentRef.setInput('tone', 'confirmed' as StatusTone);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-confirmed')).toBe(true);
    });
  });

  it('should apply correct CSS class for in-progress tone', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'In Progress');
      fixture.componentRef.setInput('tone', 'in-progress' as StatusTone);
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-in-progress')).toBe(true);
    });
  });

  it('should update label when input changes', () => {
    fixture = TestBed.createComponent(StatusBadgeComponent);

    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'First Label');
      fixture.detectChanges();

      let badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.textContent).toContain('First Label');

      fixture.componentRef.setInput('label', 'Second Label');
      fixture.detectChanges();

      badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.textContent).toContain('Second Label');
    });
  });

  it('should update CSS class when tone changes', () => {
    fixture = TestBed.createComponent(StatusBadgeComponent);

    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Status');
      fixture.componentRef.setInput('tone', 'active' as StatusTone);
      fixture.detectChanges();

      let badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-active')).toBe(true);

      fixture.componentRef.setInput('tone', 'completed' as StatusTone);
      fixture.detectChanges();

      badge = fixture.nativeElement.querySelector('.status-chip');
      expect(badge.classList.contains('status-completed')).toBe(true);
    });
  });
});

