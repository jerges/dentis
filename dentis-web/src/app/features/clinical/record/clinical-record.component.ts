import { Component, OnInit, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PatientService } from '../../../core/services/patient.service';
import { ClinicalService } from '../../../core/services/clinical.service';
import { PdfService } from '../../../core/services/pdf.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalEvolution, ClinicalRecord, Diagnosis, TreatmentPlan } from '../../../core/models/clinical.model';
import { ClinicalAttachmentsComponent } from '../attachments/clinical-attachments.component';

@Component({
  selector: 'app-clinical-record',
  standalone: true,
  imports: [
    CommonModule, RouterLink, FormsModule, ReactiveFormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTabsModule, MatDividerModule, MatChipsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSnackBarModule, MatProgressSpinnerModule,
    ClinicalAttachmentsComponent
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
        @if (record) {
          <button mat-stroked-button (click)="exportPdf()">
            <mat-icon>picture_as_pdf</mat-icon> Exportar PDF
          </button>
        }
      </div>

      @if (loading()) {
        <div class="loading-center">
          <mat-spinner diameter="48"></mat-spinner>
        </div>
      } @else if (record) {
        <mat-tab-group animationDuration="200ms">

          <!-- EVOLUTIONS TAB -->
          <mat-tab label="Evoluciones Clínicas">
            <div class="tab-content">
              <div class="tab-actions">
                <button mat-raised-button color="primary" (click)="showAddEvolution = !showAddEvolution">
                  <mat-icon>add</mat-icon> Nueva Evolución
                </button>
              </div>

              @if (showAddEvolution) {
                <mat-card class="add-card" [formGroup]="evolutionForm">
                  <mat-card-header><mat-card-title>Nueva Evolución</mat-card-title></mat-card-header>
                  <mat-card-content>
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Descripción *</mat-label>
                      <textarea matInput rows="3" formControlName="description"></textarea>
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Hallazgos</mat-label>
                      <textarea matInput rows="2" formControlName="findings"></textarea>
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Tratamiento realizado</mat-label>
                      <textarea matInput rows="2" formControlName="treatment"></textarea>
                    </mat-form-field>
                  </mat-card-content>
                  <mat-card-actions align="end">
                    <button mat-button (click)="showAddEvolution = false">Cancelar</button>
                    <button mat-raised-button color="primary"
                            [disabled]="evolutionForm.invalid || savingEvolution()"
                            (click)="submitEvolution()">
                      Guardar
                    </button>
                  </mat-card-actions>
                </mat-card>
              }

              @for (ev of record.evolutions; track ev.id) {
                <mat-card class="evolution-card">
                  <mat-card-content>
                    <div class="flex-row">
                      <div class="evo-content">
                        <div class="evo-date">{{ ev.recordedAt | date:'dd/MM/yyyy HH:mm' }}</div>
                        <div class="evo-desc">{{ ev.description }}</div>
                        @if (ev.findings) {
                          <div class="evo-sub"><strong>Hallazgos:</strong> {{ ev.findings }}</div>
                        }
                        @if (ev.treatment) {
                          <div class="evo-sub"><strong>Tratamiento:</strong> {{ ev.treatment }}</div>
                        }
                      </div>
                    </div>
                  </mat-card-content>
                </mat-card>
              } @empty {
                <p class="empty-hint">No hay evoluciones registradas.</p>
              }
            </div>
          </mat-tab>

          <!-- DIAGNOSES TAB -->
          <mat-tab label="Diagnósticos">
            <div class="tab-content">
              <div class="tab-actions">
                <button mat-raised-button color="primary" (click)="showAddDiagnosis = !showAddDiagnosis">
                  <mat-icon>add</mat-icon> Nuevo Diagnóstico
                </button>
              </div>

              @if (showAddDiagnosis) {
                <mat-card class="add-card" [formGroup]="diagnosisForm">
                  <mat-card-header><mat-card-title>Nuevo Diagnóstico</mat-card-title></mat-card-header>
                  <mat-card-content>
                    <div class="form-row">
                      <mat-form-field appearance="outline">
                        <mat-label>Código (CIE-10) *</mat-label>
                        <input matInput formControlName="code" placeholder="K02.1">
                      </mat-form-field>
                      <mat-form-field appearance="outline" class="flex-1">
                        <mat-label>Descripción *</mat-label>
                        <input matInput formControlName="description">
                      </mat-form-field>
                      <mat-form-field appearance="outline" style="width:110px">
                        <mat-label>Nº diente</mat-label>
                        <input matInput type="number" formControlName="toothNumber" placeholder="36">
                      </mat-form-field>
                    </div>
                  </mat-card-content>
                  <mat-card-actions align="end">
                    <button mat-button (click)="showAddDiagnosis = false">Cancelar</button>
                    <button mat-raised-button color="primary"
                            [disabled]="diagnosisForm.invalid || savingDiagnosis()"
                            (click)="submitDiagnosis()">
                      Guardar
                    </button>
                  </mat-card-actions>
                </mat-card>
              }

              <div class="diagnoses-grid">
                @for (d of record.diagnoses; track d.id) {
                  <mat-card class="diagnosis-card">
                    <mat-card-content>
                      <div class="diagnosis-code">{{ d.code }}</div>
                      <div class="diagnosis-name">{{ d.description }}</div>
                      @if (d.toothNumber) {
                        <div class="diagnosis-tooth">Diente {{ d.toothNumber }}</div>
                      }
                      @if (d.diagnosedAt) {
                        <div class="diagnosis-date">{{ d.diagnosedAt | date:'dd/MM/yyyy' }}</div>
                      }
                    </mat-card-content>
                  </mat-card>
                } @empty {
                  <p class="empty-hint">No hay diagnósticos registrados.</p>
                }
              </div>
            </div>
          </mat-tab>

          <!-- TREATMENT PLANS TAB -->
          <mat-tab label="Planes de Tratamiento">
            <div class="tab-content">
              <div class="tab-actions">
                <button mat-raised-button color="primary" (click)="showAddPlan = !showAddPlan">
                  <mat-icon>add</mat-icon> Nuevo Plan
                </button>
              </div>

              @if (showAddPlan) {
                <mat-card class="add-card" [formGroup]="planForm">
                  <mat-card-header><mat-card-title>Nuevo Plan de Tratamiento</mat-card-title></mat-card-header>
                  <mat-card-content>
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Título *</mat-label>
                      <input matInput formControlName="title">
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="full-width">
                      <mat-label>Descripción</mat-label>
                      <textarea matInput rows="3" formControlName="description"></textarea>
                    </mat-form-field>
                  </mat-card-content>
                  <mat-card-actions align="end">
                    <button mat-button (click)="showAddPlan = false">Cancelar</button>
                    <button mat-raised-button color="primary"
                            [disabled]="planForm.invalid || savingPlan()"
                            (click)="submitPlan()">
                      Guardar
                    </button>
                  </mat-card-actions>
                </mat-card>
              }

              @for (plan of record.treatmentPlans; track plan.id) {
                <mat-card class="plan-card">
                  <mat-card-content>
                    <div class="plan-title">{{ plan.title }}</div>
                    <div class="plan-status">{{ plan.status }}</div>
                    @if (plan.description) {
                      <div class="plan-desc">{{ plan.description }}</div>
                    }
                  </mat-card-content>
                </mat-card>
              } @empty {
                <p class="empty-hint">No hay planes de tratamiento activos.</p>
              }
            </div>
          </mat-tab>

          <!-- ATTACHMENTS TAB -->
          <mat-tab label="Adjuntos">
            <div class="tab-content">
              <app-clinical-attachments [patientId]="patientId()"></app-clinical-attachments>
            </div>
          </mat-tab>

        </mat-tab-group>
      }
    </div>
  `,
  styles: [`
    .page-header { gap: 12px; margin-bottom: 24px; flex-wrap: wrap; align-items: center; }
    .page-title { margin: 0 0 4px; font-size: 22px; font-weight: 700; }
    .loading-center { display: flex; justify-content: center; padding: 80px 0; }
    .tab-content { padding: 24px 0; }
    .tab-actions { margin-bottom: 16px; }
    .add-card { margin-bottom: 16px; background: var(--dentis-surface-alt); }
    .full-width { width: 100%; }
    .form-row { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .flex-1 { flex: 1; min-width: 200px; }
    .evolution-card { margin-bottom: 12px; }
    .evo-content { flex: 1; }
    .evo-date { font-weight: 600; color: var(--dentis-primary); font-size: 13px; }
    .evo-desc { margin: 6px 0 4px; }
    .evo-sub { font-size: 13px; color: var(--dentis-text-muted); margin-top: 4px; }
    .diagnoses-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
    .diagnosis-card { text-align: center; }
    .diagnosis-code { font-size: 20px; font-weight: 700; color: var(--dentis-primary); }
    .diagnosis-name { font-size: 13px; margin: 4px 0; }
    .diagnosis-tooth { font-size: 11px; font-weight: 600; color: var(--dentis-primary); margin: 2px 0; }
    .diagnosis-date { font-size: 11px; color: var(--dentis-text-muted); }
    .plan-card { margin-bottom: 12px; }
    .plan-title { font-weight: 600; font-size: 15px; }
    .plan-status { font-size: 12px; color: var(--dentis-text-muted); margin: 2px 0 6px; }
    .plan-desc { font-size: 13px; color: var(--dentis-text-muted); }
    .empty-hint { color: var(--dentis-text-muted); text-align: center; padding: 40px; }
  `]
})
export class ClinicalRecordComponent implements OnInit {
  patientId = input<string>('');
  patient: Patient | null = null;
  record: ClinicalRecord | null = null;
  loading = signal(true);
  showAddEvolution = false;
  showAddDiagnosis = false;
  showAddPlan = false;
  savingEvolution = signal(false);
  savingDiagnosis = signal(false);
  savingPlan = signal(false);

  private patientSvc = inject(PatientService);
  private clinicalSvc = inject(ClinicalService);
  private pdfSvc = inject(PdfService);
  private snack = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  evolutionForm = this.fb.group({
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    findings: ['', Validators.maxLength(2000)],
    treatment: ['', Validators.maxLength(2000)],
  });

  diagnosisForm = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(20)]],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    toothNumber: [null as number | null],
  });

  planForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', Validators.maxLength(2000)],
  });

  ngOnInit(): void {
    const id = this.patientId();
    if (!id) return;
    this.patientSvc.getById(id).subscribe((p) => (this.patient = p));
    this.clinicalSvc.getOrCreate(id).subscribe({
      next: (rec) => { this.record = rec; this.loading.set(false); },
      error: () => { this.loading.set(false); }
    });
  }

  exportPdf(): void {
    if (this.record && this.patient) {
      this.pdfSvc.exportClinicalRecord(this.patient, this.record);
    }
  }

  submitEvolution(): void {
    if (this.evolutionForm.invalid || this.savingEvolution()) return;
    this.savingEvolution.set(true);
    const { description, findings, treatment } = this.evolutionForm.getRawValue();
    this.clinicalSvc.addEvolution(this.patientId(), {
      dentistId: '00000000-0000-0000-0000-000000000000', // TODO: inject current user dentistId
      description: description!,
      findings: findings ?? undefined,
      treatment: treatment ?? undefined,
    }).subscribe({
      next: (rec) => {
        this.record = rec;
        this.evolutionForm.reset();
        this.showAddEvolution = false;
        this.savingEvolution.set(false);
        this.snack.open('Evolución registrada', 'OK', { duration: 3000 });
      },
      error: () => {
        this.savingEvolution.set(false);
        this.snack.open('Error al guardar evolución', 'Cerrar', { duration: 4000 });
      }
    });
  }

  submitDiagnosis(): void {
    if (this.diagnosisForm.invalid || this.savingDiagnosis()) return;
    this.savingDiagnosis.set(true);
    const { code, description, toothNumber } = this.diagnosisForm.getRawValue();
    this.clinicalSvc.addDiagnosis(this.patientId(), {
      code: code!,
      description: description!,
      toothNumber: toothNumber ?? undefined,
    }).subscribe({
      next: (rec) => {
        this.record = rec;
        this.diagnosisForm.reset();
        this.showAddDiagnosis = false;
        this.savingDiagnosis.set(false);
        this.snack.open('Diagnóstico registrado', 'OK', { duration: 3000 });
      },
      error: () => {
        this.savingDiagnosis.set(false);
        this.snack.open('Error al guardar diagnóstico', 'Cerrar', { duration: 4000 });
      }
    });
  }

  submitPlan(): void {
    if (this.planForm.invalid || this.savingPlan()) return;
    this.savingPlan.set(true);
    const { title, description } = this.planForm.getRawValue();
    this.clinicalSvc.addTreatmentPlan(this.patientId(), {
      dentistId: '00000000-0000-0000-0000-000000000000', // TODO: inject current user dentistId
      title: title!,
      description: description ?? undefined,
    }).subscribe({
      next: (rec) => {
        this.record = rec;
        this.planForm.reset();
        this.showAddPlan = false;
        this.savingPlan.set(false);
        this.snack.open('Plan de tratamiento guardado', 'OK', { duration: 3000 });
      },
      error: () => {
        this.savingPlan.set(false);
        this.snack.open('Error al guardar plan', 'Cerrar', { duration: 4000 });
      }
    });
  }
}
