import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { PatientsListComponent } from './patients-list.component';
import { PatientService } from '../../../core/services/patient.service';
import { Patient } from '../../../core/models/patient.model';

describe('PatientsListComponent', () => {
  let component: PatientsListComponent;
  let fixture: ComponentFixture<PatientsListComponent>;

  let patientServiceSpy: jasmine.SpyObj<PatientService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const patient: Patient = {
    id: 'patient-1',
    firstName: 'Ana',
    lastName: 'Lopez',
    idDocument: 'V-12345678',
    active: true,
    birthDate: '1990-01-10',
    sex: 'FEMALE',
    gender: 'FEMALE',
    contactInfo: {
      email: 'ana@dentis.dev',
      phoneNumber: '600000000'
    }
  };

  const pageResponse = {
    content: [patient],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    last: true
  };

  beforeEach(async () => {
    patientServiceSpy = jasmine.createSpyObj<PatientService>('PatientService', ['getAll', 'search']);
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    patientServiceSpy.getAll.and.returnValue(of(pageResponse));
    patientServiceSpy.search.and.returnValue(of(pageResponse));

    await TestBed.configureTestingModule({
      imports: [PatientsListComponent, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: PatientService, useValue: patientServiceSpy },
        { provide: MatSnackBar, useValue: snackSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PatientsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load patients on init', () => {
    expect(patientServiceSpy.getAll).toHaveBeenCalledWith(0, 20);
    expect(component.dataSource.data.length).toBe(1);
    expect(component.totalElements).toBe(1);
  });

  it('should update dataSource with returned patients', () => {
    expect(component.dataSource.data[0].id).toBe('patient-1');
    expect(component.dataSource.data[0].firstName).toBe('Ana');
  });

  it('should call getAll with updated page on onPageChange', () => {
    patientServiceSpy.getAll.calls.reset();
    patientServiceSpy.getAll.and.returnValue(of(pageResponse));

    component.onPageChange({ pageIndex: 2, pageSize: 20, length: 100 });

    expect(component.currentPage).toBe(2);
    expect(patientServiceSpy.getAll).toHaveBeenCalledWith(2, 20);
  });

  it('should have correct displayedColumns', () => {
    expect(component.displayedColumns).toEqual(['name', 'contact', 'birthDate', 'status', 'actions']);
  });

  it('should update totalElements from empty search result', () => {
    patientServiceSpy.getAll.and.returnValue(
      of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, last: true })
    );
    component.loadPatients();
    expect(component.totalElements).toBe(0);
    expect(component.dataSource.data.length).toBe(0);
  });

  it('should not throw when getAll returns an error', () => {
    patientServiceSpy.getAll.and.returnValue(throwError(() => new Error('Network error')));
    expect(() => component.loadPatients()).not.toThrow();
  });
});
