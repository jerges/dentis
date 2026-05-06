import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Appointment } from '../../../core/models/appointment.model';
import { AppointmentService } from '../../../core/services/appointment.service';

@Component({
  selector: 'app-appointment-calendar',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatChipsModule, MatDialogModule, MatSnackBarModule
  ],
  template: `
    <div class="page-container">
      <div class="calendar-header flex-row">
        <div>
          <h1 class="page-title">Agenda</h1>
          <p class="page-subtitle">{{ currentDateLabel }}</p>
        </div>
        <span class="spacer"></span>
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
                      <span class="apt-patient">{{ apt.patientName }}</span>
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
                <span class="patient">{{ apt.patientName }}</span>
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
  appointments: Appointment[] = [];
  weekDays: { date: Date; name: string; num: number; isToday: boolean }[] = [];
  currentWeekStart = new Date();

  get currentDateLabel(): string {
    const from = this.weekDays[0]?.date;
    const to = this.weekDays[6]?.date;
    if (!from || !to) return '';
    return `${from.toLocaleDateString('es', { day: '2-digit', month: 'short' })} — ${to.toLocaleDateString('es', { day: '2-digit', month: 'short', year: 'numeric' })}`;
  }

  constructor(private appointmentService: AppointmentService, private snack: MatSnackBar) {}

  ngOnInit(): void {
    this.setWeekStart(new Date());
    this.loadWeek();
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
    const from = new Date(this.currentWeekStart);
    const to = new Date(from);
    to.setDate(to.getDate() + 6);
    to.setHours(23, 59, 59);
    // Load for a default dentist ID — in real app comes from auth/selected dentist
    const dentistId = '00000000-0000-0000-0000-000000000001';
    this.appointmentService
      .getByDentist(dentistId, from.toISOString(), to.toISOString())
      .subscribe({ next: (list) => (this.appointments = list), error: () => (this.appointments = []) });
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
    this.snack.open(`${apt.patientName} — ${apt.status}`, 'OK', { duration: 2000 });
  }
}

