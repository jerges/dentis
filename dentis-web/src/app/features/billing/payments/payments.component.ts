import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BillingService } from '../../../core/services/billing.service';
import { Payment } from '../../../core/models/billing.model';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatDialogModule, MatSnackBarModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <div>
          <h1 class="page-title">Pagos</h1>
          <p class="page-subtitle">Registro de pagos recibidos</p>
        </div>
        <span class="spacer"></span>
        <button mat-raised-button color="primary" (click)="showForm = !showForm">
          <mat-icon>{{ showForm ? 'close' : 'add' }}</mat-icon>
          {{ showForm ? 'Cancelar' : 'Registrar Pago' }}
        </button>
      </div>

      <!-- Payment form -->
      @if (showForm) {
        <mat-card class="form-card">
          <mat-card-header><mat-card-title>Nuevo Pago</mat-card-title></mat-card-header>
          <mat-card-content>
            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="form-grid">
              <mat-form-field appearance="outline">
                <mat-label>ID Presupuesto *</mat-label>
                <input matInput formControlName="budgetId" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>ID Paciente *</mat-label>
                <input matInput formControlName="patientId" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Monto *</mat-label>
                <input matInput formControlName="amount" type="number" min="0" />
                <span matPrefix>$ </span>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Método de pago *</mat-label>
                <mat-select formControlName="method">
                  <mat-option value="CASH">Efectivo</mat-option>
                  <mat-option value="BANK_TRANSFER">Transferencia</mat-option>
                  <mat-option value="CREDIT_CARD">Tarjeta Crédito</mat-option>
                  <mat-option value="DEBIT_CARD">Tarjeta Débito</mat-option>
                  <mat-option value="OTHER">Otro</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Referencia</mat-label>
                <input matInput formControlName="reference" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Notas</mat-label>
                <input matInput formControlName="notes" />
              </mat-form-field>
              <div class="form-actions full-span">
                <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
                  <mat-icon>payments</mat-icon> Registrar
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }

      <!-- Payments table -->
      <mat-card>
        <mat-card-content>
          <table mat-table [dataSource]="dataSource" class="w-full">
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef>Fecha</th>
              <td mat-cell *matCellDef="let p">{{ p.paidAt | date:'dd/MM/yyyy HH:mm' }}</td>
            </ng-container>
            <ng-container matColumnDef="amount">
              <th mat-header-cell *matHeaderCellDef>Monto</th>
              <td mat-cell *matCellDef="let p" class="amount">{{ p.amount | currency:'USD' }}</td>
            </ng-container>
            <ng-container matColumnDef="method">
              <th mat-header-cell *matHeaderCellDef>Método</th>
              <td mat-cell *matCellDef="let p">{{ methodLabel(p.method) }}</td>
            </ng-container>
            <ng-container matColumnDef="reference">
              <th mat-header-cell *matHeaderCellDef>Referencia</th>
              <td mat-cell *matCellDef="let p">{{ p.reference ?? '—' }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let r; columns: cols;"></tr>
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell" colspan="4" style="text-align:center;padding:40px;color:#999">
                Sin pagos registrados
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
    .form-card { margin-bottom: 24px; max-width: 860px; }
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
    .full-span { grid-column: 1 / -1; }
    .form-actions { display: flex; justify-content: flex-end; }
    .w-full { width: 100%; }
    .amount { font-weight: 600; color: #2e7d32; }
  `]
})
export class PaymentsComponent {
  cols = ['date', 'amount', 'method', 'reference'];
  dataSource = new MatTableDataSource<Payment>([]);
  showForm = false;
  loading = false;

  form = this.fb.group({
    budgetId: ['', Validators.required],
    patientId: ['', Validators.required],
    amount: [null, [Validators.required, Validators.min(0.01)]],
    method: ['CASH', Validators.required],
    reference: [''],
    notes: ['']
  });

  constructor(private fb: FormBuilder, private billingService: BillingService, private snack: MatSnackBar) {}

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.billingService.registerPayment(this.form.value as any).subscribe({
      next: (p) => {
        this.dataSource.data = [p, ...this.dataSource.data];
        this.snack.open('Pago registrado', 'OK', { duration: 3000 });
        this.form.reset({ method: 'CASH' });
        this.showForm = false;
        this.loading = false;
      },
      error: () => {
        this.snack.open('Error al registrar el pago', 'OK', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  methodLabel(method: string): string {
    const map: Record<string, string> = {
      CASH: 'Efectivo', BANK_TRANSFER: 'Transferencia',
      CREDIT_CARD: 'T. Crédito', DEBIT_CARD: 'T. Débito', OTHER: 'Otro'
    };
    return map[method] ?? method;
  }
}

