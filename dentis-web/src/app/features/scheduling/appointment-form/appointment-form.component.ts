import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AppointmentService } from '../../../core/services/appointment.service';

@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatDatepickerModule,
    MatNativeDateModule, MatSnackBarModule
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
            <mat-form-field appearance="outline">
              <mat-label>ID Paciente *</mat-label>
              <input matInput formControlName="patientId" placeholder="UUID del paciente" />
              <mat-icon matSuffix>person</mat-icon>
              <mat-error>Obligatorio</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>ID Dentista *</mat-label>
              <input matInput formControlName="dentistId" placeholder="UUID del dentista" />
              <mat-icon matSuffix>medical_services</mat-icon>
              <mat-error>Obligatorio</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Fecha y hora inicio *</mat-label>
              <input matInput formControlName="startDateTime" type="datetime-local" />
              <mat-error>Obligatorio</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Fecha y hora fin *</mat-label>
              <input matInput formControlName="endDateTime" type="datetime-local" />
              <mat-error>Obligatorio</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-span">
              <mat-label>Motivo de consulta *</mat-label>
              <textarea matInput formControlName="consultationReason" rows="3"></textarea>
              <mat-error>Obligatorio</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-span">
              <mat-label>Notas adicionales</mat-label>
              <textarea matInput formControlName="notes" rows="2"></textarea>
            </mat-form-field>

            <div class="form-actions full-span">
              <button mat-stroked-button type="button" routerLink="/scheduling">Cancelar</button>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
                <mat-icon>event_available</mat-icon> Agendar Cita
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
  `]
})
export class AppointmentFormComponent {
  loading = false;

  form = this.fb.group({
    patientId: ['', Validators.required],
    dentistId: ['', Validators.required],
    startDateTime: ['', Validators.required],
    endDateTime: ['', Validators.required],
    consultationReason: ['', Validators.required],
    notes: ['']
  });

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private router: Router,
    private snack: MatSnackBar
  ) {}

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    const val = this.form.value as any;
    // Convert datetime-local to ISO
    val.startDateTime = new Date(val.startDateTime).toISOString();
    val.endDateTime = new Date(val.endDateTime).toISOString();

    this.appointmentService.schedule(val).subscribe({
      next: () => {
        this.snack.open('Cita agendada correctamente', 'OK', { duration: 3000 });
        this.router.navigate(['/scheduling']);
      },
      error: (e) => {
        this.snack.open(e?.error?.error?.message ?? 'Error al agendar', 'OK', { duration: 4000 });
        this.loading = false;
      }
    });
  }
}

