import { Component, OnInit, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { PatientService } from '../../../core/services/patient.service';
import { ClinicalService } from '../../../core/services/clinical.service';
import { Patient } from '../../../core/models/patient.model';
import {
  OdontogramTooth, ToothCondition, ToothSurface, SpaceStatus,
  DentitionType, ClinicalRecord, RootFinding, Diagnosis
} from '../../../core/models/clinical.model';

/* ── Types ───────────────────────────────────────────────────────────────── */
interface ToothStyle { fill: string; stroke: string; label: string; }
interface RootFindingStyle { symbol: string; color: string; label: string; }

const SURFACES: ToothSurface[] = ['BUCCAL', 'LINGUAL', 'MESIAL', 'DISTAL', 'OCCLUSAL'];
const SURFACE_LABEL: Record<ToothSurface, string> = {
  BUCCAL: 'Vestibular', LINGUAL: 'Lingual',
  MESIAL: 'Mesial', DISTAL: 'Distal', OCCLUSAL: 'Oclusal', INCISAL: 'Incisal',
};

const CONDITION_STYLE: Record<string, ToothStyle> = {
  HEALTHY:           { fill: '#f1f8f8', stroke: '#b2d8d8', label: 'Sano' },
  CARIES:            { fill: '#fef3c7', stroke: '#f59e0b', label: 'Caries' },
  ABSENT:            { fill: '#f1f5f9', stroke: '#94a3b8', label: 'Ausente' },
  EXTRACTED:         { fill: '#e2e8f0', stroke: '#64748b', label: 'Extraído' },
  CROWNED:           { fill: '#bfdbfe', stroke: '#0ea5e9', label: 'Corona' },
  RESTORED:          { fill: '#e9d5ff', stroke: '#a855f7', label: 'Restaurado' },
  IMPLANT:           { fill: '#ccfbf1', stroke: '#0d9488', label: 'Implante' },
  ROOT_CANAL:        { fill: '#fee2e2', stroke: '#dc2626', label: 'Endodoncia' },
  FRACTURE:          { fill: '#fef9c3', stroke: '#ca8a04', label: 'Fractura' },
  MALFORMATION:      { fill: '#ffedd5', stroke: '#ea580c', label: 'Malformación' },
  ROOT_REMNANT:      { fill: '#e7e5e4', stroke: '#78716c', label: 'Resto Radicular' },
  DEFECTIVE_FILLING: { fill: '#f5f5f5', stroke: '#a3a3a3', label: 'Obturación Defect.' },
  ERUPTING:          { fill: '#cffafe', stroke: '#06b6d4', label: 'En Erupción' },
  IMPACTED:          { fill: '#dcfce7', stroke: '#16a34a', label: 'Impactado' },
};

const ROOT_FINDING_STYLE: Record<RootFinding, RootFindingStyle> = {
  ENDODONTIC_TREATMENT: { symbol: '┃', color: '#dc2626', label: 'Endodoncia' },
  PERIAPICAL_LESION:    { symbol: '▲', color: '#f59e0b', label: 'Lesión periapical' },
  PERIAPICAL_ABSCESS:   { symbol: '◉', color: '#ef4444', label: 'Absceso periapical' },
  ROOT_FRACTURE:        { symbol: '╳', color: '#78716c', label: 'Fractura radicular' },
  INTERNAL_RESORPTION:  { symbol: '○', color: '#a855f7', label: 'Reabsorción interna' },
  EXTERNAL_RESORPTION:  { symbol: '◁', color: '#8b5cf6', label: 'Reabsorción externa' },
  POST_CORE:            { symbol: '▮', color: '#0d9488', label: 'Perno/Poste' },
  APICOECTOMY:          { symbol: '⊥', color: '#64748b', label: 'Apicectomía' },
  HYPERCEMENTOSIS:      { symbol: '●', color: '#ca8a04', label: 'Hipercementosis' },
};

const ALL_ROOT_FINDINGS: RootFinding[] = [
  'ENDODONTIC_TREATMENT', 'PERIAPICAL_LESION', 'PERIAPICAL_ABSCESS',
  'ROOT_FRACTURE', 'INTERNAL_RESORPTION', 'EXTERNAL_RESORPTION',
  'POST_CORE', 'APICOECTOMY', 'HYPERCEMENTOSIS',
];

const SPACE_LABEL: Record<SpaceStatus, string> = {
  OPEN: '△ Espacio abierto',
  PARTIALLY_CLOSED: '◐ Parcialmente cerrado',
  CLOSED: '▲ Cerrado',
};

const UPPER_PERMANENT = [18,17,16,15,14,13,12,11,21,22,23,24,25,26,27,28];
const LOWER_PERMANENT = [48,47,46,45,44,43,42,41,31,32,33,34,35,36,37,38];
const UPPER_PRIMARY   = [55,54,53,52,51,61,62,63,64,65];
const LOWER_PRIMARY   = [85,84,83,82,81,71,72,73,74,75];


@Component({
  selector: 'app-odontogram',
  standalone: true,
  imports: [
    CommonModule, RouterLink, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTooltipModule, MatDividerModule, MatSelectModule,
    MatFormFieldModule, MatInputModule, MatSnackBarModule,
    MatProgressSpinnerModule, MatButtonToggleModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <button mat-icon-button [routerLink]="['/clinical', patientId()]" aria-label="Volver">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <div>
          <h1 class="page-title">Odontograma</h1>
          @if (patient) {
            <p class="page-subtitle">{{ patient.lastName }}, {{ patient.firstName }}</p>
          }
        </div>
        <span class="spacer"></span>
        <mat-form-field appearance="outline" class="dentition-select">
          <mat-label>Dentición</mat-label>
          <mat-select [(ngModel)]="dentitionType" (ngModelChange)="onDentitionChange($event)">
            <mat-option value="PERMANENT">Permanente</mat-option>
            <mat-option value="PRIMARY">Primaria</mat-option>
            <mat-option value="MIXED">Mixta</mat-option>
          </mat-select>
        </mat-form-field>
        <button mat-raised-button color="primary" [disabled]="saving()" (click)="save()">
          @if (saving()) { <mat-spinner diameter="18" style="display:inline-block" /> }
          @else { <mat-icon>save</mat-icon> }
          Guardar Cambios
        </button>
      </div>

      <!-- Legend -->
      <mat-card class="legend-card">
        <mat-card-content>
          <div class="legend-title">Leyenda de condiciones de corona</div>
          <div class="legend-grid">
            @for (entry of legendEntries; track entry.key) {
              <div class="legend-item">
                <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
                  <rect x="1" y="1" width="16" height="16" rx="3"
                    [attr.fill]="entry.fill" [attr.stroke]="entry.stroke" stroke-width="1.5"/>
                </svg>
                <span class="legend-label">{{ entry.label }}</span>
              </div>
            }
          </div>
          <div class="legend-title" style="margin-top:10px">Leyenda de hallazgos radiculares</div>
          <div class="legend-grid">
            @for (rf of allRootFindings; track rf) {
              <div class="legend-item">
                <span class="root-symbol" [style.color]="rootStyle(rf).color">{{ rootStyle(rf).symbol }}</span>
                <span class="legend-label">{{ rootStyle(rf).label }}</span>
              </div>
            }
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Odontogram grid -->
      <mat-card class="odontogram-card">
        <mat-card-header>
          <mat-card-title>Dentición {{ dentitionLabel }}</mat-card-title>
          <mat-card-subtitle>Clic en corona o raíz del diente para editar</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="jaw-label">Maxilar Superior</div>
          <div class="teeth-row">
            @for (tooth of upperTeeth; track tooth.toothNumber) {
              <div class="tooth-wrapper"
                   [class.tooth-selected]="selectedTooth?.toothNumber === tooth.toothNumber"
                   (click)="selectTooth(tooth, 'crown')" role="button"
                   [attr.aria-label]="'Diente ' + tooth.toothNumber"
                   [matTooltip]="'Diente ' + tooth.toothNumber + ' — ' + getMainConditionLabel(tooth)">
                <!-- Root above for upper jaw -->
                <ng-container *ngTemplateOutlet="rootSvgMini; context: { tooth: tooth, isUpper: true }" />
                <ng-container *ngTemplateOutlet="toothSvg; context: { tooth: tooth }" />
                <span class="tooth-num">{{ tooth.toothNumber }}</span>
              </div>
            }
          </div>
          <mat-divider class="jaw-divider" />
          <div class="teeth-row">
            @for (tooth of lowerTeeth; track tooth.toothNumber) {
              <div class="tooth-wrapper"
                   [class.tooth-selected]="selectedTooth?.toothNumber === tooth.toothNumber"
                   (click)="selectTooth(tooth, 'crown')" role="button"
                   [attr.aria-label]="'Diente ' + tooth.toothNumber"
                   [matTooltip]="'Diente ' + tooth.toothNumber + ' — ' + getMainConditionLabel(tooth)">
                <ng-container *ngTemplateOutlet="toothSvg; context: { tooth: tooth }" />
                <!-- Root below for lower jaw -->
                <ng-container *ngTemplateOutlet="rootSvgMini; context: { tooth: tooth, isUpper: false }" />
                <span class="tooth-num">{{ tooth.toothNumber }}</span>
              </div>
            }
          </div>
          <div class="jaw-label">Maxilar Inferior</div>
        </mat-card-content>
      </mat-card>

      <!-- Tooth detail panel -->
      @if (selectedTooth) {
        <mat-card class="detail-card">
          <mat-card-header>
            <mat-card-title>
              <mat-icon class="detail-icon">radio_button_checked</mat-icon>
              Diente {{ selectedTooth.toothNumber }}
            </mat-card-title>
            <mat-card-subtitle>
              <button class="tab-btn" [class.tab-btn-active]="detailTab === 'crown'"
                      (click)="detailTab = 'crown'">Corona</button>
              <button class="tab-btn" [class.tab-btn-active]="detailTab === 'root'"
                      (click)="detailTab = 'root'">Raíz</button>
              <button class="tab-btn" [class.tab-btn-active]="detailTab === 'diagnoses'"
                      (click)="detailTab = 'diagnoses'">
                Diagnósticos
                @if (toothDiagnoses.length > 0) {
                  <span class="dx-badge">{{ toothDiagnoses.length }}</span>
                }
              </button>
            </mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>

            <!-- ── CROWN TAB ── -->
            @if (detailTab === 'crown') {
              <!-- Face selector with SVG preview -->
              <div class="face-section">
                <p class="section-label">Cara activa</p>
                <div class="face-chips">
                  @for (surf of surfaces; track surf) {
                    <button class="face-chip"
                            [class.face-chip-active]="activeSurface === surf"
                            (click)="activeSurface = surf">
                      <span class="face-dot"
                            [style.background]="getSurfaceStyle(selectedTooth, surf).fill"
                            [style.border-color]="getSurfaceStyle(selectedTooth, surf).stroke"></span>
                      {{ surfaceLabel(surf) }}
                    </button>
                  }
                </div>
              </div>

              <!-- Tooth SVG preview (larger, interactive) -->
              <div class="preview-section">
                <div class="preview-svg-wrap">
                  <svg width="100" height="100" viewBox="0 0 50 50" class="preview-svg"
                       [attr.aria-label]="'Vista de diente ' + selectedTooth.toothNumber">
                    <polygon [attr.points]="'0,0 50,0 35,15 15,15'"
                      [attr.fill]="getSurfaceStyle(selectedTooth, 'BUCCAL').fill"
                      [attr.stroke]="getSurfaceStyle(selectedTooth, 'BUCCAL').stroke"
                      stroke-width="1" class="svg-face"
                      [class.svg-face-active]="activeSurface === 'BUCCAL'"
                      (click)="activeSurface = 'BUCCAL'; $event.stopPropagation()"
                      role="button">
                      <title>Vestibular — {{ getSurfaceStyle(selectedTooth, 'BUCCAL').label }}</title>
                    </polygon>
                    <polygon [attr.points]="'0,50 50,50 35,35 15,35'"
                      [attr.fill]="getSurfaceStyle(selectedTooth, 'LINGUAL').fill"
                      [attr.stroke]="getSurfaceStyle(selectedTooth, 'LINGUAL').stroke"
                      stroke-width="1" class="svg-face"
                      [class.svg-face-active]="activeSurface === 'LINGUAL'"
                      (click)="activeSurface = 'LINGUAL'; $event.stopPropagation()"
                      role="button">
                      <title>Lingual — {{ getSurfaceStyle(selectedTooth, 'LINGUAL').label }}</title>
                    </polygon>
                    <polygon [attr.points]="'0,0 15,15 15,35 0,50'"
                      [attr.fill]="getSurfaceStyle(selectedTooth, 'MESIAL').fill"
                      [attr.stroke]="getSurfaceStyle(selectedTooth, 'MESIAL').stroke"
                      stroke-width="1" class="svg-face"
                      [class.svg-face-active]="activeSurface === 'MESIAL'"
                      (click)="activeSurface = 'MESIAL'; $event.stopPropagation()"
                      role="button">
                      <title>Mesial — {{ getSurfaceStyle(selectedTooth, 'MESIAL').label }}</title>
                    </polygon>
                    <polygon [attr.points]="'50,0 50,50 35,35 35,15'"
                      [attr.fill]="getSurfaceStyle(selectedTooth, 'DISTAL').fill"
                      [attr.stroke]="getSurfaceStyle(selectedTooth, 'DISTAL').stroke"
                      stroke-width="1" class="svg-face"
                      [class.svg-face-active]="activeSurface === 'DISTAL'"
                      (click)="activeSurface = 'DISTAL'; $event.stopPropagation()"
                      role="button">
                      <title>Distal — {{ getSurfaceStyle(selectedTooth, 'DISTAL').label }}</title>
                    </polygon>
                    <rect x="15" y="15" width="20" height="20"
                      [attr.fill]="getSurfaceStyle(selectedTooth, 'OCCLUSAL').fill"
                      [attr.stroke]="getSurfaceStyle(selectedTooth, 'OCCLUSAL').stroke"
                      stroke-width="1" class="svg-face"
                      [class.svg-face-active]="activeSurface === 'OCCLUSAL'"
                      (click)="activeSurface = 'OCCLUSAL'; $event.stopPropagation()"
                      role="button">
                      <title>Oclusal — {{ getSurfaceStyle(selectedTooth, 'OCCLUSAL').label }}</title>
                    </rect>
                    @if (selectedTooth.condition === 'ABSENT' || selectedTooth.condition === 'EXTRACTED') {
                      <line x1="5" y1="5" x2="45" y2="45" stroke="#64748b" stroke-width="2"/>
                      <line x1="45" y1="5" x2="5" y2="45" stroke="#64748b" stroke-width="2"/>
                    }
                  </svg>
                  <div class="preview-label">{{ surfaceLabel(activeSurface) }}</div>
                </div>
              </div>

              <!-- Condition picker for active surface -->
              <div class="condition-section">
                <p class="section-label">Condición — cara {{ surfaceLabel(activeSurface) }}</p>
                <div class="condition-chips">
                  @for (cond of conditions; track cond) {
                    <button class="cond-chip"
                            [class.cond-chip-active]="getSurfaceCondition(selectedTooth, activeSurface) === cond"
                            [style.--chip-fill]="getStyle(cond).fill"
                            [style.--chip-stroke]="getStyle(cond).stroke"
                            (click)="setSurfaceCondition(selectedTooth, activeSurface, cond)">
                      <span class="cond-dot"
                            [style.background]="getStyle(cond).fill"
                            [style.border-color]="getStyle(cond).stroke"></span>
                      {{ getStyle(cond).label }}
                    </button>
                  }
                </div>
              </div>

              <!-- Whole-tooth overrides -->
              <div class="whole-tooth-section">
                <p class="section-label">Estado completo del diente</p>
                <div class="whole-chips">
                  @for (cond of wholeTooth; track cond) {
                    <button class="cond-chip"
                            [class.cond-chip-active]="selectedTooth.condition === cond"
                            [style.--chip-fill]="getStyle(cond).fill"
                            [style.--chip-stroke]="getStyle(cond).stroke"
                            (click)="setWholeTooth(selectedTooth, cond)">
                      <span class="cond-dot"
                            [style.background]="getStyle(cond).fill"
                            [style.border-color]="getStyle(cond).stroke"></span>
                      {{ getStyle(cond).label }}
                    </button>
                  }
                </div>
              </div>

              @if (selectedTooth.condition === 'ABSENT') {
                <div class="space-section">
                  <p class="section-label">Estado del espacio</p>
                  <mat-button-toggle-group [(ngModel)]="selectedTooth.spaceStatus" class="space-toggle">
                    <mat-button-toggle value="OPEN">△ Abierto</mat-button-toggle>
                    <mat-button-toggle value="PARTIALLY_CLOSED">◐ Parcial</mat-button-toggle>
                    <mat-button-toggle value="CLOSED">▲ Cerrado</mat-button-toggle>
                  </mat-button-toggle-group>
                </div>
              }

              <mat-form-field appearance="outline" class="notes-field">
                <mat-label>Notas del diente</mat-label>
                <textarea matInput rows="2" [(ngModel)]="selectedTooth.notes"
                          placeholder="Observaciones adicionales…"></textarea>
              </mat-form-field>
            }

            <!-- ── ROOT TAB ── -->
            @if (detailTab === 'root') {
              <div class="root-preview-section">
                <!-- Mini root SVG preview -->
                <svg width="50" height="80" viewBox="0 0 30 60" class="root-preview-svg" aria-hidden="true">
                  <!-- Root body (trapezoid tapering to apex) -->
                  <polygon points="5,0 25,0 20,55 10,55"
                    fill="#f1f8f8" stroke="#b2d8d8" stroke-width="1.5"
                    [attr.fill]="hasRootFindings ? '#fff7ed' : '#f1f8f8'"
                    [attr.stroke]="hasRootFindings ? '#f59e0b' : '#b2d8d8'"/>
                  <!-- Endodontic treatment: vertical fill line -->
                  @if (hasRootFinding('ENDODONTIC_TREATMENT')) {
                    <line x1="15" y1="2" x2="15" y2="52" stroke="#dc2626" stroke-width="3"/>
                  }
                  <!-- Post/core: pin inside canal -->
                  @if (hasRootFinding('POST_CORE')) {
                    <rect x="12" y="5" width="6" height="35" fill="#0d9488" rx="1"/>
                  }
                  <!-- Root fracture: diagonal line -->
                  @if (hasRootFinding('ROOT_FRACTURE')) {
                    <line x1="5" y1="20" x2="25" y2="35" stroke="#78716c" stroke-width="2" stroke-dasharray="3,2"/>
                  }
                  <!-- Internal resorption: oval in canal -->
                  @if (hasRootFinding('INTERNAL_RESORPTION')) {
                    <ellipse cx="15" cy="28" rx="6" ry="10" fill="none" stroke="#a855f7" stroke-width="1.5"/>
                  }
                  <!-- External resorption: notch on side -->
                  @if (hasRootFinding('EXTERNAL_RESORPTION')) {
                    <polygon points="5,25 12,22 12,32" fill="#8b5cf6"/>
                  }
                  <!-- Apicoectomy: flat apex cut -->
                  @if (hasRootFinding('APICOECTOMY')) {
                    <line x1="10" y1="52" x2="20" y2="52" stroke="#64748b" stroke-width="3"/>
                    <line x1="10" y1="48" x2="20" y2="48" stroke="#64748b" stroke-width="1.5"/>
                  }
                  <!-- Hypercementosis: bulbous apex -->
                  @if (hasRootFinding('HYPERCEMENTOSIS')) {
                    <ellipse cx="15" cy="54" rx="8" ry="5" fill="none" stroke="#ca8a04" stroke-width="2"/>
                  }
                  <!-- Periapical abscess: filled circle at apex -->
                  @if (hasRootFinding('PERIAPICAL_ABSCESS')) {
                    <circle cx="15" cy="62" r="5" fill="#ef4444" opacity="0.7"/>
                    <circle cx="15" cy="62" r="8" fill="none" stroke="#ef4444" stroke-width="1" opacity="0.4"/>
                  }
                  <!-- Periapical lesion: triangle at apex -->
                  @if (hasRootFinding('PERIAPICAL_LESION')) {
                    <polygon points="15,56 8,68 22,68"
                      fill="none" stroke="#f59e0b" stroke-width="2"/>
                  }
                </svg>
              </div>

              <p class="section-label" style="margin-top:4px">Hallazgos radiculares (multi-selección)</p>
              <div class="root-chips">
                @for (rf of allRootFindings; track rf) {
                  <button class="root-chip"
                          [class.root-chip-active]="hasRootFinding(rf)"
                          [style.--root-color]="rootStyle(rf).color"
                          (click)="toggleRootFinding(selectedTooth, rf)">
                    <span class="root-sym" [style.color]="rootStyle(rf).color">{{ rootStyle(rf).symbol }}</span>
                    {{ rootStyle(rf).label }}
                  </button>
                }
              </div>

              <mat-form-field appearance="outline" class="notes-field" style="margin-top:16px">
                <mat-label>Notas de la raíz</mat-label>
                <textarea matInput rows="2" [(ngModel)]="selectedTooth.rootNotes"
                          placeholder="Notas endodónticas…"></textarea>
              </mat-form-field>
            }

            <!-- ── DIAGNOSES TAB ── -->
            @if (detailTab === 'diagnoses') {
              @if (toothDiagnoses.length > 0) {
                <div class="tooth-dx-list">
                  @for (d of toothDiagnoses; track d.id) {
                    <div class="tooth-dx-item">
                      <span class="tooth-dx-code">{{ d.code }}</span>
                      <span class="tooth-dx-desc">{{ d.description }}</span>
                      @if (d.diagnosedAt) {
                        <span class="tooth-dx-date">{{ d.diagnosedAt | date:'dd/MM/yy' }}</span>
                      }
                    </div>
                  }
                </div>
              } @else {
                <p class="empty-hint">No hay diagnósticos asociados a este diente.</p>
              }
              <p class="dx-hint">Para añadir un diagnóstico a este diente, ve a la pestaña
                <a [routerLink]="['/clinical', patientId()]" class="dx-link">Historia Clínica</a>
                e introduce el nº de diente <strong>{{ selectedTooth.toothNumber }}</strong>.</p>
            }

          </mat-card-content>
        </mat-card>
      }
    </div>

    <!-- Crown SVG template (grid size) -->
    <ng-template #toothSvg let-tooth="tooth">
      <svg width="40" height="40" viewBox="0 0 50 50" class="tooth-svg"
           [attr.aria-hidden]="true">
        <polygon points="0,0 50,0 35,15 15,15"
          [attr.fill]="getSurfaceStyle(tooth, 'BUCCAL').fill"
          [attr.stroke]="getSurfaceStyle(tooth, 'BUCCAL').stroke"
          stroke-width="1.5"/>
        <polygon points="0,50 50,50 35,35 15,35"
          [attr.fill]="getSurfaceStyle(tooth, 'LINGUAL').fill"
          [attr.stroke]="getSurfaceStyle(tooth, 'LINGUAL').stroke"
          stroke-width="1.5"/>
        <polygon points="0,0 15,15 15,35 0,50"
          [attr.fill]="getSurfaceStyle(tooth, 'MESIAL').fill"
          [attr.stroke]="getSurfaceStyle(tooth, 'MESIAL').stroke"
          stroke-width="1.5"/>
        <polygon points="50,0 50,50 35,35 35,15"
          [attr.fill]="getSurfaceStyle(tooth, 'DISTAL').fill"
          [attr.stroke]="getSurfaceStyle(tooth, 'DISTAL').stroke"
          stroke-width="1.5"/>
        <rect x="15" y="15" width="20" height="20"
          [attr.fill]="getSurfaceStyle(tooth, 'OCCLUSAL').fill"
          [attr.stroke]="getSurfaceStyle(tooth, 'OCCLUSAL').stroke"
          stroke-width="1.5"/>
        @if (tooth.condition === 'ABSENT' || tooth.condition === 'EXTRACTED') {
          <line x1="5" y1="5" x2="45" y2="45" stroke="#64748b" stroke-width="2"/>
          <line x1="45" y1="5" x2="5" y2="45" stroke="#64748b" stroke-width="2"/>
        }
        @if (tooth.condition === 'IMPLANT') {
          <circle cx="25" cy="25" r="6" fill="none" stroke="#0d9488" stroke-width="2"/>
          <circle cx="25" cy="25" r="2" fill="#0d9488"/>
        }
      </svg>
    </ng-template>

    <!-- Root SVG template (mini, grid) -->
    <ng-template #rootSvgMini let-tooth="tooth" let-isUpper="isUpper">
      <svg width="20" height="18" viewBox="0 0 30 24" class="root-mini-svg"
           aria-hidden="true">
        <!-- Root trapezoid -->
        <polygon [attr.points]="isUpper ? '5,24 25,24 20,2 10,2' : '5,0 25,0 20,22 10,22'"
          [attr.fill]="(tooth.rootFindings?.length) ? '#fff7ed' : '#f1f8f8'"
          [attr.stroke]="(tooth.rootFindings?.length) ? '#f59e0b' : '#d1d5db'"
          stroke-width="1.2"/>
        <!-- Endodontic line -->
        @if (toothHasRootFinding(tooth, 'ENDODONTIC_TREATMENT')) {
          <line [attr.x1]="15" [attr.y1]="isUpper ? 22 : 2"
                [attr.x2]="15" [attr.y2]="isUpper ? 4 : 20"
                stroke="#dc2626" stroke-width="2"/>
        }
        <!-- Periapical lesion triangle at apex -->
        @if (toothHasRootFinding(tooth, 'PERIAPICAL_LESION')) {
          <polygon [attr.points]="isUpper ? '15,0 10,-6 20,-6' : '15,24 10,30 20,30'"
            fill="none" stroke="#f59e0b" stroke-width="1.5"/>
        }
      </svg>
    </ng-template>
  `,
  styles: [`
    .page-header { gap: 12px; margin-bottom: 20px; flex-wrap: wrap; align-items: center; }
    .dentition-select { width: 160px; }

    /* Legend */
    .legend-card { margin-bottom: 16px; }
    .legend-title { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: var(--dentis-text-muted); margin-bottom: 10px; }
    .legend-grid { display: flex; flex-wrap: wrap; gap: 8px 20px; }
    .legend-item { display: flex; align-items: center; gap: 6px; }
    .legend-label { font-size: 11.5px; color: var(--dentis-text-muted); }
    .root-symbol { font-size: 16px; width: 18px; text-align: center; flex-shrink: 0; line-height: 1; }

    /* Odontogram */
    .odontogram-card { margin-bottom: 16px; }
    .jaw-label { text-align: center; font-size: 11px; color: var(--dentis-text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: .06em; padding: 6px; }
    .teeth-row { display: flex; justify-content: center; gap: 3px; flex-wrap: wrap; padding: 6px 0; }
    .tooth-wrapper {
      display: flex; flex-direction: column; align-items: center; gap: 0;
      cursor: pointer; border-radius: 6px; padding: 3px;
      transition: background var(--dentis-transition, .2s ease), transform .15s;
    }
    .tooth-wrapper:hover { background: rgba(13,148,136,.08); transform: scale(1.08); }
    .tooth-selected { background: rgba(13,148,136,.14) !important; outline: 2px solid var(--dentis-primary); outline-offset: 1px; border-radius: 6px; }
    .tooth-svg { display: block; }
    .root-mini-svg { display: block; overflow: visible; }
    .tooth-num { font-size: 10px; font-weight: 700; color: var(--dentis-text-muted); }
    .jaw-divider { margin: 6px 0; }

    /* Detail card */
    .detail-card { max-width: 760px; }
    .detail-icon { font-size: 18px; width: 18px; height: 18px; color: var(--dentis-primary); margin-right: 6px; vertical-align: middle; }

    /* Detail tabs */
    .tab-btn {
      background: none; border: none; cursor: pointer; padding: 4px 14px;
      font-size: 13px; font-weight: 600; color: var(--dentis-text-muted);
      border-bottom: 2px solid transparent; transition: all .15s; margin-right: 4px;
    }
    .tab-btn:hover { color: var(--dentis-primary); }
    .tab-btn-active { color: var(--dentis-primary); border-bottom-color: var(--dentis-primary); }
    .dx-badge {
      display: inline-flex; align-items: center; justify-content: center;
      background: var(--dentis-primary); color: #fff; border-radius: 999px;
      font-size: 10px; font-weight: 700; width: 16px; height: 16px; margin-left: 4px;
    }

    /* Section labels */
    .section-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: var(--dentis-text-muted); margin: 12px 0 8px; }

    /* Face chips */
    .face-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 4px; }
    .face-chip {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 5px 12px; border-radius: 999px; border: 1.5px solid var(--dentis-border);
      background: var(--dentis-surface); cursor: pointer; font-size: 12.5px; font-weight: 600;
      transition: all .15s; color: var(--dentis-text-muted);
    }
    .face-chip:hover { border-color: var(--dentis-primary); color: var(--dentis-primary); }
    .face-chip-active { border-color: var(--dentis-primary) !important; background: rgba(13,148,136,.08); color: var(--dentis-primary) !important; }
    .face-dot { width: 10px; height: 10px; border-radius: 50%; border: 1.5px solid; flex-shrink: 0; }

    /* SVG preview */
    .preview-section { display: flex; align-items: center; gap: 12px; margin: 8px 0 16px; }
    .preview-svg-wrap { text-align: center; }
    .preview-svg { display: block; filter: drop-shadow(0 4px 12px rgba(15,23,42,.12)); cursor: pointer; }
    .preview-label { font-size: 11px; font-weight: 600; color: var(--dentis-primary); text-transform: uppercase; letter-spacing: .05em; margin-top: 4px; }
    .svg-face { cursor: pointer; transition: opacity .15s; }
    .svg-face:hover { opacity: .78; }
    .svg-face-active { stroke-width: 2.5 !important; filter: brightness(.88); }

    /* Condition chips */
    .condition-chips, .whole-chips { display: flex; flex-wrap: wrap; gap: 6px; }
    .cond-chip {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 5px 12px; border-radius: 999px; border: 1.5px solid var(--dentis-border);
      background: var(--dentis-surface); cursor: pointer; font-size: 12px; font-weight: 500;
      color: var(--dentis-text-muted); transition: all .15s;
    }
    .cond-chip:hover { border-color: var(--chip-stroke, var(--dentis-primary)); background: var(--chip-fill, rgba(13,148,136,.05)); }
    .cond-chip-active {
      border-color: var(--chip-stroke, var(--dentis-primary)) !important;
      background: var(--chip-fill, rgba(13,148,136,.1)) !important;
      color: var(--dentis-text) !important; font-weight: 700;
    }
    .cond-dot { width: 10px; height: 10px; border-radius: 50%; border: 1.5px solid; flex-shrink: 0; }

    .condition-section { margin-bottom: 16px; }
    .whole-tooth-section { margin-bottom: 16px; }
    .space-section { margin-bottom: 16px; }
    .space-toggle { margin-bottom: 8px; }
    .notes-field { width: 100%; margin-top: 4px; }

    /* Root panel */
    .root-preview-section { display: flex; justify-content: center; padding: 8px 0; }
    .root-preview-svg { display: block; overflow: visible; filter: drop-shadow(0 2px 8px rgba(15,23,42,.1)); }
    .root-chips { display: flex; flex-wrap: wrap; gap: 6px; }
    .root-chip {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 5px 12px; border-radius: 999px; border: 1.5px solid var(--dentis-border);
      background: var(--dentis-surface); cursor: pointer; font-size: 12px; font-weight: 500;
      color: var(--dentis-text-muted); transition: all .15s;
    }
    .root-chip:hover { border-color: var(--root-color, var(--dentis-primary)); }
    .root-chip-active {
      border-color: var(--root-color, var(--dentis-primary)) !important;
      background: color-mix(in srgb, var(--root-color, var(--dentis-primary)) 10%, transparent) !important;
      color: var(--dentis-text) !important; font-weight: 700;
    }
    .root-sym { font-size: 15px; width: 16px; text-align: center; flex-shrink: 0; line-height: 1; }

    /* Diagnoses tab */
    .tooth-dx-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
    .tooth-dx-item {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 12px; border-radius: 8px;
      background: var(--dentis-surface); border: 1px solid var(--dentis-border);
    }
    .tooth-dx-code { font-size: 13px; font-weight: 700; color: var(--dentis-primary); min-width: 60px; }
    .tooth-dx-desc { font-size: 13px; flex: 1; }
    .tooth-dx-date { font-size: 11px; color: var(--dentis-text-muted); white-space: nowrap; }
    .empty-hint { color: var(--dentis-text-muted); font-size: 13px; padding: 12px 0; }
    .dx-hint { font-size: 12px; color: var(--dentis-text-muted); margin-top: 12px; }
    .dx-link { color: var(--dentis-primary); text-decoration: none; font-weight: 600; }
  `],
})
export class OdontogramComponent implements OnInit {
  patientId = input<string>('');
  patient: Patient | null = null;
  record: ClinicalRecord | null = null;
  selectedTooth: OdontogramTooth | null = null;
  activeSurface: ToothSurface = 'OCCLUSAL';
  saving = signal(false);
  dentitionType: DentitionType = 'PERMANENT';
  detailTab: 'crown' | 'root' | 'diagnoses' = 'crown';

  upperTeeth: OdontogramTooth[] = [];
  lowerTeeth: OdontogramTooth[] = [];

  readonly surfaces: ToothSurface[] = SURFACES;
  readonly conditions: ToothCondition[] = [
    'HEALTHY','CARIES','RESTORED','FRACTURE','DEFECTIVE_FILLING','ERUPTING','IMPACTED',
  ];
  readonly wholeTooth: ToothCondition[] = [
    'ABSENT','EXTRACTED','CROWNED','IMPLANT','ROOT_CANAL','ROOT_REMNANT','MALFORMATION',
  ];
  readonly legendEntries = Object.entries(CONDITION_STYLE).map(([key, v]) => ({ key, ...v }));
  readonly allRootFindings: RootFinding[] = ALL_ROOT_FINDINGS;

  get dentitionLabel(): string {
    return { PERMANENT: 'Permanente', PRIMARY: 'Primaria', MIXED: 'Mixta' }[this.dentitionType];
  }

  get hasRootFindings(): boolean {
    return (this.selectedTooth?.rootFindings?.length ?? 0) > 0;
  }

  get toothDiagnoses(): Diagnosis[] {
    if (!this.selectedTooth || !this.record) return [];
    return (this.record.diagnoses ?? []).filter(
      (d) => d.toothNumber === this.selectedTooth!.toothNumber
    );
  }

  private patientSvc = inject(PatientService);
  private clinicalSvc = inject(ClinicalService);
  private snack = inject(MatSnackBar);

  ngOnInit(): void {
    const id = this.patientId();
    if (!id) return;
    this.patientSvc.getById(id).subscribe((p) => (this.patient = p));
    this.clinicalSvc.getOrCreate(id).subscribe((rec) => this.applyRecord(rec));
  }

  private applyRecord(rec: ClinicalRecord): void {
    this.record = rec;
    this.dentitionType = rec.dentitionType ?? 'PERMANENT';
    this.buildTeeth(rec.odontogram ?? []);
  }

  private buildTeeth(savedTeeth: OdontogramTooth[]): void {
    const savedMap = new Map(savedTeeth.map((t) => [t.toothNumber, t]));
    const upper = this.dentitionType === 'PRIMARY' ? UPPER_PRIMARY : UPPER_PERMANENT;
    const lower = this.dentitionType === 'PRIMARY' ? LOWER_PRIMARY : LOWER_PERMANENT;
    this.upperTeeth = upper.map((n) => savedMap.get(n) ?? { toothNumber: n, condition: 'HEALTHY' });
    this.lowerTeeth = lower.map((n) => savedMap.get(n) ?? { toothNumber: n, condition: 'HEALTHY' });
  }

  onDentitionChange(type: DentitionType): void {
    this.selectedTooth = null;
    if (this.record) {
      this.buildTeeth(this.record.odontogram ?? []);
      this.clinicalSvc.updateDentitionType(this.patientId(), type).subscribe();
    }
  }

  getStyle(condition: string): ToothStyle {
    return CONDITION_STYLE[condition] ?? CONDITION_STYLE['HEALTHY'];
  }

  rootStyle(rf: RootFinding): RootFindingStyle {
    return ROOT_FINDING_STYLE[rf];
  }

  getSurfaceCondition(tooth: OdontogramTooth, surface: ToothSurface): ToothCondition {
    return tooth.surfaceConditions?.[surface] ?? tooth.condition ?? 'HEALTHY';
  }

  getSurfaceStyle(tooth: OdontogramTooth, surface: ToothSurface): ToothStyle {
    const cond = this.getSurfaceCondition(tooth, surface);
    return this.getStyle(cond);
  }

  getMainConditionLabel(tooth: OdontogramTooth): string {
    return this.getStyle(tooth.condition).label;
  }

  surfaceLabel(surface: ToothSurface): string {
    return SURFACE_LABEL[surface];
  }

  spaceLabel(status: SpaceStatus): string {
    return SPACE_LABEL[status] ?? '';
  }

  hasRootFinding(rf: RootFinding): boolean {
    return this.selectedTooth?.rootFindings?.includes(rf) ?? false;
  }

  toothHasRootFinding(tooth: OdontogramTooth, rf: RootFinding): boolean {
    return tooth.rootFindings?.includes(rf) ?? false;
  }

  selectTooth(tooth: OdontogramTooth, tab: 'crown' | 'root' | 'diagnoses' = 'crown'): void {
    this.selectedTooth = tooth;
    this.activeSurface = 'OCCLUSAL';
    this.detailTab = tab;
  }

  setSurfaceCondition(tooth: OdontogramTooth, surface: ToothSurface, cond: ToothCondition): void {
    tooth.surfaceConditions = { ...tooth.surfaceConditions, [surface]: cond };
    if (cond !== 'HEALTHY') {
      tooth.condition = cond;
    }
  }

  setWholeTooth(tooth: OdontogramTooth, cond: ToothCondition): void {
    tooth.condition = cond;
    tooth.surfaceConditions = {};
    if (cond !== 'ABSENT') tooth.spaceStatus = undefined;
  }

  toggleRootFinding(tooth: OdontogramTooth, rf: RootFinding): void {
    const current = tooth.rootFindings ?? [];
    if (current.includes(rf)) {
      tooth.rootFindings = current.filter((r) => r !== rf);
    } else {
      tooth.rootFindings = [...current, rf];
    }
  }

  save(): void {
    if (this.saving()) return;
    this.saving.set(true);

    const allTeeth: OdontogramTooth[] = [...this.upperTeeth, ...this.lowerTeeth];

    this.clinicalSvc.updateOdontogram(this.patientId(), allTeeth).subscribe({
      next: (rec) => {
        this.applyRecord(rec);
        this.selectedTooth = null;
        this.saving.set(false);
        this.snack.open('Odontograma guardado', 'OK', { duration: 3000 });
      },
      error: () => {
        this.saving.set(false);
        this.snack.open('Error al guardar', 'Cerrar', { duration: 4000 });
      },
    });
  }
}
