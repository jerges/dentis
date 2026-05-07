import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, forkJoin, map, Observable, of, startWith, switchMap, tap } from 'rxjs';
import { Appointment } from '../../../core/models/appointment.model';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClinicService } from '../../../core/services/clinic.service';
import { Clinic, ClinicUser } from '../../../core/models/clinic.model';
import { EntityAutocompleteComponent } from '../../../shared/components/entity-autocomplete/entity-autocomplete.component';

@Component({
  selector: 'app-appointment-calendar',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatDialogModule, MatSnackBarModule,
    EntityAutocompleteComponent
  ],
  template: `
    <div class="page-container">
      <div class="calendar-header flex-row">
        <div>
          <h1 class="page-title">Agenda</h1>
          <p class="page-subtitle">{{ currentDateLabel }}</p>
        </div>
        <span class="spacer"></span>
        <div class="dentist-selector">
          <app-entity-autocomplete
            [label]="'Dentista'"
            [placeholder]="'Buscar profesional'"
            [suffixIcon]="'medical_services'"
            [control]="dentistSearchControl"
            [options]="filteredDentists()"
            [displayWith]="displayDentist"
            [trackByValue]="trackDentist"
            [showEmptyState]="searchTerm(dentistSearchControl.value).length >= 1"
            [emptyMessage]="'No dentists found'"
            [errorMessage]="'Select a dentist from the list'"
            (optionSelected)="onDentistSelected($event)" />
        </div>
        <div class="flex-row gap-8">
          <button mat-icon-button (click)="prevWeek()"><mat-icon>chevron_left</mat-icon></button>
          <button mat-stroked-button (click)="goToday()">Hoy</button>
          <button mat-icon-button (click)="nextWeek()"><mat-icon>chevron_right</mat-icon></button>
        </div>
        <button mat-raised-button color="primary" routerLink="/scheduling/new">
          <mat-icon>add</mat-icon> Nueva Cita
        </button>
      </div>

      <!-- Week view -->
      <mat-card class="calendar-card">
        <mat-card-content>
          <div class="week-grid">
            @for (day of weekDays; track day.date) {
              <div class="day-column" [class.today]="day.isToday">
                <div class="day-header">
                  <span class="day-name">{{ day.name }}</span>
                  <span class="day-num" [class.today-badge]="day.isToday">{{ day.num }}</span>
                </div>
                <div class="day-appointments">
                  @for (apt of getAppointmentsForDay(day.date); track apt.id) {
                    <div class="apt-block" [class]="'apt-' + apt.status.toLowerCase()" (click)="openDetail(apt)">
                      <span class="apt-time">{{ apt.startDateTime | date:'HH:mm' }}</span>
                      <span class="apt-patient">{{ displayPatient(apt) }}</span>
                    </div>
                  }
                </div>
              </div>
            }
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Appointments list for selected week -->
      <mat-card class="list-card">
        <mat-card-header>
          <mat-card-title>Citas de la Semana</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @for (apt of appointments; track apt.id) {
            <div class="apt-row flex-row" (click)="openDetail(apt)">
              <div class="apt-datetime">
                <span class="date">{{ apt.startDateTime | date:'dd/MM' }}</span>
                <span class="time">{{ apt.startDateTime | date:'HH:mm' }}</span>
              </div>
              <div class="apt-info">
                <span class="patient">{{ displayPatient(apt) }}</span>
                <span class="reason">{{ apt.consultationReason }}</span>
              </div>
              <span class="spacer"></span>
              <span class="status-chip" [class]="'status-' + apt.status.toLowerCase()">{{ apt.status }}</span>
            </div>
          } @empty {
            <p class="empty-msg">Sin citas para esta semana</p>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .calendar-header { margin-bottom: 24px; gap: 12px; flex-wrap: wrap; }
    .dentist-selector { min-width: 280px; max-width: 360px; }
    .page-title { margin: 0 0 4px; font-size: 24px; font-weight: 700; color: #1a237e; }
    .page-subtitle { margin: 0; color: #666; font-size: 13px; }
    .calendar-card { margin-bottom: 24px; }
    .week-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; min-height: 300px; }
    .day-column { border-right: 1px solid #e0e0e0; padding: 4px; }
    .day-column.today { background: rgba(63,81,181,.05); }
    .day-header { text-align: center; padding: 8px 4px; }
    .day-name { display: block; font-size: 11px; color: #888; text-transform: uppercase; }
    .day-num { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; font-weight: 600; font-size: 15px; border-radius: 50%; }
    .today-badge { background: #3f51b5; color: white; }
    .day-appointments { display: flex; flex-direction: column; gap: 4px; }
    .apt-block { padding: 4px 6px; border-radius: 4px; cursor: pointer; font-size: 11px; }
    .apt-block:hover { opacity: .85; }
    .apt-scheduled { background: #fff3e0; border-left: 3px solid #ff9800; }
    .apt-confirmed { background: #e8f5e9; border-left: 3px solid #4caf50; }
    .apt-completed { background: #e3f2fd; border-left: 3px solid #2196f3; }
    .apt-cancelled { background: #fce4ec; border-left: 3px solid #e91e63; }
    .apt-in_progress { background: #ede7f6; border-left: 3px solid #673ab7; }
    .apt-no_show { background: #eceff1; border-left: 3px solid #607d8b; }
    .apt-time { display: block; font-weight: 600; }
    .apt-patient { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .list-card mat-card-content { padding-top: 8px; }
    .apt-row { padding: 12px 8px; border-bottom: 1px solid #f5f5f5; cursor: pointer; gap: 16px; }
    .apt-row:hover { background: #fafafa; }
    .apt-datetime { display: flex; flex-direction: column; align-items: center; min-width: 48px; }
    .date { font-weight: 600; font-size: 14px; }
    .time { font-size: 12px; color: #888; }
    .apt-info { display: flex; flex-direction: column; }
    .patient { font-weight: 500; }
    .reason { font-size: 12px; color: #666; }
    .empty-msg { text-align: center; color: #999; padding: 40px; }
  `]
})
export class AppointmentCalendarComponent implements OnInit {
  private static readonly LAST_DENTIST_STORAGE_KEY = 'dentis_last_selected_dentist_id';

