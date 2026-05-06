import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ClinicService } from '../../../core/services/clinic.service';
import { ClinicUser, CreateClinicUserRequest } from '../../../core/models/clinic.model';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-clinic-users',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatDialogModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    PageHeaderComponent,
    LoadingSpinnerComponent,
    StatusBadgeComponent
  ],
  template: `
    <div class="page-container">
      <app-page-header title="Usuarios de la Clínica" subtitle="Alta y gestión de ADMIN/MEDICO para la clínica" backRoute="/clinics" />

      @if (errorMessage) {
        <div class="error-alert">{{ errorMessage }}</div>
      }

      <mat-card class="mb-16">
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="createUser()" class="user-form">
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

            <mat-form-field appearance="outline">
              <mat-label>Rol</mat-label>
              <mat-select formControlName="role">
                <mat-option value="ADMIN">ADMIN</mat-option>
                <mat-option value="MEDICO">MEDICO</mat-option>
              </mat-select>
            </mat-form-field>

            <button mat-raised-button color="primary" type="submit">
              <mat-icon>person_add</mat-icon>
              Crear usuario
            </button>
          </form>
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-content>
          @if (loading) {
            <app-loading-spinner message="Cargando usuarios..." />
          } @else {
          <table mat-table [dataSource]="dataSource" class="w-full">
            <ng-container matColumnDef="username">
              <th mat-header-cell *matHeaderCellDef>Usuario</th>
              <td mat-cell *matCellDef="let u">{{ u.username }}</td>
            </ng-container>

            <ng-container matColumnDef="fullName">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let u">{{ u.fullName }}</td>
            </ng-container>

            <ng-container matColumnDef="email">
              <th mat-header-cell *matHeaderCellDef>Email</th>
              <td mat-cell *matCellDef="let u">{{ u.email }}</td>
            </ng-container>

            <ng-container matColumnDef="role">
              <th mat-header-cell *matHeaderCellDef>Rol</th>
              <td mat-cell *matCellDef="let u">{{ u.role }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let u">
                <app-status-badge [label]="u.active ? 'Activo' : 'Inactivo'" [tone]="u.active ? 'active' : 'inactive'" />
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Acciones</th>
              <td mat-cell *matCellDef="let u">
                <button mat-icon-button color="warn" [disabled]="!u.active" (click)="deactivateUser(u.id)">
                  <mat-icon>person_off</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let _row; columns: cols;"></tr>
          </table>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .mb-16 { margin-bottom: 16px; }
    .w-full { width: 100%; }
    .user-form { display:grid; grid-template-columns: repeat(3, minmax(220px, 1fr)); gap: 12px; align-items:center; }
  `]
})
export class ClinicUsersComponent implements OnInit {
  cols = ['username', 'fullName', 'email', 'role', 'status', 'actions'];
  dataSource = new MatTableDataSource<ClinicUser>([]);
  clinicId = '';
  errorMessage = '';
  loading = false;

  readonly form = this.fb.group({
    username: ['', Validators.required],
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['MEDICO', Validators.required]
  });

  constructor(
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly clinicService: ClinicService,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('clinicId');
    if (!id) {
      this.errorMessage = 'Falta identificador de clínica.';
      return;
    }
    this.clinicId = id;
    this.loadUsers();
  }

  loadUsers(): void {
    this.errorMessage = '';
    this.loading = true;
    this.clinicService.getClinicUsers(this.clinicId).subscribe({
      next: (res) => {
        this.dataSource.data = res.data ?? [];
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar los usuarios de la clínica.';
        this.loading = false;
      }
    });
  }

  createUser(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload: CreateClinicUserRequest = {
      username: this.form.value.username ?? '',
      fullName: this.form.value.fullName ?? '',
      email: this.form.value.email ?? '',
      password: this.form.value.password ?? '',
      role: (this.form.value.role as 'ADMIN' | 'MEDICO') ?? 'MEDICO'
    };

    this.clinicService.createClinicUser(this.clinicId, payload).subscribe({
      next: () => {
        this.form.patchValue({ password: '', role: 'MEDICO' });
        this.loadUsers();
      },
      error: () => {
        this.errorMessage = 'No se pudo crear el usuario.';
      }
    });
  }

  deactivateUser(userId: string): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Desactivar usuario',
        message: 'El usuario dejará de poder acceder al sistema para esta clínica. ¿Deseas continuar?',
        confirmLabel: 'Desactivar',
        cancelLabel: 'Cancelar',
        danger: true
      }
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }

      this.clinicService.deactivateClinicUser(this.clinicId, userId).subscribe({
        next: () => this.loadUsers(),
        error: () => {
          this.errorMessage = 'No se pudo desactivar el usuario.';
        }
      });
    });
  }
}
