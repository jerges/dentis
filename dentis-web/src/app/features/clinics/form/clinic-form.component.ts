import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ClinicService } from '../../../core/services/clinic.service';
import { CreateClinicRequest, UpdateClinicRequest } from '../../../core/models/clinic.model';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-clinic-form',
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
        [title]="isEdit ? 'Editar Clínica' : 'Nueva Clínica'"
        subtitle="Completa los datos de la clínica"
        backRoute="/clinics" />

      @if (errorMessage) {
        <div class="error-alert">{{ errorMessage }}</div>
      }

      <mat-card>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="save()" class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="name" />
              <mat-error>Nombre obligatorio (max 200)</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>NIF</mat-label>
              <input matInput formControlName="nif" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" />
              <mat-error>Email inválido</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Teléfono</mat-label>
              <input matInput formControlName="phone" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full">
              <mat-label>Dirección</mat-label>
              <input matInput formControlName="address" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Ciudad</mat-label>
              <input matInput formControlName="city" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Provincia</mat-label>
              <input matInput formControlName="province" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Código Postal</mat-label>
              <input matInput formControlName="zipCode" />
            </mat-form-field>

            <div class="actions full">
              <button mat-stroked-button routerLink="/clinics" type="button">Cancelar</button>
              <button mat-raised-button color="primary" type="submit">
                <mat-icon>save</mat-icon>
                {{ isEdit ? 'Guardar cambios' : 'Crear clínica' }}
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
export class ClinicFormComponent implements OnInit {
  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    nif: ['', Validators.maxLength(20)],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    phone: ['', Validators.maxLength(20)],
    address: ['', Validators.maxLength(300)],
    city: ['', Validators.maxLength(100)],
    province: ['', Validators.maxLength(100)],
    zipCode: ['', Validators.maxLength(10)]
  });

  isEdit = false;
  clinicId = '';
  errorMessage = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly clinicService: ClinicService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.isEdit = true;
    this.clinicId = id;
    this.clinicService.getClinic(id).subscribe({
      next: (res) => this.form.patchValue(res.data),
      error: () => {
        this.errorMessage = 'No se pudo cargar la clínica.';
      }
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: CreateClinicRequest | UpdateClinicRequest = {
      name: this.form.value.name ?? '',
      nif: this.form.value.nif ?? undefined,
      email: this.form.value.email ?? undefined,
      phone: this.form.value.phone ?? undefined,
      address: this.form.value.address ?? undefined,
      city: this.form.value.city ?? undefined,
      province: this.form.value.province ?? undefined,
      zipCode: this.form.value.zipCode ?? undefined
    };

    const req$ = this.isEdit
      ? this.clinicService.updateClinic(this.clinicId, payload)
      : this.clinicService.createClinic(payload);

    req$.subscribe({
      next: () => this.router.navigate(['/clinics']),
      error: (error) => {
        this.errorMessage = getHttpErrorMessage(error);
      }
    });
  }
}