  appointments: Appointment[] = [];
  weekDays: { date: Date; name: string; num: number; isToday: boolean }[] = [];
  currentWeekStart = new Date();
  dentists: ClinicUser[] = [];
  selectedDentistId = '';

  readonly dentistSearchControl = this.fb.control<string | ClinicUser>('');

  readonly filteredDentists$: Observable<ClinicUser[]> = this.dentistSearchControl.valueChanges.pipe(
    startWith(''),
    tap((value) => {
      if (typeof value === 'string') {
        this.selectedDentistId = '';
        this.appointments = [];
      }
    }),
    map((value) => this.filterDentists(this.searchTerm(value), value))
  );

  readonly filteredDentists = toSignal(this.filteredDentists$, { initialValue: [] as ClinicUser[] });

  get currentDateLabel(): string {
    const from = this.weekDays[0]?.date;
    const to = this.weekDays[6]?.date;
    if (!from || !to) return '';
    return `${from.toLocaleDateString('es', { day: '2-digit', month: 'short' })} — ${to.toLocaleDateString('es', { day: '2-digit', month: 'short', year: 'numeric' })}`;
  }

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly clinicService: ClinicService,
    private readonly auth: AuthService,
    private readonly fb: FormBuilder,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.setWeekStart(new Date());
    this.loadDentists();
  }

  private setWeekStart(date: Date): void {
    const d = new Date(date);
    d.setDate(d.getDate() - d.getDay() + 1); // Monday
    d.setHours(0, 0, 0, 0);
    this.currentWeekStart = d;
    const today = new Date();
    this.weekDays = Array.from({ length: 7 }, (_, i) => {
      const day = new Date(d);
      day.setDate(d.getDate() + i);
      return {
        date: day,
        name: day.toLocaleDateString('es', { weekday: 'short' }),
        num: day.getDate(),
        isToday: day.toDateString() === today.toDateString()
      };
    });
  }

  loadWeek(): void {
    if (!this.selectedDentistId) {
      this.appointments = [];
      return;
    }

    const from = new Date(this.currentWeekStart);
    const to = new Date(from);
    to.setDate(to.getDate() + 6);
    to.setHours(23, 59, 59);
    this.appointmentService
      .getByDentist(this.selectedDentistId, this.toLocalDateTimeParam(from), this.toLocalDateTimeParam(to))
      .subscribe({ next: (list) => (this.appointments = list), error: () => (this.appointments = []) });
  }

  displayDentist = (dentist: ClinicUser | string | null): string => {
    if (!dentist || typeof dentist === 'string') {
      return dentist ?? '';
    }

    return `${dentist.fullName} · ${dentist.username}`;
  };

  trackDentist = (dentist: ClinicUser): string => dentist.id;

  onDentistSelected(dentist: ClinicUser): void {
    this.selectDentist(dentist, true);
  }

  searchTerm(value: string | ClinicUser | null): string {
    if (typeof value === 'string') {
      return value.trim();
    }

    return value ? this.displayDentist(value) : '';
  }

  private filterDentists(term: string, value: string | ClinicUser | null): ClinicUser[] {
    if (value && typeof value !== 'string') {
      return [value];
    }

    if (!term) {
      return this.dentists.slice(0, 10);
    }

    const normalized = term.toLowerCase();
    return this.dentists
      .filter((dentist) => {
        const fullName = dentist.fullName.toLowerCase();
        const username = dentist.username.toLowerCase();
        return fullName.includes(normalized) || username.includes(normalized);
      })
      .slice(0, 10);
  }

  private loadDentists(): void {
    this.getDentistSource().subscribe({
      next: (dentists) => {
        this.dentists = dentists;
        if (dentists.length > 0) {
          const savedDentistId = this.readLastDentistId();
          const dentistToSelect = savedDentistId
            ? dentists.find((dentist) => dentist.id === savedDentistId) ?? dentists[0]
            : dentists[0];
          this.selectDentist(dentistToSelect, true);
        } else {
          this.selectedDentistId = '';
          this.appointments = [];
          this.clearLastDentistId();
        }
      },
      error: () => {
        this.dentists = [];
        this.selectedDentistId = '';
        this.appointments = [];
        this.clearLastDentistId();
        this.snack.open('No se pudieron cargar los dentistas', 'OK', { duration: 4000 });
      }
    });
  }

  private selectDentist(dentist: ClinicUser, persist: boolean): void {
    this.dentistSearchControl.setValue(dentist, { emitEvent: false });
    this.selectedDentistId = dentist.id;
    if (persist) {
      this.saveLastDentistId(dentist.id);
    }
    this.loadWeek();
  }

  private readLastDentistId(): string | null {
    return localStorage.getItem(AppointmentCalendarComponent.LAST_DENTIST_STORAGE_KEY);
  }

  private saveLastDentistId(dentistId: string): void {
    localStorage.setItem(AppointmentCalendarComponent.LAST_DENTIST_STORAGE_KEY, dentistId);
  }

  private clearLastDentistId(): void {
    localStorage.removeItem(AppointmentCalendarComponent.LAST_DENTIST_STORAGE_KEY);
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
          map((usersByClinic: ClinicUser[][]) => usersByClinic.flat()),
          map((users) => this.extractActiveDentists(users))
        );
      }),
      catchError(() => of([]))
    );
  }

  private extractActiveDentists(users: ClinicUser[]): ClinicUser[] {
    const unique = new Map<string, ClinicUser>();
    users
      .filter((user) => user.active && user.staffType === 'DENTIST')
      .forEach((user) => unique.set(user.id, user));

    return Array.from(unique.values()).sort((left, right) => left.fullName.localeCompare(right.fullName));
  }

  getAppointmentsForDay(date: Date): Appointment[] {
    return this.appointments.filter((a) => {
      const d = new Date(a.startDateTime);
      return d.toDateString() === date.toDateString();
    });
  }

  prevWeek(): void {
    const d = new Date(this.currentWeekStart);
    d.setDate(d.getDate() - 7);
    this.setWeekStart(d);
    this.loadWeek();
  }

  nextWeek(): void {
    const d = new Date(this.currentWeekStart);
    d.setDate(d.getDate() + 7);
    this.setWeekStart(d);
    this.loadWeek();
  }

  goToday(): void {
    this.setWeekStart(new Date());
    this.loadWeek();
  }

  openDetail(apt: Appointment): void {
    this.snack.open(`${this.displayPatient(apt)} — ${apt.status}`, 'OK', { duration: 2000 });
  }

  displayPatient(appointment: Appointment): string {
    return appointment.patientName?.trim() || `Paciente ${appointment.patientId.slice(0, 8)}`;
  }

  private toLocalDateTimeParam(value: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    const year = value.getFullYear();
    const month = pad(value.getMonth() + 1);
    const day = pad(value.getDate());
    const hour = pad(value.getHours());
    const minute = pad(value.getMinutes());
    const second = pad(value.getSeconds());
    return `${year}-${month}-${day}T${hour}:${minute}:${second}`;
  }
}

