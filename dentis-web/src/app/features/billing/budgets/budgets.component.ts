import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BillingService } from '../../../core/services/billing.service';
import { Budget } from '../../../core/models/billing.model';

@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatChipsModule, MatSnackBarModule, MatTooltipModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <div>
          <h1 class="page-title">Presupuestos</h1>
          <p class="page-subtitle">Gestión de presupuestos de tratamiento</p>
        </div>
        <span class="spacer"></span>
        <button mat-raised-button color="primary">
          <mat-icon>add</mat-icon> Nuevo Presupuesto
        </button>
      </div>

      <mat-card>
        <mat-card-content>
          <table mat-table [dataSource]="dataSource" class="w-full">
            <ng-container matColumnDef="patient">
              <th mat-header-cell *matHeaderCellDef>Paciente</th>
              <td mat-cell *matCellDef="let b">{{ b.patientName }}</td>
            </ng-container>
            <ng-container matColumnDef="dentist">
              <th mat-header-cell *matHeaderCellDef>Dentista</th>
              <td mat-cell *matCellDef="let b">Dr. {{ b.dentistName }}</td>
            </ng-container>
            <ng-container matColumnDef="total">
              <th mat-header-cell *matHeaderCellDef>Total</th>
              <td mat-cell *matCellDef="let b" class="amount">{{ b.totalAmount | currency:'USD' }}</td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let b">
                <span class="status-chip" [class]="'status-' + b.status.toLowerCase()">{{ b.status }}</span>
              </td>
            </ng-container>
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef>Fecha</th>
              <td mat-cell *matCellDef="let b">{{ b.createdAt | date:'dd/MM/yyyy' }}</td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Acciones</th>
              <td mat-cell *matCellDef="let b">
                <button mat-icon-button matTooltip="Aprobar" color="primary"
                        (click)="approveBudget(b.id)" [disabled]="b.status !== 'PENDING_APPROVAL'">
                  <mat-icon>check_circle</mat-icon>
                </button>
                <button mat-icon-button matTooltip="Ver resumen" (click)="viewSummary(b.id)">
                  <mat-icon>summarize</mat-icon>
                </button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let r; columns: cols;" class="table-row"></tr>
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell" colspan="6" style="text-align:center;padding:40px;color:#999">
                Sin presupuestos registrados
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
    .w-full { width: 100%; }
    .amount { font-weight: 600; color: #2e7d32; }
    .table-row:hover { background: #f5f5f5; cursor: pointer; }
  `]
})
export class BudgetsComponent implements OnInit {
  cols = ['patient', 'dentist', 'total', 'status', 'date', 'actions'];
  dataSource = new MatTableDataSource<Budget>([]);

  constructor(private billingService: BillingService, private snack: MatSnackBar) {}

  ngOnInit(): void { /* Load budgets for a patient — in real app uses filters/search */ }

  approveBudget(id: string): void {
    this.billingService.approveBudget(id).subscribe({
      next: (b) => {
        const idx = this.dataSource.data.findIndex((x) => x.id === id);
        if (idx >= 0) { const data = [...this.dataSource.data]; data[idx] = b; this.dataSource.data = data; }
        this.snack.open('Presupuesto aprobado', 'OK', { duration: 3000 });
      },
      error: () => this.snack.open('Error al aprobar', 'OK', { duration: 3000 })
    });
  }

  viewSummary(id: string): void {
    this.billingService.getBudgetSummary(id).subscribe((s) =>
      this.snack.open(`Total: ${s.totalAmount} | Pagado: ${s.totalPaid} | Pendiente: ${s.totalPending}`, 'OK', { duration: 5000 })
    );
  }
}

