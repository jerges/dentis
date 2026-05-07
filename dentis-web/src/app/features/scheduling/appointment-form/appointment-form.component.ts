import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, debounceTime, distinctUntilChanged, forkJoin, map, Observable, of, startWith, switchMap, tap } from 'rxjs';
import { AppointmentService } from '../../../core/services/appointment.service';
import { CreateAppointmentRequest } from '../../../core/models/appointment.model';
import { PatientService } from '../../../core/services/patient.service';
import { ClinicService } from '../../../core/services/clinic.service';
import { AuthService } from '../../../core/services/auth.service';
import { Patient } from '../../../core/models/patient.model';
import { Clinic, ClinicUser } from '../../../core/models/clinic.model';
import { EntityAutocompleteComponent } from '../../../shared/components/entity-autocomplete/entity-autocomplete.component';

@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatDatepickerModule,
    MatNativeDateModule, MatSnackBarModule, EntityAutocompleteComponent
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <button mat-icon-button routerLink="/scheduling"><mat-icon>arrow_back</mat-icon></button>
        <h1 class="page-title">Nueva Cita</h1>
      </div>

      <mat-card class="form-card">
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="form-grid">
            <app-entity-autocomplete
              [label]="'Paciente *'"
              [placeholder]="'Busca por nombre o documento'"
              [suffixIcon]="'person'"
              [control]="patientSearchControl"
              [options]="filteredPatients()"
              [displayWith]="displayPatient"
              [trackByValue]="trackPatient"
              [showEmptyState]="searchTerm(patientSearchControl.value).length >= 2"
              [emptyMessage]="'No patients found'"
              [errorMessage]="'Select a patient from the list'"
              (optionSelected)="onPatientSelected($event)" />

            <app-entity-autocomplete
              [label]="'Dentista *'"
              [placeholder]="'Busca por nombre del profesional'"
              [suffixIcon]="'medical_services'"
              [control]="dentistSearchControl"
              [options]="filteredDentists()"
              [displayWith]="displayDentist"
              [trackByValue]="trackDentist"
              [showEmptyState]="searchTerm(dentistSearchControl.value).length >= 1"
              [emptyMessage]="'No se encontraron dentistas'"
              [errorMessage]="'Selecciona un dentista de la lista'"
              (optionSelected)="onDentistSelected($event)" />

            <mat-form-field appearance="outline">
              <mat-label>Fecha y hora inicio *</mat-label>
              <input matInput formControlName="startDateTime" type="datetime-local" [min]="minDateTime" />
              <mat-error *ngIf="isControlInvalid('startDateTime', 'required')">La fecha de inicio es obligatoria</mat-error>
              <mat-error *ngIf="isControlInvalid('startDateTime', 'notFutureDateTime')">La fecha de inicio debe ser futura</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Fecha y hora fin *</mat-label>
              <input matInput formControlName="endDateTime" type="datetime-local" [min]="minDateTime" />
              <mat-error *ngIf="isControlInvalid('endDateTime', 'required')">La fecha de fin es obligatoria</mat-error>
              <mat-error *ngIf="isControlInvalid('endDateTime', 'notFutureDateTime')">La fecha de fin debe ser futura</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-span">
              <mat-label>Motivo de consulta *</mat-label>
              <textarea matInput formControlName="consultationReason" rows="3"></textarea>
              <mat-error *ngIf="isControlInvalid('consultationReason', 'required')">El motivo de consulta es obligatorio</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-span">
              <mat-label>Notas adicionales</mat-label>
              <textarea matInput formControlName="notes" rows="2"></textarea>
            </mat-form-field>

            <div class="full-span form-error" *ngIf="submitAttempted && form.hasError('invalidDateRange')">
              La fecha/hora de fin debe ser posterior a la fecha/hora de inicio.
            </div>

            <div class="full-span form-error" *ngIf="submitAttempted && (!form.controls.patientId.value || !form.controls.dentistId.value)">
              Selecciona paciente y dentista desde la lista para continuar.
            </div>

            <div class="form-actions full-span">
              <button mat-stroked-button type="button" (click)="onCancel()">Cancelar</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="loading">
                <mat-icon>{{ loading ? 'hourglass_top' : 'event_available' }}</mat-icon>
                {{ loading ? 'Guardando...' : 'Agendar cita' }}
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-header { gap: 12px; margin-bottom: 24px; align-items: center; }
    .page-title { margin: 0; font-size: 22px; font-weight: 700; }
    .form-card { max-width: 860px; }
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
    .full-span { grid-column: 1 / -1; }
    .form-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 8px; }
    .form-error { color: #b00020; font-size: 13px; margin-top: -4px; }
    @media (max-width: 900px) {
      .form-grid { grid-template-columns: 1fr; }
    }
  `]
})
export class AppointmentFormComponent implements OnInit {
  loading = false;
  submitAttempted = false;
  dentists: ClinicUser[] = [];

  readonly patientSearchControl = this.fb.control<string | Patient>('');
  readonly dentistSearchControl = this.fb.control<string | ClinicUser>('');

  readonly filteredPatients$: Observable<Patient[]> = this.patientSearchControl.valueChanges.pipe(
    startWith(''),
    debounceTime(250),
    distinctUntilChanged(),
    tap((value) => {
      if (typeof value === 'string') {
        this.form?.controls.patientId.setValue('');
      }
    }),
    switchMap((value) => this.searchPatients(this.searchTerm(value), value))
  );

  readonly filteredDentists$: Observable<ClinicUser[]> = this.dentistSearchControl.valueChanges.pipe(
    startWith(''),
    tap((value) => {
      if (typeof value === 'string') {
        this.form?.controls.dentistId.setValue('');
      }
    }),
    map((value) => this.filterDentists(this.searchTerm(value), value))
  );

  readonly filteredPatients = toSignal(this.filteredPatients$, { initialValue: [] as Patient[] });

  readonly filteredDentists = toSignal(this.filteredDentists$, { initialValue: [] as ClinicUser[] });

  form = this.fb.group(
    {
      patientId: ['', Validators.required],
      dentistId: ['', Validators.required],
      startDateTime: ['', [Validators.required, this.futureDateValidator()]],
      endDateTime: ['', [Validators.required, this.futureDateValidator()]],
      consultationReason: ['', [Validators.required, Validators.maxLength(500)]],
      notes: ['']
    },
    { validators: this.dateRangeValidator() }
  );

  constructor(
    private readonly fb: FormBuilder,
    private readonly appointmentService: AppointmentService,
    private readonly patientService: PatientService,
    private readonly clinicService: ClinicService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadDentists();
  }

  get minDateTime(): string {
    const now = new Date();
    now.setSeconds(0, 0);
    return this.toDatetimeLocal(now);
  }

  onSubmit(): void {
    this.submitAttempted = true;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.patientSearchControl.markAsTouched();
      this.dentistSearchControl.markAsTouched();
      this.snack.open('Revisa los campos obligatorios del formulario', 'OK', { duration: 3000 });
      return;
    }
    this.loading = true;
    const val = this.form.getRawValue();
    const payload: CreateAppointmentRequest = {
      patientId: val.patientId ?? '',
      dentistId: val.dentistId ?? '',
      consultationReason: val.consultationReason ?? '',
      notes: val.notes ?? undefined,
      startDateTime: this.normalizeLocalDateTime(val.startDateTime ?? ''),
      endDateTime: this.normalizeLocalDateTime(val.endDateTime ?? '')
    };

    this.appointmentService.schedule(payload).subscribe({
      next: () => {
        this.snack.open('Cita agendada correctamente', 'OK', { duration: 3000 });
        this.router.navigate(['/scheduling']);
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/scheduling']);
  }

  displayPatient = (patient: Patient | string | null): string => {
    if (!patient || typeof patient === 'string') {
      return patient ?? '';
    }

    return `${patient.firstName} ${patient.lastName} · ${patient.idDocument}`;
  };

  displayDentist = (dentist: ClinicUser | string | null): string => {
    if (!dentist || typeof dentist === 'string') {
      return dentist ?? '';
    }

    return `${dentist.fullName} · ${dentist.username}`;
  };

  onPatientSelected(patient: Patient): void {
    this.form.controls.patientId.setValue(patient.id);
  }

  onDentistSelected(dentist: ClinicUser): void {
    this.form.controls.dentistId.setValue(dentist.id);
  }

  trackPatient = (patient: Patient): string => patient.id;

  trackDentist = (dentist: ClinicUser): string => dentist.id;

  searchTerm(value: string | Patient | ClinicUser | null): string {
    if (typeof value === 'string') {
      return value.trim();
    }

    if (!value) {
      return '';
    }

    return 'firstName' in value ? this.displayPatient(value) : this.displayDentist(value);
  }

  private searchPatients(term: string, value: string | Patient | null): Observable<Patient[]> {
    if (value && typeof value !== 'string') {
      return of([value]);
    }

    if (term.length < 2) {
      return of([]);
    }

    return this.patientService.search(term, 0, 10).pipe(
      map((page) => page.content),
      catchError(() => of([]))
    );
  }

  private filterDentists(term: string, value: string | ClinicUser | null): ClinicUser[] {
    if (value && typeof value !== 'string') {
      return [value];
    }

    if (!term) {
      return this.dentists.slice(0, 10);
    }

    const normalizedTerm = term.toLowerCase();
    return this.dentists
      .filter((dentist) => {
        const fullName = dentist.fullName.toLowerCase();
        const username = dentist.username.toLowerCase();
        return fullName.includes(normalizedTerm) || username.includes(normalizedTerm);
      })
      .slice(0, 10);
  }

  private loadDentists(): void {
    this.getDentistSource().subscribe({
      next: (dentists) => {
        this.dentists = dentists;
      },
      error: () => {
        this.snack.open('No se pudieron cargar los dentistas', 'OK', { duration: 4000 });
      }
    });
  }

  isControlInvalid(controlName: string, errorKey: string): boolean {
    const control = this.form.get(controlName);
    if (!control) {
      return false;
    }

    return !!control.errors?.[errorKey] && (control.touched || this.submitAttempted);
  }

  private getDentistSource(): Observable<ClinicUser[]> {
    const clinicId = this.auth.currentUser()?.clinicId;
    if (clinicId) {
      return this.clinicService.getClinicUsers(clinicId).pipe(
        map((response) => this.extractActiveDentists(response.data ?? [])),
        catchError(() => of([]))
      );
    }

    return this.clinicService.getActiveClinics().pipe(
      switchMap((response) => {
        const clinics = response.data ?? [];
        if (!clinics.length) {
          return of([]);
        }

        return forkJoin(
          clinics.map((clinic: Clinic) =>
            this.clinicService.getClinicUsers(clinic.id).pipe(
              map((usersResponse) => usersResponse.data ?? []),
              catchError(() => of([]))
            )
          )
        ).pipe(
          map((usersByClinic) => usersByClinic.flat()),
          map((users) => this.extractActiveDentists(users))
        );
      }),
      catchError(() => of([]))
    );
  }

  private extractActiveDentists(users: ClinicUser[]): ClinicUser[] {
    const uniqueDentists = new Map<string, ClinicUser>();
    users
      .filter((user) => user.active && user.staffType === 'DENTIST')
      .forEach((user) => uniqueDentists.set(user.id, user));

    return Array.from(uniqueDentists.values()).sort((left, right) => left.fullName.localeCompare(right.fullName));
  }

  private futureDateValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const value = new Date(control.value);
      if (Number.isNaN(value.getTime())) {
        return { invalidDateTime: true };
      }

      return value.getTime() > Date.now() ? null : { notFutureDateTime: true };
    };
  }

  private dateRangeValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const start = control.get('startDateTime')?.value;
      const end = control.get('endDateTime')?.value;
      if (!start || !end) {
        return null;
      }

      const startDate = new Date(start);
      const endDate = new Date(end);
      if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) {
        return null;
      }

      return endDate > startDate ? null : { invalidDateRange: true };
    };
  }

  private normalizeLocalDateTime(value: string): string {
    if (!value) {
      return value;
    }

    const trimmed = value.trim().replace('Z', '');
    return trimmed.length === 16 ? `${trimmed}:00` : trimmed;
  }

  private toDatetimeLocal(value: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}`;
  }
}

