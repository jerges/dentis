import { TestBed } from '@angular/core/testing';
import { PdfService } from './pdf.service';
import { Patient } from '../models/patient.model';
import { ClinicalRecord } from '../models/clinical.model';

const mockPatient: Patient = {
  id: 'p1',
  firstName: 'Ana',
  lastName: 'López',
  idDocument: 'V-12345678',
  birthDate: '1990-05-10',
  sex: 'FEMALE',
  gender: 'FEMALE',
  contactInfo: { email: 'ana@example.com' },
  active: true,
};

const mockRecord: ClinicalRecord = {
  id: 'rec-1',
  patientId: 'p1',
  dentitionType: 'PERMANENT',
  odontogram: [],
  evolutions: [
    {
      dentistId: 'dentist-1',
      description: 'First visit',
      findings: 'Caries on tooth 18',
      treatment: 'Composite filling',
      recordedAt: '2026-01-10T10:00:00Z',
    },
  ],
  diagnoses: [
    {
      code: 'K02.1',
      description: 'Caries of dentine',
      toothNumber: 18,
      diagnosedAt: '2026-01-10',
    },
  ],
  treatmentPlans: [
    {
      dentistId: 'dentist-1',
      title: 'Plan A',
      description: 'Restorative treatment',
      status: 'IN_PROGRESS',
    },
  ],
};

describe('PdfService', () => {
  let service: PdfService;
  let mockWin: jasmine.SpyObj<Window>;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [PdfService] });
    service = TestBed.inject(PdfService);

    mockWin = jasmine.createSpyObj<Window>('Window', ['focus', 'print']);
    mockWin.document = jasmine.createSpyObj('document', ['write', 'close']);
    spyOn(window, 'open').and.returnValue(mockWin as any);
  });

  it('should open a new window when exporting a clinical record', () => {
    service.exportClinicalRecord(mockPatient, mockRecord);

    expect(window.open).toHaveBeenCalledWith('', '_blank', 'width=900,height=700');
  });

  it('should write HTML content with the patient name to the new window', () => {
    service.exportClinicalRecord(mockPatient, mockRecord);

    const writeSpy = mockWin.document.write as jasmine.Spy;
    expect(writeSpy).toHaveBeenCalled();
    const html: string = writeSpy.calls.mostRecent().args[0];
    expect(html).toContain('Ana López');
    expect(html).toContain('V-12345678');
  });

  it('should include evolution data in the generated HTML', () => {
    service.exportClinicalRecord(mockPatient, mockRecord);

    const writeSpy = mockWin.document.write as jasmine.Spy;
    const html: string = writeSpy.calls.mostRecent().args[0];
    expect(html).toContain('First visit');
    expect(html).toContain('Caries on tooth 18');
    expect(html).toContain('Composite filling');
  });

  it('should include diagnosis and treatment plan in the generated HTML', () => {
    service.exportClinicalRecord(mockPatient, mockRecord);

    const writeSpy = mockWin.document.write as jasmine.Spy;
    const html: string = writeSpy.calls.mostRecent().args[0];
    expect(html).toContain('K02.1');
    expect(html).toContain('Caries of dentine');
    expect(html).toContain('Plan A');
    expect(html).toContain('IN_PROGRESS');
  });

  it('should render empty-state messages when record has no entries', () => {
    const emptyRecord: ClinicalRecord = {
      ...mockRecord,
      evolutions: [],
      diagnoses: [],
      treatmentPlans: [],
    };

    service.exportClinicalRecord(mockPatient, emptyRecord);

    const writeSpy = mockWin.document.write as jasmine.Spy;
    const html: string = writeSpy.calls.mostRecent().args[0];
    expect(html).toContain('Sin evoluciones registradas.');
    expect(html).toContain('Sin diagnósticos registrados.');
    expect(html).toContain('Sin planes de tratamiento.');
  });

  it('should do nothing when window.open returns null', () => {
    (window.open as jasmine.Spy).and.returnValue(null);

    expect(() => service.exportClinicalRecord(mockPatient, mockRecord)).not.toThrow();
  });
});
