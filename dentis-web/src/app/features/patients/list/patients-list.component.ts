import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged, Subject, switchMap } from 'rxjs';
import { Patient } from '../../../core/models/patient.model';
import { PatientService } from '../../../core/services/patient.service';

@Component({
  selector: 'app-patients-list',
  standalone: true,
  imports: [
    CommonModule, RouterLink, FormsModule,
    MatTableModule, MatPaginatorModule, MatInputModule,
    MatFormFieldModule, MatButtonModule, MatIconModule,
    MatCardModule, MatChipsModule, MatTooltipModule, MatSnackBarModule
  ],
  template: `
    <div class="page-container">
      <!-- Header -->
      <div class="page-header flex-row">
        <div>
          <h1 class="page-title">Pacientes</h1>
          <p class="page-subtitle">{{ totalElements }} pacientes registrados</p>
        </div>
        <span class="spacer"></span>
        <button mat-raised-button color="primary" routerLink="/patients/new">
          <mat-icon>person_add</mat-icon> Nuevo Paciente
        </button>
      </div>

      <mat-card>
        <!-- Search bar -->
        <mat-card-content>
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Buscar paciente por nombre</mat-label>
            <input matInput [(ngModel)]="searchTerm" (ngModelChange)="onSearch($event)" />
            <mat-icon matSuffix>search</mat-icon>
          </mat-form-field>

          <!-- Table -->
          <table mat-table [dataSource]="dataSource" class="w-full">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let p">
                <div class="patient-name">{{ p.lastName }}, {{ p.firstName }}</div>
                <div class="patient-sub">{{ p.idDocument }}</div>
              </td>
            </ng-container>

            <ng-container matColumnDef="contact">
              <th mat-header-cell *matHeaderCellDef>Contacto</th>
              <td mat-cell *matCellDef="let p">
                <div>{{ p.contactInfo?.email }}</div>
                <div class="patient-sub">{{ p.contactInfo?.phoneNumber }}</div>
              </td>
            </ng-container>

            <ng-container matColumnDef="birthDate">
              <th mat-header-cell *matHeaderCellDef>F. Nacimiento</th>
              <td mat-cell *matCellDef="let p">{{ p.birthDate | date:'dd/MM/yyyy' }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let p">
                <span class="status-chip" [class]="p.active ? 'status-active' : 'status-inactive'">
                  {{ p.active ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Acciones</th>
              <td mat-cell *matCellDef="let p">
                <button mat-icon-button color="primary" [routerLink]="['/patients', p.id]" matTooltip="Ver detalle">
                  <mat-icon>visibility</mat-icon>
                </button>
                <button mat-icon-button [routerLink]="['/patients', p.id, 'edit']" matTooltip="Editar">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button color="accent" [routerLink]="['/clinical', p.id]" matTooltip="Historia clínica">
                  <mat-icon>medical_services</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;" class="table-row"></tr>
            <tr class="mat-row no-data-row" *matNoDataRow>
              <td class="mat-cell no-data-cell" colspan="5">
                <mat-icon>search_off</mat-icon>
                <span>No se encontraron pacientes</span>
              </td>
            </tr>
          </table>

          <mat-paginator [length]="totalElements" [pageSize]="20"
                         [pageSizeOptions]="[10,20,50]"
                         (page)="onPageChange($event)" />
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 24px; }
    .page-title { margin: 0 0 4px; font-size: 24px; font-weight: 700; color: #1a237e; }
    .page-subtitle { margin: 0; color: #666; font-size: 13px; }
    .search-field { width: 100%; max-width: 420px; margin-bottom: 16px; }
    .patient-name { font-weight: 500; }
    .patient-sub { font-size: 12px; color: #888; }
    .w-full { width: 100%; }
    .table-row:hover { background: #f5f5f5; cursor: pointer; }
    .no-data-row td { padding: 40px; text-align: center; }
    .no-data-cell { display: flex; align-items: center; justify-content: center; gap: 8px; color: #999; }
  `]
})
export class PatientsListComponent implements OnInit {
  displayedColumns = ['name', 'contact', 'birthDate', 'status', 'actions'];
  dataSource = new MatTableDataSource<Patient>([]);
  totalElements = 0;
  searchTerm = '';
  currentPage = 0;
  private search$ = new Subject<string>();

  constructor(private patientService: PatientService, private snack: MatSnackBar) {}

  ngOnInit(): void {
    this.loadPatients();
    this.search$.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap((name) => name
        ? this.patientService.search(name, 0, 20)
        : this.patientService.getAll(0, 20))
    ).subscribe((page) => {
      this.dataSource.data = page.content;
      this.totalElements = page.totalElements;
    });
  }

  loadPatients(): void {
    this.patientService.getAll(this.currentPage, 20).subscribe((page) => {
      this.dataSource.data = page.content;
      this.totalElements = page.totalElements;
    });
  }

  onSearch(term: string): void { this.search$.next(term); }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.loadPatients();
  }
}

