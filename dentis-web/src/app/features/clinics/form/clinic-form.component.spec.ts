import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { ClinicFormComponent } from './clinic-form.component';
import { ClinicService } from '../../../core/services/clinic.service';
import { ApiResponse } from '../../../core/models/api.model';
import { Clinic } from '../../../core/models/clinic.model';

describe('ClinicFormComponent', () => {
  let component: ClinicFormComponent;
  let fixture: ComponentFixture<ClinicFormComponent>;
  let clinicServiceSpy: jasmine.SpyObj<ClinicService>;
  let routerSpy: jasmine.SpyObj<Router>;

  async function configure(id: string | null): Promise<void> {
    clinicServiceSpy = jasmine.createSpyObj<ClinicService>('ClinicService', ['getClinic', 'createClinic', 'updateClinic']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    const getClinicResponse: ApiResponse<Clinic> = {
      success: true,
      data: { id: '1', name: 'Dental Norte', city: 'Madrid', active: true },
      timestamp: new Date().toISOString()
    };
    const createClinicResponse: ApiResponse<Clinic> = {
      success: true,
      data: { id: '1', name: 'Nueva', active: true },
      timestamp: new Date().toISOString()
    };
    const updateClinicResponse: ApiResponse<Clinic> = {
      success: true,
      data: { id: '1', name: 'Actualizada', active: true },
      timestamp: new Date().toISOString()
    };

    clinicServiceSpy.getClinic.and.returnValue(of(getClinicResponse));
    clinicServiceSpy.createClinic.and.returnValue(of(createClinicResponse));
    clinicServiceSpy.updateClinic.and.returnValue(of(updateClinicResponse));

    await TestBed.configureTestingModule({
      imports: [ClinicFormComponent, NoopAnimationsModule],
      providers: [
        { provide: ClinicService, useValue: clinicServiceSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should create clinic in create mode', async () => {
    await configure(null);

    component.form.patchValue({ name: 'Dental Sur', city: 'Sevilla' });
    component.save();

    expect(clinicServiceSpy.createClinic).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/clinics']);
  });

  it('should load clinic and update in edit mode', async () => {
    await configure('1');

    expect(clinicServiceSpy.getClinic).toHaveBeenCalledWith('1');
    expect(component.isEdit).toBeTrue();

    component.form.patchValue({ name: 'Dental Norte Editada' });
    component.save();

    expect(clinicServiceSpy.updateClinic).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/clinics']);
  });
});

