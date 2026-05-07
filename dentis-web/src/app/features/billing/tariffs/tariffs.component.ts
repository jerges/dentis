import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BillingService } from '../../../core/services/billing.service';
import { CreateTariffRequest, Tariff, TariffCategory } from '../../../core/models/billing.model';

@Component({
  selector: 'app-tariffs',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatChipsModule, MatTooltipModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatSnackBarModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <div>
          <h1 class="page-title">Aranceles</h1>
          <p class="page-subtitle">Catálogo de procedimientos y precios</p>
        </div>
        <span class="spacer"></span>
        <button mat-raised-button color="primary" (click)="showForm = !showForm">
          <mat-icon>add</mat-icon> Nuevo Arancel
        </button>
      </div>

      @if (showForm) {
        <mat-card class="form-card">
          <mat-card-content>
            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="form-grid">
              <mat-form-field appearance="outline">
                <mat-label>Código *</mat-label>
                <input matInput formControlName="code" placeholder="AR-001" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Nombre *</mat-label>
                <input matInput formControlName="name" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-span">
                <mat-label>Descripción</mat-label>
                <input matInput formControlName="description" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Categoría *</mat-label>
                <mat-select formControlName="category">
                  @for (category of tariffCategories; track category) {
                    <mat-option [value]="category">{{ category }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Precio base *</mat-label>
                <input matInput type="number" min="0.01" step="0.01" formControlName="basePrice" />
              </mat-form-field>
              <div class="actions full-span">
                <button mat-stroked-button type="button" (click)="showForm = false">Cancelar</button>
                <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
                  <mat-icon>save</mat-icon>
                  Guardar arancel
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }

      <mat-card>
        <mat-card-content>
          <table mat-table [dataSource]="dataSource" class="w-full">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Procedimiento</th>
              <td mat-cell *matCellDef="let t">
                <div class="tariff-name">{{ t.name }}</div>
                <div class="tariff-desc">{{ t.description }}</div>
              </td>
            </ng-container>
            <ng-container matColumnDef="category">
              <th mat-header-cell *matHeaderCellDef>Categoría</th>
              <td mat-cell *matCellDef="let t">{{ t.category }}</td>
            </ng-container>
            <ng-container matColumnDef="price">
              <th mat-header-cell *matHeaderCellDef>Precio</th>
              <td mat-cell *matCellDef="let t" class="price">{{ t.basePrice | currency:'USD' }}</td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let t">
                <span class="status-chip" [class]="t.active ? 'status-active' : 'status-inactive'">
                  {{ t.active ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Acciones</th>
              <td mat-cell *matCellDef="let t">
                <button mat-icon-button matTooltip="Desactivar" color="warn" (click)="deactivate(t.id)">
                  <mat-icon>toggle_off</mat-icon>
                </button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let r; columns: cols;" class="table-row"></tr>
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell" colspan="5" style="text-align:center;padding:40px;color:#999">
                Sin aranceles registrados
              </td>
            </tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 24px; }
    .page-title { margin: 0 0 4px; font-size: 24px; font-weight: 700; color: #1a237e; }
    .page-subtitle { margin: 0; color: #666; font-size: 13px; }
    .form-card { margin-bottom: 16px; }
    .form-grid { display:grid; grid-template-columns: repeat(2, minmax(220px, 1fr)); gap: 12px; }
    .full-span { grid-column: 1 / -1; }
    .actions { display:flex; justify-content:flex-end; gap: 10px; }
    .w-full { width: 100%; }
    .tariff-name { font-weight: 500; }
    .tariff-desc { font-size: 12px; color: #888; }
    .price { font-weight: 600; color: #2e7d32; }
    .table-row:hover { background: #f5f5f5; }
  `]
})
export class TariffsComponent implements OnInit {
  readonly tariffCategories: TariffCategory[] = [
    'LABORATORY', 'SUPPLIES', 'ORTHODONTICS', 'SURGERY', 'GENERAL_DENTISTRY',
    'SPECIALTY', 'IMAGING', 'ENDODONTICS', 'PERIODONTICS', 'PEDIATRIC_DENTISTRY',
    'ADMINISTRATIVE', 'OTHER'
  ];

  cols = ['name', 'category', 'price', 'status', 'actions'];
  dataSource = new MatTableDataSource<Tariff>([]);
  showForm = false;
  loading = false;

  readonly form = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(30)]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    category: ['GENERAL_DENTISTRY' as TariffCategory, Validators.required],
    basePrice: [null as number | null, [Validators.required, Validators.min(0.01)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly billingService: BillingService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadTariffs();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const raw = this.form.getRawValue();
    const payload: CreateTariffRequest = {
      code: (raw.code ?? '').trim(),
      name: (raw.name ?? '').trim(),
      description: raw.description?.trim() || undefined,
      category: raw.category as TariffCategory,
      basePrice: Number(raw.basePrice ?? 0),
      discountAllowed: true
    };

    this.billingService.createTariff(payload).subscribe({
      next: (created) => {
        this.dataSource.data = [created, ...this.dataSource.data];
        this.snack.open('Arancel creado', 'OK', { duration: 3000 });
        this.form.reset({ category: 'GENERAL_DENTISTRY' });
        this.showForm = false;
        this.loading = false;
      },
      error: () => {
        this.snack.open('No se pudo crear el arancel', 'OK', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  deactivate(id: string): void {
    this.billingService.deactivateTariff(id).subscribe({
      next: () => {
        const updated = this.dataSource.data.map((tariff) => tariff.id === id ? { ...tariff, active: false } : tariff);
        this.dataSource.data = updated;
        this.snack.open('Arancel desactivado', 'OK', { duration: 3000 });
      },
      error: () => this.snack.open('No se pudo desactivar el arancel', 'OK', { duration: 3000 })
    });
  }

  private loadTariffs(): void {
    this.billingService.getTariffs(0, 200).subscribe({
      next: (page) => {
        this.dataSource.data = page.content ?? [];
      },
      error: () => {
        this.dataSource.data = [];
        this.snack.open('No se pudieron cargar los aranceles', 'OK', { duration: 3000 });
      }
    });
  }
}

