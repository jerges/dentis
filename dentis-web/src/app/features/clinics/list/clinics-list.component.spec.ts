import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { ClinicsListComponent } from './clinics-list.component';
import { ClinicService } from '../../../core/services/clinic.service';
import { ApiResponse, PageResponse } from '../../../core/models/api.model';
import { Clinic } from '../../../core/models/clinic.model';

describe('ClinicsListComponent', () => {
  let component: ClinicsListComponent;
  let fixture: ComponentFixture<ClinicsListComponent>;
  let clinicServiceSpy: jasmine.SpyObj<ClinicService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  beforeEach(async () => {
    clinicServiceSpy = jasmine.createSpyObj<ClinicService>('ClinicService', ['getClinics', 'deactivateClinic']);
    dialogSpy = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);

    const pageResponse: ApiResponse<PageResponse<Clinic>> = {
      success: true,
      data: {
        content: [{ id: '1', name: 'Dental Norte', city: 'Madrid', active: true }],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        last: true
      },
      timestamp: new Date().toISOString()
    };

    clinicServiceSpy.getClinics.and.returnValue(of(pageResponse));

    await TestBed.configureTestingModule({
      imports: [ClinicsListComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: ClinicService, useValue: clinicServiceSpy },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicsListComponent);
    component = fixture.componentInstance;
    spyOn((component as never)['dialog'] as MatDialog, 'open').and.callFake(dialogSpy.open);
    fixture.detectChanges();
  });

  it('should load clinics on init', () => {
    expect(clinicServiceSpy.getClinics).toHaveBeenCalledWith(0, 10);
    expect(component.dataSource.data.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('should show error message when loading fails', () => {
    clinicServiceSpy.getClinics.and.returnValue(throwError(() => new Error('fail')));

    component.load();

    expect(component.errorMessage).toBe('No se pudieron cargar las clínicas.');
    expect(component.loading).toBeFalse();
  });

  it('should deactivate clinic after confirmation', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as never);
    const deactivateResponse: ApiResponse<null> = {
      success: true,
      data: null,
      timestamp: new Date().toISOString()
    };
    clinicServiceSpy.deactivateClinic.and.returnValue(of(deactivateResponse));

    component.deactivate('1');

    expect(dialogSpy.open).toHaveBeenCalled();
    expect(clinicServiceSpy.deactivateClinic).toHaveBeenCalledWith('1');
  });
});

