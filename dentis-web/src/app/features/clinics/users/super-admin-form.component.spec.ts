import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { ClinicService } from '../../../core/services/clinic.service';
import { ApiResponse } from '../../../core/models/api.model';
import { ClinicUser } from '../../../core/models/clinic.model';
import { SuperAdminFormComponent } from './super-admin-form.component';

describe('SuperAdminFormComponent', () => {
  let component: SuperAdminFormComponent;
  let fixture: ComponentFixture<SuperAdminFormComponent>;
  let clinicServiceSpy: jasmine.SpyObj<ClinicService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    clinicServiceSpy = jasmine.createSpyObj<ClinicService>('ClinicService', ['createGlobalClinicUser']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    routerSpy.navigate.and.returnValue(Promise.resolve(true));

    const response: ApiResponse<ClinicUser> = {
      success: true,
      data: {
        id: 'user-1',
        username: 'global-admin',
        fullName: 'Global Admin',
        email: 'global.admin@dentis.dev',
        role: 'SUPER_ADMIN',
        staffType: 'ADMINISTRATIVE',
        active: true
      },
      timestamp: new Date().toISOString()
    };

    clinicServiceSpy.createGlobalClinicUser.and.returnValue(of(response));

    await TestBed.configureTestingModule({
      imports: [SuperAdminFormComponent, NoopAnimationsModule],
      providers: [
        { provide: ClinicService, useValue: clinicServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SuperAdminFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create super admin without clinic', () => {
    component.form.patchValue({
      username: 'global-admin',
      fullName: 'Global Admin',
      email: 'global.admin@dentis.dev',
      password: 'Admin1234'
    });

    component.save();

    expect(clinicServiceSpy.createGlobalClinicUser).toHaveBeenCalledWith({
      username: 'global-admin',
      fullName: 'Global Admin',
      email: 'global.admin@dentis.dev',
      password: 'Admin1234',
      role: 'SUPER_ADMIN',
      staffType: 'ADMINISTRATIVE'
    });
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/clinics']);
  });
});

