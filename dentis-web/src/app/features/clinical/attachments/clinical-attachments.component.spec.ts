import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { ClinicalAttachmentsComponent } from './clinical-attachments.component';
import { ClinicalService } from '../../../core/services/clinical.service';
import { ClinicalAttachment } from '../../../core/models/clinical.model';

const mockAttachment: ClinicalAttachment = {
  id: 'att-1',
  fileName: 'xray.jpg',
  contentType: 'image/jpeg',
  fileSize: 204800,
  uploadedAt: '2024-03-01T10:00:00Z',
  downloadUrl: 'https://example.com/xray.jpg',
};

const mockAttachmentPdf: ClinicalAttachment = {
  id: 'att-2',
  fileName: 'report.pdf',
  contentType: 'application/pdf',
  fileSize: 512000,
  uploadedAt: '2024-03-02T10:00:00Z',
};

describe('ClinicalAttachmentsComponent', () => {
  let component: ClinicalAttachmentsComponent;
  let fixture: ComponentFixture<ClinicalAttachmentsComponent>;

  let clinicalSvcSpy: jasmine.SpyObj<ClinicalService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    clinicalSvcSpy = jasmine.createSpyObj<ClinicalService>('ClinicalService', [
      'listAttachments', 'presignAttachmentUpload', 'uploadToS3',
      'registerAttachment', 'deleteAttachment',
    ]);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    clinicalSvcSpy.listAttachments.and.returnValue(of([mockAttachment]));

    await TestBed.configureTestingModule({
      imports: [ClinicalAttachmentsComponent, NoopAnimationsModule],
      providers: [
        { provide: ClinicalService, useValue: clinicalSvcSpy },
        { provide: MatSnackBar, useValue: snackSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicalAttachmentsComponent);
    component = fixture.componentInstance;
    // Set required signal input
    (component as any)['patientId'] = () => 'patient-uuid';
    (component as any)['toothNumber'] = () => null;
    fixture.detectChanges();
  });

  it('should load attachments on init', () => {
    expect(clinicalSvcSpy.listAttachments).toHaveBeenCalledWith('patient-uuid', undefined);
    expect(component.attachments()).toEqual([mockAttachment]);
    expect(component.loading()).toBeFalse();
  });

  it('should set loading to false when listAttachments fails', () => {
    clinicalSvcSpy.listAttachments.and.returnValue(throwError(() => new Error('fail')));
    component.ngOnInit();

    expect(component.loading()).toBeFalse();
    expect(component.attachments()).toEqual([]);
  });

  it('should upload file through presign -> s3 -> register pipeline and prepend to list', () => {
    clinicalSvcSpy.presignAttachmentUpload.and.returnValue(of({ s3Key: 'key/xray2.jpg', uploadUrl: 'https://s3.example.com/upload' }));
    clinicalSvcSpy.uploadToS3.and.returnValue(of(undefined as unknown as void));
    clinicalSvcSpy.registerAttachment.and.returnValue(of(mockAttachmentPdf));

    const mockFile = new File(['data'], 'report.pdf', { type: 'application/pdf' });
    const event = { target: { files: [mockFile], value: '' } } as unknown as Event;
    component.onFileSelected(event);

    expect(clinicalSvcSpy.presignAttachmentUpload).toHaveBeenCalledWith('patient-uuid', 'report.pdf', 'application/pdf');
    expect(clinicalSvcSpy.uploadToS3).toHaveBeenCalledWith('https://s3.example.com/upload', mockFile);
    expect(clinicalSvcSpy.registerAttachment).toHaveBeenCalled();
    expect(component.attachments()[0]).toEqual(mockAttachmentPdf);
    expect(component.uploading()).toBeFalse();
    expect(snackSpy.open).toHaveBeenCalledWith('Archivo subido correctamente', 'OK', { duration: 3000 });
  });

  it('should show error snack when upload fails', () => {
    clinicalSvcSpy.presignAttachmentUpload.and.returnValue(throwError(() => new Error('presign fail')));

    const mockFile = new File(['data'], 'fail.jpg', { type: 'image/jpeg' });
    const event = { target: { files: [mockFile], value: '' } } as unknown as Event;
    component.onFileSelected(event);

    expect(component.uploading()).toBeFalse();
    expect(snackSpy.open).toHaveBeenCalledWith('Error al subir el archivo', 'Cerrar', { duration: 4000 });
  });

  it('should delete attachment and remove it from the list', () => {
    clinicalSvcSpy.deleteAttachment.and.returnValue(of(undefined as unknown as void));
    component.attachments.set([mockAttachment]);

    component.deleteAttachment(mockAttachment);

    expect(clinicalSvcSpy.deleteAttachment).toHaveBeenCalledWith('patient-uuid', 'att-1');
    expect(component.attachments().find(a => a.id === 'att-1')).toBeUndefined();
  });

  it('should show error snack when deleteAttachment fails', () => {
    clinicalSvcSpy.deleteAttachment.and.returnValue(throwError(() => new Error('delete fail')));
    component.attachments.set([mockAttachment]);

    component.deleteAttachment(mockAttachment);

    expect(snackSpy.open).toHaveBeenCalledWith('Error al eliminar', 'Cerrar', { duration: 3000 });
  });

  it('should correctly classify content types via isImage and formatSize', () => {
    expect(component.isImage('image/jpeg')).toBeTrue();
    expect(component.isImage('image/png')).toBeTrue();
    expect(component.isImage('application/pdf')).toBeFalse();

    expect(component.formatSize(0)).toBe('');
    expect(component.formatSize(500)).toBe('500 B');
    expect(component.formatSize(2048)).toBe('2 KB');
    expect(component.formatSize(1572864)).toBe('1.5 MB');
  });
});
