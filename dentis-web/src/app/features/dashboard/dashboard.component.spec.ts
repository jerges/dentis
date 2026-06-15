import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DashboardComponent } from './dashboard.component';
import { environment } from '@environments/environment';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let httpMock: HttpTestingController;

  const statsUrl = `${environment.apiUrl}/api/v1/dashboard/stats`;

  const mockStats = {
    totalPatients: 120,
    todayAppointments: 5,
    weekAppointments: 22,
    totalRevenue: 4500.75
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule, RouterTestingModule, HttpClientTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should request dashboard stats on init', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(statsUrl);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: mockStats, timestamp: new Date().toISOString() });
  });

  it('should populate statCards with values from API response', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(statsUrl);
    req.flush({ success: true, data: mockStats, timestamp: new Date().toISOString() });

    expect(component.statCards[0].value).toBe(120);
    expect(component.statCards[1].value).toBe(5);
    expect(component.statCards[2].value).toBe(22);
  });

  it('should format totalRevenue as currency string', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(statsUrl);
    req.flush({ success: true, data: mockStats, timestamp: new Date().toISOString() });

    expect(String(component.statCards[3].value)).toContain('$');
    expect(String(component.statCards[3].value)).toContain('4.500');
  });

  it('should keep default dash values when API fails', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(statsUrl);
    req.error(new ProgressEvent('Network error'));

    expect(component.statCards[0].value).toBe('—');
    expect(component.statCards[1].value).toBe('—');
  });

  it('should have 4 stat cards with correct routes', () => {
    fixture.detectChanges();
    httpMock.expectOne(statsUrl).flush({ success: true, data: mockStats, timestamp: new Date().toISOString() });

    expect(component.statCards.length).toBe(4);
    expect(component.statCards[0].route).toBe('/patients');
    expect(component.statCards[3].route).toBe('/billing/payments');
  });

  it('should handle zero revenue gracefully', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(statsUrl);
    req.flush({ success: true, data: { ...mockStats, totalRevenue: 0 }, timestamp: new Date().toISOString() });

    expect(String(component.statCards[3].value)).toContain('$0');
  });
});
