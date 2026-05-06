import { Component, OnInit, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { PatientService } from '../../../core/services/patient.service';
import { Patient } from '../../../core/models/patient.model';

@Component({
  selector: 'app-clinical-record',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTabsModule, MatDividerModule, MatChipsModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <button mat-icon-button routerLink="/patients"><mat-icon>arrow_back</mat-icon></button>
        <div>
          <h1 class="page-title">Historia Clínica</h1>
          @if (patient) {
            <p class="page-subtitle">{{ patient.lastName }}, {{ patient.firstName }} · {{ patient.idDocument }}</p>
          }
        </div>
        <span class="spacer"></span>
        <button mat-stroked-button [routerLink]="['/clinical', patientId(), 'odontogram']">
          <mat-icon>grid_on</mat-icon> Odontograma
        </button>
        <button mat-raised-button color="primary">
          <mat-icon>add</mat-icon> Nueva Evolución
        </button>
      </div>

      <mat-tab-group animationDuration="200ms">
        <mat-tab label="Evoluciones Clínicas">
          <div class="tab-content">
            @for (ev of [1,2,3]; track ev) {
              <mat-card class="evolution-card">
                <mat-card-content>
                  <div class="flex-row">
                    <div>
                      <div class="evo-date">{{ today | date:'dd/MM/yyyy' }}</div>
                      <div class="evo-desc">Control de rutina — Limpieza y revisión general</div>
                      <div class="evo-dentist">Dr. García</div>
                    </div>
                    <span class="spacer"></span>
                    <button mat-icon-button><mat-icon>edit</mat-icon></button>
                  </div>
                </mat-card-content>
              </mat-card>
            }
            <p class="empty-hint">Cargando evoluciones del paciente...</p>
          </div>
        </mat-tab>

        <mat-tab label="Diagnósticos">
          <div class="tab-content">
            <div class="diagnoses-grid">
              @for (d of mockDiagnoses; track d.code) {
                <mat-card class="diagnosis-card">
                  <mat-card-content>
                    <div class="diagnosis-code">{{ d.code }}</div>
                    <div class="diagnosis-name">{{ d.name }}</div>
                    <div class="diagnosis-date">{{ d.date | date:'dd/MM/yyyy' }}</div>
                  </mat-card-content>
                </mat-card>
              }
            </div>
          </div>
        </mat-tab>

        <mat-tab label="Planes de Tratamiento">
          <div class="tab-content">
            <p class="empty-hint">No hay planes de tratamiento activos.</p>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .page-header { gap: 12px; margin-bottom: 24px; flex-wrap: wrap; }
    .page-title { margin: 0 0 4px; font-size: 22px; font-weight: 700; }
    .page-subtitle { margin: 0; color: #666; font-size: 13px; }
    .tab-content { padding: 24px 0; }
    .evolution-card { margin-bottom: 12px; }
    .evo-date { font-weight: 600; color: #3f51b5; }
    .evo-desc { margin: 4px 0; }
    .evo-dentist { font-size: 12px; color: #888; }
    .diagnoses-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
    .diagnosis-card { text-align: center; }
    .diagnosis-code { font-size: 20px; font-weight: 700; color: #3f51b5; }
    .diagnosis-name { font-size: 13px; margin: 4px 0; }
    .diagnosis-date { font-size: 11px; color: #888; }
    .empty-hint { color: #999; text-align: center; padding: 40px; }
  `]
})
export class ClinicalRecordComponent implements OnInit {
  patientId = input<string>('');
  patient: Patient | null = null;
  today = new Date();

  mockDiagnoses = [
    { code: 'K02.1', name: 'Caries de dentina', date: new Date() },
    { code: 'K05.1', name: 'Gingivitis crónica', date: new Date() }
  ];

  constructor(private patientService: PatientService) {}

  ngOnInit(): void {
    const id = this.patientId();
    if (id) this.patientService.getById(id).subscribe((p) => (this.patient = p));
  }
}

