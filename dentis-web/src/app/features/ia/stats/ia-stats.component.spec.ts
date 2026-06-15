import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { IaStatsComponent } from './ia-stats.component';
import { IaService } from '../../../core/services/ia.service';
import { AuthService } from '../../../core/services/auth.service';
import { IaStatsResponse } from '../../../core/models/ia.model';

describe('IaStatsComponent', () => {
  let component: IaStatsComponent;
  let fixture: ComponentFixture<IaStatsComponent>;
  let iaSpy: jasmine.SpyObj<IaService>;
  let authSpy: jasmine.SpyObj<AuthService>;

  const statsData: IaStatsResponse = {
    totalSessions: 10,
    totalMessages: 50,
    totalInputTokens: 1000,
    totalOutputTokens: 2000,
    rows: [
      { username: 'dr.house', clinicName: 'Clínica A', messages: 20, inputTokens: 500, outputTokens: 1000 }
    ]
  };

  beforeEach(async () => {
    iaSpy = jasmine.createSpyObj<IaService>('IaService', ['getStats']);
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getRole'], {
      currentUser: signal({ username: 'dr.house', role: 'USER', token: 'tok', clinicId: 'clinic-1' } as never)
    });

    iaSpy.getStats.and.returnValue(of(statsData));
    authSpy.getRole.and.returnValue('USER');

    await TestBed.configureTestingModule({
      imports: [IaStatsComponent, NoopAnimationsModule],
      providers: [
        { provide: IaService, useValue: iaSpy },
        { provide: AuthService, useValue: authSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IaStatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load stats on init', () => {
    expect(component).toBeTruthy();
    expect(iaSpy.getStats).toHaveBeenCalled();
    expect(component.stats()).toEqual(statsData);
    expect(component.loading()).toBeFalse();
  });

  it('should set displayedColumns without clinicName for non-super-admin', () => {
    expect(component.isSuperAdmin).toBeFalse();
    expect(component.displayedColumns).not.toContain('clinicName');
    expect(component.displayedColumns).toContain('username');
  });

  it('should set displayedColumns with clinicName for SUPER_ADMIN', async () => {
    authSpy.getRole.and.returnValue('SUPER_ADMIN');
    fixture = TestBed.createComponent(IaStatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isSuperAdmin).toBeTrue();
    expect(component.displayedColumns).toContain('clinicName');
  });

  it('should calculate estimatedCost correctly', () => {
    const cost = component.estimatedCost();
    const expected = (1000 / 1000 * 0.0008) + (2000 / 1000 * 0.0032);
    expect(cost).toBeCloseTo(expected, 6);
  });

  it('should set loading to false on error', async () => {
    iaSpy.getStats.and.returnValue(throwError(() => new Error('Network error')));
    fixture = TestBed.createComponent(IaStatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.loading()).toBeFalse();
  });
});
