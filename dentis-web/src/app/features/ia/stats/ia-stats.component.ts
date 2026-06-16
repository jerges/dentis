import {
  Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit, signal
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import {
  Chart, BarController, BarElement, CategoryScale, LinearScale,
  Tooltip, Legend
} from 'chart.js';
import { IaService } from '../../../core/services/ia.service';
import { AuthService } from '../../../core/services/auth.service';
import { IaStatsResponse, IaUserStats } from '../../../core/models/ia.model';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

@Component({
  selector: 'app-ia-stats',
  standalone: true,
  imports: [CommonModule, DecimalPipe, MatCardModule, MatIconModule, MatProgressSpinnerModule, MatTableModule],
  template: `
    <div class="stats-container">
      <h2 class="page-title">
        <mat-icon class="title-icon">bar_chart</mat-icon>
        Métricas IA
        <span class="scope-badge">{{ isSuperAdmin ? 'Todas las clínicas' : 'Mi clínica' }}</span>
      </h2>

      @if (loading()) {
        <div class="loading-center"><mat-spinner diameter="48" /></div>
      }

      @if (!loading() && stats()) {
        <div class="kpi-row">
          <mat-card class="kpi-card">
            <mat-icon class="kpi-icon c-blue">forum</mat-icon>
            <div class="kpi-value">{{ stats()!.totalSessions | number }}</div>
            <div class="kpi-label">Sesiones</div>
          </mat-card>
          <mat-card class="kpi-card">
            <mat-icon class="kpi-icon c-purple">chat_bubble</mat-icon>
            <div class="kpi-value">{{ stats()!.totalMessages | number }}</div>
            <div class="kpi-label">Mensajes</div>
          </mat-card>
          <mat-card class="kpi-card">
            <mat-icon class="kpi-icon c-teal">input</mat-icon>
            <div class="kpi-value">{{ stats()!.totalInputTokens | number }}</div>
            <div class="kpi-label">Tokens entrada</div>
          </mat-card>
          <mat-card class="kpi-card">
            <mat-icon class="kpi-icon c-amber">output</mat-icon>
            <div class="kpi-value">{{ stats()!.totalOutputTokens | number }}</div>
            <div class="kpi-label">Tokens salida</div>
          </mat-card>
          <mat-card class="kpi-card">
            <mat-icon class="kpi-icon c-green">attach_money</mat-icon>
            <div class="kpi-value c-green">\${{ stats()!.totalBilledCostUsd | number:'1.4-4' }}</div>
            <div class="kpi-label">Facturación</div>
          </mat-card>
          @if (isSuperAdmin) {
            <mat-card class="kpi-card">
              <mat-icon class="kpi-icon c-red">payments</mat-icon>
              <div class="kpi-value c-red">\${{ stats()!.totalRawCostUsd | number:'1.4-4' }}</div>
              <div class="kpi-label">Coste real (AWS)</div>
            </mat-card>
          }
        </div>

        @if (stats()!.rows.length > 0) {
          <mat-card class="chart-card">
            <mat-card-header>
              <mat-card-title>Uso por usuario — tokens totales</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="chart-wrapper">
                <canvas #chartCanvas></canvas>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="table-card">
            <mat-card-header>
              <mat-card-title>Detalle por usuario</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <table mat-table [dataSource]="stats()!.rows" class="stats-table">
                <ng-container matColumnDef="username">
                  <th mat-header-cell *matHeaderCellDef>Usuario</th>
                  <td mat-cell *matCellDef="let row">{{ row.username }}</td>
                </ng-container>
                <ng-container matColumnDef="clinicName">
                  <th mat-header-cell *matHeaderCellDef>Clínica</th>
                  <td mat-cell *matCellDef="let row">{{ row.clinicName ?? '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="messages">
                  <th mat-header-cell *matHeaderCellDef class="num-col">Mensajes</th>
                  <td mat-cell *matCellDef="let row" class="num-col">{{ row.messages | number }}</td>
                </ng-container>
                <ng-container matColumnDef="inputTokens">
                  <th mat-header-cell *matHeaderCellDef class="num-col">Tokens entrada</th>
                  <td mat-cell *matCellDef="let row" class="num-col">{{ row.inputTokens | number }}</td>
                </ng-container>
                <ng-container matColumnDef="outputTokens">
                  <th mat-header-cell *matHeaderCellDef class="num-col">Tokens salida</th>
                  <td mat-cell *matCellDef="let row" class="num-col">{{ row.outputTokens | number }}</td>
                </ng-container>
                <ng-container matColumnDef="cost">
                  <th mat-header-cell *matHeaderCellDef class="num-col">Facturación</th>
                  <td mat-cell *matCellDef="let row" class="num-col">\${{ row.billedCostUsd | number:'1.4-4' }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
              </table>
            </mat-card-content>
          </mat-card>
        } @else {
          <mat-card class="empty-card">
            <mat-icon>smart_toy</mat-icon>
            <p>Aún no hay datos de uso del asistente IA.</p>
          </mat-card>
        }
      }
    </div>
  `,
  styles: [`
    .stats-container { max-width: 1100px; }
    .page-title {
      display: flex; align-items: center; gap: 10px;
      font-size: 22px; font-weight: 700; margin: 0 0 24px;
    }
    .title-icon { font-size: 26px; width: 26px; height: 26px; color: #0d9488; }
    .scope-badge {
      font-size: 12px; font-weight: 500; border-radius: 20px; padding: 3px 10px;
      background: rgba(13,148,136,.12); color: #0d9488; margin-left: 4px;
    }
    .loading-center { display: flex; justify-content: center; padding: 80px 0; }
    .kpi-row {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 16px; margin-bottom: 24px;
    }
    .kpi-card { padding: 20px; text-align: center; border-radius: 16px !important; }
    .kpi-icon { font-size: 32px; width: 32px; height: 32px; margin-bottom: 8px; display: block; }
    .c-blue   { color: #0ea5e9; }
    .c-purple { color: #8b5cf6; }
    .c-teal   { color: #0d9488; }
    .c-amber  { color: #f59e0b; }
    .c-green  { color: #10b981; }
    .c-red    { color: #ef4444; }
    .kpi-value { font-size: 26px; font-weight: 800; line-height: 1.1; }
    .kpi-label { font-size: 12px; opacity: .6; margin-top: 4px; font-weight: 500; }
    .chart-card, .table-card { border-radius: 16px !important; margin-bottom: 24px; }
    .chart-wrapper { padding: 16px 0; height: 300px; position: relative; }
    canvas { max-height: 300px; }
    .stats-table { width: 100%; }
    .num-col { text-align: right; }
    th.num-col { text-align: right; }
    .empty-card {
      display: flex; flex-direction: column; align-items: center;
      gap: 12px; padding: 48px; border-radius: 16px !important; opacity: .6;
    }
    .empty-card mat-icon { font-size: 48px; width: 48px; height: 48px; }
  `]
})
export class IaStatsComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('chartCanvas') chartCanvas?: ElementRef<HTMLCanvasElement>;

  readonly loading = signal(true);
  readonly stats   = signal<IaStatsResponse | null>(null);

  isSuperAdmin = false;
  displayedColumns: string[] = [];

  private chart?: Chart;
  private pendingStats?: IaStatsResponse;

  constructor(private ia: IaService, private auth: AuthService) {}

  ngOnInit(): void {
    this.isSuperAdmin = this.auth.getRole() === 'SUPER_ADMIN';
    this.displayedColumns = this.isSuperAdmin
      ? ['username', 'clinicName', 'messages', 'inputTokens', 'outputTokens', 'cost']
      : ['username', 'messages', 'inputTokens', 'outputTokens', 'cost'];

    this.ia.getStats().subscribe({
      next: s => {
        this.stats.set(s);
        this.loading.set(false);
        if (this.chartCanvas) {
          this.renderChart(s);
        } else {
          this.pendingStats = s;
        }
      },
      error: () => this.loading.set(false)
    });
  }

  ngAfterViewInit(): void {
    if (this.pendingStats) {
      this.renderChart(this.pendingStats);
      this.pendingStats = undefined;
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private renderChart(s: IaStatsResponse): void {
    if (!this.chartCanvas || s.rows.length === 0) return;
    this.chart?.destroy();

    const top10 = [...s.rows]
      .sort((a, b) => (b.inputTokens + b.outputTokens) - (a.inputTokens + a.outputTokens))
      .slice(0, 10);

    const labels = top10.map(r =>
      this.isSuperAdmin && r.clinicName ? r.username + ' (' + r.clinicName + ')' : r.username
    );

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Tokens entrada',
            data: top10.map(r => r.inputTokens),
            backgroundColor: 'rgba(13,148,136,0.75)',
            borderRadius: 4
          },
          {
            label: 'Tokens salida',
            data: top10.map(r => r.outputTokens),
            backgroundColor: 'rgba(245,158,11,0.75)',
            borderRadius: 4
          }
        ]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom' },
          tooltip: {
            callbacks: {
              label: ctx => ' ' + ctx.dataset.label + ': ' + (ctx.parsed.x ?? 0).toLocaleString()
            }
          }
        },
        scales: {
          x: { grid: { color: 'rgba(0,0,0,0.05)' } }
        }
      }
    });
  }
}
