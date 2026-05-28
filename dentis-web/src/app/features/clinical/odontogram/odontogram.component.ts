import { Component, OnInit, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { PatientService } from '../../../core/services/patient.service';
import { Patient } from '../../../core/models/patient.model';

interface Tooth {
  number: number;
  condition: 'HEALTHY' | 'CAVITY' | 'EXTRACTED' | 'CROWNED' | 'FILLED' | 'ROOT_CANAL';
  surface?: string;
}

const CONDITION_STYLE: Record<string, { bg: string; label: string }> = {
  HEALTHY:     { bg: '#e8f5e9', label: 'Sano' },
  CAVITY:      { bg: '#fff3cd', label: 'Caries' },
  EXTRACTED:   { bg: '#f5f5f5', label: 'Extraído' },
  CROWNED:     { bg: '#e3f2fd', label: 'Corona' },
  FILLED:      { bg: '#f3e5f5', label: 'Obturado' },
  ROOT_CANAL:  { bg: '#fce4ec', label: 'Endo.' }
};

@Component({
  selector: 'app-odontogram',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTooltipModule, MatChipsModule, MatDividerModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <button mat-icon-button [routerLink]="['/clinical', patientId()]">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <div>
          <h1 class="page-title">Odontograma</h1>
          @if (patient) {
            <p class="page-subtitle">{{ patient.lastName }}, {{ patient.firstName }}</p>
          }
        </div>
        <span class="spacer"></span>
        <button mat-raised-button color="primary">
          <mat-icon>save</mat-icon> Guardar Cambios
        </button>
      </div>

      <!-- Legend -->
      <mat-card class="legend-card">
        <mat-card-content>
          <div class="legend flex-row gap-16">
            @for (entry of legendEntries; track entry.key) {
              <div class="legend-item flex-row gap-8">
                <div class="legend-dot" [style.background]="entry.bg"></div>
                <span>{{ entry.label }}</span>
              </div>
            }
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Odontogram -->
      <mat-card class="odontogram-card">
        <mat-card-header><mat-card-title>Dentición Permanente</mat-card-title></mat-card-header>
        <mat-card-content>
          <!-- Upper jaw -->
          <div class="jaw-label">Maxilar Superior</div>
          <div class="teeth-row upper">
            @for (tooth of upperTeeth; track tooth.number) {
              <div class="tooth"
                   [style.background]="getStyle(tooth.condition).bg"
                   [matTooltip]="tooth.number + ' — ' + getStyle(tooth.condition).label"
                   (click)="selectTooth(tooth)">
                <span class="tooth-num">{{ tooth.number }}</span>
              </div>
            }
          </div>
          <mat-divider class="jaw-divider" />
          <!-- Lower jaw -->
          <div class="teeth-row lower">
            @for (tooth of lowerTeeth; track tooth.number) {
              <div class="tooth"
                   [style.background]="getStyle(tooth.condition).bg"
                   [matTooltip]="tooth.number + ' — ' + getStyle(tooth.condition).label"
                   (click)="selectTooth(tooth)">
                <span class="tooth-num">{{ tooth.number }}</span>
              </div>
            }
          </div>
          <div class="jaw-label">Mandíbula Inferior</div>
        </mat-card-content>
      </mat-card>

      <!-- Selected tooth detail -->
      @if (selectedTooth) {
        <mat-card class="tooth-detail-card">
          <mat-card-header><mat-card-title>Diente {{ selectedTooth.number }}</mat-card-title></mat-card-header>
          <mat-card-content>
            <div class="condition-chips flex-row gap-8 flex-wrap">
              @for (cond of conditions; track cond) {
                <button mat-stroked-button
                        [class.active-condition]="selectedTooth.condition === cond"
                        (click)="setCondition(cond)">
                  <div class="btn-dot" [style.background]="getStyle(cond).bg"></div>
                  {{ getStyle(cond).label }}
                </button>
              }
            </div>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .page-header { gap: 12px; margin-bottom: 24px; flex-wrap: wrap; }
    .page-title { margin: 0 0 4px; font-size: 22px; font-weight: 700; }
    .page-subtitle { margin: 0; color: #666; font-size: 13px; }
    .legend-card { margin-bottom: 16px; }
    .legend { flex-wrap: wrap; }
    .legend-dot { width: 16px; height: 16px; border-radius: 4px; border: 1px solid #ddd; }
    .odontogram-card { margin-bottom: 16px; }
    .jaw-label { text-align: center; font-size: 12px; color: #888; padding: 8px; }
    .teeth-row { display: flex; justify-content: center; gap: 4px; flex-wrap: wrap; padding: 8px 0; }
    .tooth {
      width: 44px; height: 56px; border-radius: 8px; border: 2px solid #bdbdbd;
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      cursor: pointer; transition: transform .15s, box-shadow .15s;
    }
    .tooth:hover { transform: scale(1.1); box-shadow: 0 4px 12px rgba(0,0,0,.15); }
    .tooth-num { font-size: 12px; font-weight: 600; }
    .jaw-divider { margin: 4px 0; }
    .tooth-detail-card { max-width: 600px; }
    .condition-chips { flex-wrap: wrap; padding-top: 8px; }
    .active-condition { border-color: #3f51b5 !important; color: #3f51b5 !important; }
    .btn-dot { width: 12px; height: 12px; border-radius: 3px; border: 1px solid #ddd; display: inline-block; margin-right: 4px; }
    .flex-wrap { flex-wrap: wrap; }
  `]
})
export class OdontogramComponent implements OnInit {
  patientId = input<string>('');
  patient: Patient | null = null;
  selectedTooth: Tooth | null = null;

  conditions: Tooth['condition'][] = ['HEALTHY', 'CAVITY', 'EXTRACTED', 'CROWNED', 'FILLED', 'ROOT_CANAL'];

  legendEntries = Object.entries(CONDITION_STYLE).map(([key, v]) => ({ key, ...v }));

  upperTeeth: Tooth[] = [18,17,16,15,14,13,12,11,21,22,23,24,25,26,27,28].map((n) => ({ number: n, condition: 'HEALTHY' }));
  lowerTeeth: Tooth[] = [48,47,46,45,44,43,42,41,31,32,33,34,35,36,37,38].map((n) => ({ number: n, condition: 'HEALTHY' }));

  constructor(private patientService: PatientService) {}

  ngOnInit(): void {
    const id = this.patientId();
    if (id) this.patientService.getById(id).subscribe((p) => (this.patient = p));
  }

  getStyle(condition: Tooth['condition']) { return CONDITION_STYLE[condition]; }

  selectTooth(tooth: Tooth): void { this.selectedTooth = tooth; }

  setCondition(condition: Tooth['condition']): void {
    if (this.selectedTooth) this.selectedTooth.condition = condition;
  }
}

