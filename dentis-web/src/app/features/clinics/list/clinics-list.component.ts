import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ClinicService } from '../../../core/services/clinic.service';
import { Clinic } from '../../../core/models/clinic.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-clinics-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatTooltipModule,
    PageHeaderComponent,
    LoadingSpinnerComponent,
    StatusBadgeComponent
  ],
  template: `
    <div class="page-container">
      <app-page-header title="Administración de Clínicas" subtitle="Gestiona clínicas y usuarios por clínica">
        <button mat-raised-button color="primary" routerLink="/clinics/new">
          <mat-icon>add_business</mat-icon> Nueva Clínica
        </button>
      </app-page-header>

      @if (errorMessage) {
        <div class="error-alert">{{ errorMessage }}</div>
      }

      <mat-card>
        <mat-card-content>
          @if (loading) {
            <app-loading-spinner message="Cargando clínicas..." />
          } @else {
          <table mat-table [dataSource]="dataSource" class="w-full">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let c">{{ c.name }}</td>
            </ng-container>

            <ng-container matColumnDef="nif">
              <th mat-header-cell *matHeaderCellDef>NIF</th>
              <td mat-cell *matCellDef="let c">{{ c.nif || '-' }}</td>
            </ng-container>

            <ng-container matColumnDef="city">
              <th mat-header-cell *matHeaderCellDef>Ciudad</th>
              <td mat-cell *matCellDef="let c">{{ c.city || '-' }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let c">
                <app-status-badge [label]="c.active ? 'Activa' : 'Inactiva'" [tone]="c.active ? 'active' : 'inactive'" />
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Acciones</th>
              <td mat-cell *matCellDef="let c">
                <button mat-icon-button [routerLink]="['/clinics', c.id, 'edit']" matTooltip="Editar clínica">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button [routerLink]="['/clinics', c.id, 'users']" matTooltip="Usuarios de la clínica">
                  <mat-icon>group</mat-icon>
                </button>
                <button mat-icon-button color="warn" (click)="deactivate(c.id)" matTooltip="Desactivar clínica">
                  <mat-icon>block</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let _row; columns: cols;"></tr>
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell" colspan="5" style="text-align:center;padding:40px;color:#999">
                No hay clínicas registradas
              </td>
            </tr>
          </table>

          <div class="pager">
            <button mat-stroked-button (click)="prevPage()" [disabled]="page === 0">Anterior</button>
            <span>Página {{ page + 1 }} de {{ totalPages }}</span>
            <button mat-stroked-button (click)="nextPage()" [disabled]="page + 1 >= totalPages">Siguiente</button>
          </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .w-full { width: 100%; }
    .pager { margin-top: 16px; display:flex; align-items:center; gap:12px; justify-content:flex-end; }
  `]
})
export class ClinicsListComponent implements OnInit {
  cols = ['name', 'nif', 'city', 'status', 'actions'];
  dataSource = new MatTableDataSource<Clinic>([]);
  page = 0;
  size = 10;
  totalPages = 1;
  errorMessage = '';
  loading = false;

  constructor(
    private readonly clinicService: ClinicService,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.errorMessage = '';
    this.loading = true;
    this.clinicService.getClinics(this.page, this.size).subscribe({
      next: (res) => {
        this.dataSource.data = res.data?.content ?? [];
        this.totalPages = res.data?.totalPages || 1;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar las clínicas.';
        this.loading = false;
      }
    });
  }

  deactivate(id: string): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Desactivar clínica',
        message: 'Esta acción desactivará la clínica seleccionada. ¿Deseas continuar?',
        confirmLabel: 'Desactivar',
        cancelLabel: 'Cancelar',
        danger: true
      }
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }

      this.clinicService.deactivateClinic(id).subscribe({
        next: () => this.load(),
        error: () => {
          this.errorMessage = 'No se pudo desactivar la clínica.';
        }
      });
    });
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) {
      this.page++;
      this.load();
    }
  }
}

