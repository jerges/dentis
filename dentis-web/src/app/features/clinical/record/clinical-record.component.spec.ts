import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { ClinicalRecordComponent } from './clinical-record.component';
import { ClinicalService } from '../../../core/services/clinical.service';
import { PatientService } from '../../../core/services/patient.service';
import { PdfService } from '../../../core/services/pdf.service';
import { ClinicalRecord } from '../../../core/models/clinical.model';
import { Patient } from '../../../core/models/patient.model';

const mockPatient: Patient = {
  id: 'patient-uuid',
  firstName: 'Jane',
  lastName: 'Doe',
  idDocument: '12345678',
  birthDate: '1990-01-01',
  sex: 'FEMALE',
  gender: 'FEMALE',
  contactInfo: {},
  active: true,
};

const mockRecord: ClinicalRecord = {
  id: 'record-uuid',
  patientId: 'patient-uuid',
  dentitionType: 'PERMANENT',
  odontogram: [],
  evolutions: [],
  diagnoses: [],
  treatmentPlans: [],
};

const updatedRecord: ClinicalRecord = {
  ...mockRecord,
  evolutions: [{ id: 'evo-1', dentistId: 'dent-1', description: 'First visit', recordedAt: '2024-01-01T10:00:00Z' }],
};

describe('ClinicalRecordComponent', () => {
  let component: ClinicalRecordComponent;
  let fixture: ComponentFixture<ClinicalRecordComponent>;

  let clinicalSvcSpy: jasmine.SpyObj<ClinicalService>;
  let patientSvcSpy: jasmine.SpyObj<PatientService>;
  let pdfSvcSpy: jasmine.SpyObj<PdfService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    clinicalSvcSpy = jasmine.createSpyObj<ClinicalService>('ClinicalService', [
      'getOrCreate', 'addEvolution', 'addDiagnosis', 'addTreatmentPlan',
    ]);
    patientSvcSpy = jasmine.createSpyObj<PatientService>('PatientService', ['getById']);
    pdfSvcSpy = jasmine.createSpyObj<PdfService>('PdfService', ['exportClinicalRecord']);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    clinicalSvcSpy.getOrCreate.and.returnValue(of(mockRecord));
    patientSvcSpy.getById.and.returnValue(of(mockPatient));

    await TestBed.configureTestingModule({
      imports: [ClinicalRecordComponent, NoopAnimationsModule],
      providers: [
        { provide: ClinicalService, useValue: clinicalSvcSpy },
        { provide: PatientService, useValue: patientSvcSpy },
        { provide: PdfService, useValue: pdfSvcSpy },
        { provide: MatSnackBar, useValue: snackSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicalRecordComponent);
    component = fixture.componentInstance;
    // Set required input via the component directly (Angular 18 signal input)
    (component as any)['patientId'] = () => 'patient-uuid';
    fixture.detectChanges();
  });

  it('should load patient and clinical record on init', () => {
    expect(patientSvcSpy.getById).toHaveBeenCalledWith('patient-uuid');
    expect(clinicalSvcSpy.getOrCreate).toHaveBeenCalledWith('patient-uuid');
    expect(component.patient).toEqual(mockPatient);
    expect(component.record).toEqual(mockRecord);
    expect(component.loading()).toBeFalse();
  });

  it('should set loading to false even when getOrCreate fails', () => {
    clinicalSvcSpy.getOrCreate.and.returnValue(throwError(() => new Error('Network error')));
    component.ngOnInit();
    expect(component.loading()).toBeFalse();
    expect(component.record).toBeNull();
  });

  it('should submit evolution successfully and reset form', () => {
    clinicalSvcSpy.addEvolution.and.returnValue(of(updatedRecord));
    component.showAddEvolution = true;
    component.evolutionForm.setValue({ description: 'Test evolution', findings: '', treatment: '' });

    component.submitEvolution();

    expect(clinicalSvcSpy.addEvolution).toHaveBeenCalledWith('patient-uuid', jasmine.objectContaining({
      description: 'Test evolution',
    }));
    expect(component.record).toEqual(updatedRecord);
    expect(component.showAddEvolution).toBeFalse();
    expect(component.savingEvolution()).toBeFalse();
    expect(snackSpy.open).toHaveBeenCalledWith('Evolución registrada', 'OK', { duration: 3000 });
  });

  it('should show error snack when addEvolution fails', () => {
    clinicalSvcSpy.addEvolution.and.returnValue(throwError(() => new Error('fail')));
    component.evolutionForm.setValue({ description: 'Test', findings: '', treatment: '' });

    component.submitEvolution();

    expect(component.savingEvolution()).toBeFalse();
    expect(snackSpy.open).toHaveBeenCalledWith('Error al guardar evolución', 'Cerrar', { duration: 4000 });
  });

  it('should not submit evolution when form is invalid', () => {
    component.evolutionForm.setValue({ description: '', findings: '', treatment: '' });
    component.submitEvolution();
    expect(clinicalSvcSpy.addEvolution).not.toHaveBeenCalled();
  });

  it('should call exportClinicalRecord when exportPdf is called with patient and record', () => {
    component.exportPdf();
    expect(pdfSvcSpy.exportClinicalRecord).toHaveBeenCalledWith(mockPatient, mockRecord);
  });

  it('should submit diagnosis and show success snack', () => {
    const diagRecord: ClinicalRecord = { ...mockRecord, diagnoses: [{ id: 'd-1', code: 'K02.1', description: 'Caries' }] };
    clinicalSvcSpy.addDiagnosis.and.returnValue(of(diagRecord));
    component.diagnosisForm.setValue({ code: 'K02.1', description: 'Caries', toothNumber: null });

    component.submitDiagnosis();

    expect(clinicalSvcSpy.addDiagnosis).toHaveBeenCalledWith('patient-uuid', jasmine.objectContaining({ code: 'K02.1', description: 'Caries' }));
    expect(component.record).toEqual(diagRecord);
    expect(snackSpy.open).toHaveBeenCalledWith('Diagnóstico registrado', 'OK', { duration: 3000 });
  });

  it('should submit treatment plan and show success snack', () => {
    const planRecord: ClinicalRecord = { ...mockRecord, treatmentPlans: [{ id: 'p-1', dentistId: 'dent-1', title: 'Plan A', status: 'PROPOSED' }] };
    clinicalSvcSpy.addTreatmentPlan.and.returnValue(of(planRecord));
    component.planForm.setValue({ title: 'Plan A', description: '' });

    component.submitPlan();

    expect(clinicalSvcSpy.addTreatmentPlan).toHaveBeenCalledWith('patient-uuid', jasmine.objectContaining({ title: 'Plan A' }));
    expect(component.record).toEqual(planRecord);
    expect(snackSpy.open).toHaveBeenCalledWith('Plan de tratamiento guardado', 'OK', { duration: 3000 });
  });
});
