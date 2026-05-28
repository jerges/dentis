import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ClinicService } from '../../../core/services/clinic.service';
import { CreateGlobalClinicUserRequest } from '../../../core/models/clinic.model';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-super-admin-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent
  ],
  template: `
    <div class="page-container">
      <app-page-header
        title="Nuevo Super Admin"
        subtitle="Crea un usuario global con acceso a todas las clínicas"
        backRoute="/clinics" />

      @if (errorMessage) {
        <div class="error-alert">{{ errorMessage }}</div>
      }

      <mat-card>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="save()" class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>Usuario</mat-label>
              <input matInput formControlName="username" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Nombre completo</mat-label>
              <input matInput formControlName="fullName" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Contraseña</mat-label>
              <input matInput type="password" formControlName="password" />
            </mat-form-field>

            <div class="actions full">
              <button mat-stroked-button routerLink="/clinics" type="button">Cancelar</button>
              <button mat-raised-button color="primary" type="submit">
                <mat-icon>admin_panel_settings</mat-icon>
                Crear Super Admin
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .form-grid { display:grid; grid-template-columns: repeat(2, minmax(220px, 1fr)); gap: 12px; }
    .full { grid-column: 1 / -1; }
    .actions { display:flex; justify-content:flex-end; gap: 10px; margin-top: 8px; }
  `]
})
export class SuperAdminFormComponent {
  readonly form = this.fb.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]]
  });

  errorMessage = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly clinicService: ClinicService,
    private readonly router: Router
  ) {}

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: CreateGlobalClinicUserRequest = {
      username: this.form.value.username ?? '',
      fullName: this.form.value.fullName ?? '',
      email: this.form.value.email ?? '',
      password: this.form.value.password ?? '',
      role: 'SUPER_ADMIN',
      staffType: 'ADMINISTRATIVE'
    };

    this.clinicService.createGlobalClinicUser(payload).subscribe({
      next: () => this.router.navigate(['/clinics']),
      error: (error) => {
        this.errorMessage = getHttpErrorMessage(error);
      }
    });
  }
}

