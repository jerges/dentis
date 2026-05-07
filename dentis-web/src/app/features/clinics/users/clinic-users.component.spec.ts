import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { ClinicUsersComponent } from './clinic-users.component';
import { AuthService } from '../../../core/services/auth.service';
import { ClinicService } from '../../../core/services/clinic.service';
import { ApiResponse } from '../../../core/models/api.model';
import { ClinicUser } from '../../../core/models/clinic.model';

describe('ClinicUsersComponent', () => {
  let component: ClinicUsersComponent;
  let fixture: ComponentFixture<ClinicUsersComponent>;
  let clinicServiceSpy: jasmine.SpyObj<ClinicService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  beforeEach(async () => {
    clinicServiceSpy = jasmine.createSpyObj<ClinicService>('ClinicService', [
      'getClinicUsers',
      'createClinicUser',
      'updateClinicUser',
      'deactivateClinicUser'
    ]);
    dialogSpy = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);

    const getUsersResponse: ApiResponse<ClinicUser[]> = {
      success: true,
      data: [{ id: 'user-1', username: 'doctor1', fullName: 'Doctor One', email: 'doctor1@dentis.dev', role: 'USER', staffType: 'DENTIST', active: true }],
      timestamp: new Date().toISOString()
    };
    const createUserResponse: ApiResponse<ClinicUser> = {
      success: true,
      data: { id: 'user-2', username: 'admin1', fullName: 'Admin One', email: 'admin1@dentis.dev', role: 'ADMIN', staffType: 'ADMINISTRATIVE', active: true },
      timestamp: new Date().toISOString()
    };
    const deactivateResponse: ApiResponse<null> = {
      success: true,
      data: null,
      timestamp: new Date().toISOString()
    };

    clinicServiceSpy.getClinicUsers.and.returnValue(of(getUsersResponse));
    clinicServiceSpy.createClinicUser.and.returnValue(of(createUserResponse));
    clinicServiceSpy.updateClinicUser.and.returnValue(of(createUserResponse));
    clinicServiceSpy.deactivateClinicUser.and.returnValue(of(deactivateResponse));

    await TestBed.configureTestingModule({
      imports: [ClinicUsersComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            currentUser: () => ({ username: 'admin', role: 'ADMIN', token: 'token', clinicId: 'clinic-1' })
          }
        },
        { provide: ClinicService, useValue: clinicServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ clinicId: 'clinic-1' }) } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicUsersComponent);
    component = fixture.componentInstance;
    spyOn((component as never)['dialog'] as MatDialog, 'open').and.callFake(dialogSpy.open);
    fixture.detectChanges();
  });

  it('should load users on init', () => {
    expect(clinicServiceSpy.getClinicUsers).toHaveBeenCalledWith('clinic-1');
    expect(component.dataSource.data.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('should create a dentist user with USER access', () => {
    component.form.patchValue({
      username: 'doctor2',
      fullName: 'Doctor Two',
      email: 'doctor2@dentis.dev',
      password: 'Admin1234',
      staffType: 'DENTIST',
      role: 'USER'
    });

    component.saveUser();

    expect(clinicServiceSpy.createClinicUser).toHaveBeenCalledWith('clinic-1', {
      username: 'doctor2',
      fullName: 'Doctor Two',
      email: 'doctor2@dentis.dev',
      password: 'Admin1234',
      staffType: 'DENTIST',
      role: 'USER'
    });
  });

  it('should enforce USER role for dentist even if ADMIN is provided', () => {
    component.form.patchValue({
      username: 'doctor3',
      fullName: 'Doctor Three',
      email: 'doctor3@dentis.dev',
      password: 'Admin1234',
      staffType: 'DENTIST',
      role: 'ADMIN'
    });

    component.saveUser();

    expect(clinicServiceSpy.createClinicUser).toHaveBeenCalledWith('clinic-1', {
      username: 'doctor3',
      fullName: 'Doctor Three',
      email: 'doctor3@dentis.dev',
      password: 'Admin1234',
      staffType: 'DENTIST',
      role: 'USER'
    });
  });

  it('should update an administrative user in edit mode', () => {
    component.startEdit({
      id: 'user-1',
      username: 'assistant1',
      fullName: 'Assistant One',
      email: 'assistant1@dentis.dev',
      role: 'ADMIN',
      staffType: 'ADMINISTRATIVE',
      active: true
    });

    component.form.patchValue({
      fullName: 'Assistant Updated',
      password: ''
    });

    component.saveUser();

    expect(clinicServiceSpy.updateClinicUser).toHaveBeenCalledWith('clinic-1', 'user-1', {
      username: 'assistant1',
      fullName: 'Assistant Updated',
      email: 'assistant1@dentis.dev',
      password: undefined,
      staffType: 'ADMINISTRATIVE',
      role: 'ADMIN'
    });
  });

  it('should deactivate user after confirmation', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as never);

    component.deactivateUser('user-1');

    expect(dialogSpy.open).toHaveBeenCalled();
    expect(clinicServiceSpy.deactivateClinicUser).toHaveBeenCalledWith('clinic-1', 'user-1');
  });
});

