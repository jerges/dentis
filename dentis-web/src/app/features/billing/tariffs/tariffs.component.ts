import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Tariff } from '../../../core/models/billing.model';

@Component({
  selector: 'app-tariffs',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatChipsModule, MatTooltipModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <div>
          <h1 class="page-title">Aranceles</h1>
          <p class="page-subtitle">Catálogo de procedimientos y precios</p>
        </div>
        <span class="spacer"></span>
        <button mat-raised-button color="primary">
          <mat-icon>add</mat-icon> Nuevo Arancel
        </button>
      </div>

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
              <td mat-cell *matCellDef="let t" class="price">{{ t.price | currency:'USD' }}</td>
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
                <button mat-icon-button matTooltip="Editar"><mat-icon>edit</mat-icon></button>
                <button mat-icon-button matTooltip="Desactivar" color="warn"><mat-icon>toggle_off</mat-icon></button>
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
    .w-full { width: 100%; }
    .tariff-name { font-weight: 500; }
    .tariff-desc { font-size: 12px; color: #888; }
    .price { font-weight: 600; color: #2e7d32; }
    .table-row:hover { background: #f5f5f5; }
  `]
})
export class TariffsComponent {
  cols = ['name', 'category', 'price', 'status', 'actions'];
  dataSource = new MatTableDataSource<Tariff>([]);
}

