import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { GlobalSearchComponent } from './global-search.component';
import { PatientService } from '../../../core/services/patient.service';
import { PageResponse } from '../../../core/models/api.model';
import { Patient } from '../../../core/models/patient.model';

describe('GlobalSearchComponent', () => {
  let component: GlobalSearchComponent;
  let fixture: ComponentFixture<GlobalSearchComponent>;
  let patientSpy: jasmine.SpyObj<PatientService>;

  const emptyPage: PageResponse<Patient> = {
    content: [], page: 0, size: 0, totalElements: 0, totalPages: 0, last: true
  };

  const patientPage: PageResponse<Patient> = {
    content: [{
      id: 'p-1', firstName: 'Ana', lastName: 'Lopez',
      idDocument: 'V-123', active: true
    } as Patient],
    page: 0, size: 8, totalElements: 1, totalPages: 1, last: true
  };

  beforeEach(async () => {
    patientSpy = jasmine.createSpyObj<PatientService>('PatientService', ['search']);
    patientSpy.search.and.returnValue(of(patientPage));

    await TestBed.configureTestingModule({
      imports: [GlobalSearchComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: PatientService, useValue: patientSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GlobalSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with search closed', () => {
    expect(component).toBeTruthy();
    expect(component.open()).toBeFalse();
  });

  it('should open and close search panel via toggle()', () => {
    component.toggle();
    expect(component.open()).toBeTrue();

    component.toggle();
    expect(component.open()).toBeFalse();
  });

  it('should clear query and results when closed', () => {
    component.openSearch();
    component.query = 'Ana';
    component.close();

    expect(component.open()).toBeFalse();
    expect(component.query).toBe('');
    expect(component.results().length).toBe(0);
  });

  it('should search and set results after debounce', fakeAsync(() => {
    component.openSearch();
    component.onQuery('Ana');
    tick(300);

    expect(patientSpy.search).toHaveBeenCalledWith('Ana', 0, 8);
    expect(component.results().length).toBe(1);
    expect(component.results()[0].label).toBe('Lopez, Ana');
  }));

  it('should not search when query is less than 2 chars', fakeAsync(() => {
    component.openSearch();
    component.onQuery('A');
    tick(300);

    expect(patientSpy.search).not.toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  }));
});
