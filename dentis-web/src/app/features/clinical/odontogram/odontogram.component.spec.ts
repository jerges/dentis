import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { OdontogramComponent } from './odontogram.component';
import { ClinicalService } from '../../../core/services/clinical.service';
import { PatientService } from '../../../core/services/patient.service';
import { ClinicalRecord, OdontogramTooth } from '../../../core/models/clinical.model';
import { Patient } from '../../../core/models/patient.model';

const mockPatient: Patient = {
  id: 'patient-uuid',
  firstName: 'Carlos',
  lastName: 'Lopez',
  idDocument: '87654321',
  birthDate: '1985-06-15',
  sex: 'MALE',
  gender: 'MALE',
  contactInfo: {},
  active: true,
};

const mockRecord: ClinicalRecord = {
  id: 'record-uuid',
  patientId: 'patient-uuid',
  dentitionType: 'PERMANENT',
  odontogram: [],
  evolutions: [],
  diagnoses: [{ id: 'd-1', code: 'K02.1', description: 'Caries', toothNumber: 16 }],
  treatmentPlans: [],
};

const savedRecord: ClinicalRecord = {
  ...mockRecord,
  odontogram: [{ toothNumber: 11, condition: 'CARIES' }],
};

describe('OdontogramComponent', () => {
  let component: OdontogramComponent;
  let fixture: ComponentFixture<OdontogramComponent>;

  let clinicalSvcSpy: jasmine.SpyObj<ClinicalService>;
  let patientSvcSpy: jasmine.SpyObj<PatientService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    clinicalSvcSpy = jasmine.createSpyObj<ClinicalService>('ClinicalService', [
      'getOrCreate', 'updateOdontogram', 'updateDentitionType',
    ]);
    patientSvcSpy = jasmine.createSpyObj<PatientService>('PatientService', ['getById']);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    clinicalSvcSpy.getOrCreate.and.returnValue(of(mockRecord));
    clinicalSvcSpy.updateOdontogram.and.returnValue(of(savedRecord));
    clinicalSvcSpy.updateDentitionType.and.returnValue(of(mockRecord));
    patientSvcSpy.getById.and.returnValue(of(mockPatient));

    await TestBed.configureTestingModule({
      imports: [OdontogramComponent, NoopAnimationsModule],
      providers: [
        { provide: ClinicalService, useValue: clinicalSvcSpy },
        { provide: PatientService, useValue: patientSvcSpy },
        { provide: MatSnackBar, useValue: snackSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OdontogramComponent);
    component = fixture.componentInstance;
    (component as any)['patientId'] = () => 'patient-uuid';
    fixture.detectChanges();
  });

  it('should load patient and record on init and build teeth', () => {
    expect(patientSvcSpy.getById).toHaveBeenCalledWith('patient-uuid');
    expect(clinicalSvcSpy.getOrCreate).toHaveBeenCalledWith('patient-uuid');
    expect(component.patient).toEqual(mockPatient);
    expect(component.record).toEqual(mockRecord);
    expect(component.upperTeeth.length).toBe(16);
    expect(component.lowerTeeth.length).toBe(16);
    expect(component.dentitionType).toBe('PERMANENT');
  });

  it('should save odontogram successfully and show snack', () => {
    component.save();

    expect(clinicalSvcSpy.updateOdontogram).toHaveBeenCalledWith('patient-uuid', jasmine.any(Array));
    expect(component.saving()).toBeFalse();
    expect(component.selectedTooth).toBeNull();
    expect(snackSpy.open).toHaveBeenCalledWith('Odontograma guardado', 'OK', { duration: 3000 });
  });

  it('should show error snack when save fails', () => {
    clinicalSvcSpy.updateOdontogram.and.returnValue(throwError(() => new Error('fail')));
    component.save();

    expect(component.saving()).toBeFalse();
    expect(snackSpy.open).toHaveBeenCalledWith('Error al guardar', 'Cerrar', { duration: 4000 });
  });

  it('should select a tooth and set default active surface', () => {
    const tooth: OdontogramTooth = { toothNumber: 21, condition: 'HEALTHY' };
    component.selectTooth(tooth, 'crown');

    expect(component.selectedTooth).toBe(tooth);
    expect(component.activeSurface).toBe('OCCLUSAL');
    expect(component.detailTab).toBe('crown');
  });

  it('should toggle root finding on a tooth', () => {
    const tooth: OdontogramTooth = { toothNumber: 21, condition: 'HEALTHY', rootFindings: [] };
    component.toggleRootFinding(tooth, 'ENDODONTIC_TREATMENT');
    expect(tooth.rootFindings).toContain('ENDODONTIC_TREATMENT');

    component.toggleRootFinding(tooth, 'ENDODONTIC_TREATMENT');
    expect(tooth.rootFindings).not.toContain('ENDODONTIC_TREATMENT');
  });

  it('should return tooth diagnoses for the selected tooth', () => {
    const tooth: OdontogramTooth = { toothNumber: 16, condition: 'CARIES' };
    component.selectedTooth = tooth;

    const diags = component.toothDiagnoses;

    expect(diags.length).toBe(1);
    expect(diags[0].code).toBe('K02.1');
  });

  it('should rebuild teeth and call updateDentitionType when dentition changes', () => {
    component.onDentitionChange('PRIMARY');

    expect(clinicalSvcSpy.updateDentitionType).toHaveBeenCalledWith('patient-uuid', 'PRIMARY');
    expect(component.upperTeeth.length).toBe(10);
    expect(component.lowerTeeth.length).toBe(10);
    expect(component.selectedTooth).toBeNull();
  });
});
