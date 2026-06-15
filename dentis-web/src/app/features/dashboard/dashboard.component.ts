import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { map } from 'rxjs';
import { environment } from '@environments/environment';
import { ApiResponse } from '../../core/models/api.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

interface StatCard {
  label: string;
  value: string | number;
  icon: string;
  color: string;
  route: string;
}

interface DashboardStats {
  totalPatients: number;
  todayAppointments: number;
  weekAppointments: number;
  totalRevenue: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatIconModule, MatButtonModule,
    MatDividerModule, MatProgressBarModule, PageHeaderComponent
  ],
  template: `
    <div class="page-container">
      <app-page-header title="Dashboard" subtitle="Resumen operativo de la clínica">
        <div class="dashboard-chip">Live</div>
      </app-page-header>

      <div class="stats-grid">
        @for (card of statCards; track card.label) {
          <mat-card class="stat-card" [routerLink]="card.route">
            <mat-card-content>
              <div class="stat-body">
                <div class="stat-info">
                  <span class="stat-label">{{ card.label }}</span>
                  <span class="stat-value">{{ card.value }}</span>
                  <span class="stat-meta">Vista rápida</span>
                </div>
                <div class="stat-icon" [style.background]="card.color + '22'">
                  <mat-icon [style.color]="card.color">{{ card.icon }}</mat-icon>
                </div>
              </div>
            </mat-card-content>
          </mat-card>
        }
      </div>

      <mat-card class="actions-card">
        <mat-card-header>
          <mat-card-title>Acciones Rápidas</mat-card-title>
          <mat-card-subtitle>Atajos operativos del día a día</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="actions-grid">
            <button mat-raised-button color="primary" routerLink="/patients/new">
              <mat-icon>person_add</mat-icon> Nuevo Paciente
            </button>
            <button mat-raised-button color="accent" routerLink="/scheduling/new">
              <mat-icon>event</mat-icon> Agendar Cita
            </button>
            <button mat-stroked-button routerLink="/billing/budgets">
              <mat-icon>description</mat-icon> Ver Presupuestos
            </button>
            <button mat-stroked-button routerLink="/billing/payments">
              <mat-icon>payments</mat-icon> Registrar Pago
            </button>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 18px; margin-bottom: 24px; }
    .dashboard-chip {
      padding: 8px 12px;
      border-radius: 999px;
      background: rgba(22, 163, 74, .12);
      color: #15803d;
      font-weight: 700;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: .08em;
    }
    .stat-card { cursor: pointer; transition: transform .2s, box-shadow .2s, border-color .2s; }
    .stat-card:hover { transform: translateY(-4px); box-shadow: 0 20px 40px rgba(13,148,136,.14); border-color: rgba(13,148,136,.28); }
    .stat-body { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; }
    .stat-info { display: flex; flex-direction: column; gap: 4px; }
    .stat-label { font-size: 13px; color: #64748b; font-weight: 600; }
    .stat-value { font-size: 34px; font-weight: 800; color: #0f172a; letter-spacing: -.02em; }
    .stat-meta { font-size: 12px; color: #94a3b8; }
    .stat-icon { width: 60px; height: 60px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
    .stat-icon mat-icon { font-size: 28px; width: 28px; height: 28px; }
    .actions-card { margin-top: 8px; }
    .actions-grid { display: flex; flex-wrap: wrap; gap: 12px; padding-top: 8px; }
    .actions-grid button { display: flex; align-items: center; gap: 8px; border-radius: 14px; }
  `]
})
export class DashboardComponent implements OnInit {
  statCards: StatCard[] = [
    { label: 'Total Pacientes',   value: '—', icon: 'people',          color: '#0d9488', route: '/patients' },
    { label: 'Citas Hoy',         value: '—', icon: 'calendar_today',  color: '#009688', route: '/scheduling' },
    { label: 'Citas esta Semana', value: '—', icon: 'date_range',      color: '#ff9800', route: '/scheduling' },
    { label: 'Ingresos Totales',  value: '—', icon: 'payments',        color: '#4caf50', route: '/billing/payments' },
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<ApiResponse<DashboardStats>>(`${environment.apiUrl}/api/v1/dashboard/stats`)
      .pipe(map((r) => r.data))
      .subscribe({
        next: (stats) => {
          this.statCards[0].value = stats.totalPatients;
          this.statCards[1].value = stats.todayAppointments;
          this.statCards[2].value = stats.weekAppointments;
          this.statCards[3].value = `$${Number(stats.totalRevenue ?? 0).toLocaleString('es-VE', { minimumFractionDigits: 2 })}`;
        },
        error: () => {}
      });
  }
}

