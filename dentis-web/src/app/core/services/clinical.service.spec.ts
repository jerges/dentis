import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClinicalService } from './clinical.service';
import { ClinicalAttachment, ClinicalRecord } from '../models/clinical.model';

const BASE = '/api/v1/clinical-records';

const mockRecord: ClinicalRecord = {
  id: 'record-1',
  patientId: 'patient-1',
  odontogram: { teeth: [] },
  dentitionType: 'PERMANENT',
  evolutions: [],
  diagnoses: [],
  treatmentPlans: []
} as unknown as ClinicalRecord;

const mockAttachment: ClinicalAttachment = {
  id: 'attach-1',
  patientId: 'patient-1',
  fileName: 'xray.jpg',
  contentType: 'image/jpeg',
  url: 'https://s3.example.com/xray.jpg'
} as unknown as ClinicalAttachment;

describe('ClinicalService', () => {
  let service: ClinicalService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ClinicalService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ClinicalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get or create a clinical record for a patient', () => {
    let result: ClinicalRecord | undefined;

    service.getOrCreate('patient-1').subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/patient/patient-1`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: mockRecord, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockRecord);
  });

  it('should update the odontogram', () => {
    const teeth = [{ number: 11, conditions: [] }];
    let result: ClinicalRecord | undefined;

    service.updateOdontogram('patient-1', teeth as any).subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/odontogram`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ teeth });
    req.flush({ success: true, data: mockRecord, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockRecord);
  });

  it('should update the dentition type', () => {
    service.updateDentitionType('patient-1', 'MIXED').subscribe();

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/dentition-type?dentitionType=MIXED`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ success: true, data: { ...mockRecord, dentitionType: 'MIXED' }, timestamp: new Date().toISOString() });
  });

  it('should add an evolution note', () => {
    const evolution = { note: 'Patient recovering well', toothNumber: 11 };
    let result: ClinicalRecord | undefined;

    service.addEvolution('patient-1', evolution as any).subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/evolutions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(evolution);
    req.flush({ success: true, data: mockRecord, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockRecord);
  });

  it('should add a diagnosis', () => {
    const diagnosis = { code: 'K02.1', description: 'Caries' };

    service.addDiagnosis('patient-1', diagnosis as any).subscribe();

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/diagnoses`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: mockRecord, timestamp: new Date().toISOString() });
  });

  it('should add a treatment plan', () => {
    const plan = { description: 'Root canal treatment', toothNumber: 36 };

    service.addTreatmentPlan('patient-1', plan as any).subscribe();

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/treatment-plans`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: mockRecord, timestamp: new Date().toISOString() });
  });

  it('should presign an attachment upload', () => {
    const presigned = { uploadUrl: 'https://s3.example.com/presigned', key: 'xray.jpg' };

    service.presignAttachmentUpload('patient-1', 'xray.jpg', 'image/jpeg').subscribe();

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/attachments/presign`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ fileName: 'xray.jpg', contentType: 'image/jpeg' });
    req.flush({ success: true, data: presigned, timestamp: new Date().toISOString() });
  });

  it('should register an attachment', () => {
    let result: ClinicalAttachment | undefined;
    const regReq = { key: 'xray.jpg', fileName: 'xray.jpg', contentType: 'image/jpeg' };

    service.registerAttachment('patient-1', regReq as any).subscribe((r) => (result = r));

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/attachments`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: mockAttachment, timestamp: new Date().toISOString() });
    expect(result).toEqual(mockAttachment);
  });

  it('should list attachments without tooth filter', () => {
    service.listAttachments('patient-1').subscribe();

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/attachments`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockAttachment], timestamp: new Date().toISOString() });
  });

  it('should list attachments filtered by tooth number', () => {
    service.listAttachments('patient-1', 11).subscribe();

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/attachments?toothNumber=11`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockAttachment], timestamp: new Date().toISOString() });
  });

  it('should delete an attachment', () => {
    service.deleteAttachment('patient-1', 'attach-1').subscribe();

    const req = httpMock.expectOne(`${BASE}/patient/patient-1/attachments/attach-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, data: null, timestamp: new Date().toISOString() });
  });
});
