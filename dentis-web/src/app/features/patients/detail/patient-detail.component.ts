import { Component, OnInit, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { PatientService } from '../../../core/services/patient.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { Patient } from '../../../core/models/patient.model';
import { Appointment } from '../../../core/models/appointment.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTabsModule, MatDividerModule, MatChipsModule, MatListModule,
    PageHeaderComponent
  ],
  template: `
    <div class="page-container">
      @if (patient) {
        <app-page-header [title]="patient.lastName + ', ' + patient.firstName" backRoute="/patients">
          <span class="status-chip" [class]="patient.active ? 'status-active' : 'status-inactive'">
            {{ patient.active ? 'Activo' : 'Inactivo' }}
          </span>
          <button mat-stroked-button [routerLink]="['/patients', patient.id, 'edit']">
            <mat-icon>edit</mat-icon> Editar
          </button>
          <button mat-raised-button color="primary" [routerLink]="['/clinical', patient.id]">
            <mat-icon>medical_services</mat-icon> Historia Clínica
          </button>
        </app-page-header>

        <mat-tab-group animationDuration="200ms">
          <!-- Datos personales -->
          <mat-tab label="Datos Personales">
            <div class="tab-content">
              <div class="info-grid">
                <mat-card class="info-card">
                  <mat-card-header><mat-card-title>Identificación</mat-card-title></mat-card-header>
                  <mat-card-content>
                    <div class="info-row"><span class="label">Cédula / Doc.</span><span>{{ patient.idDocument }}</span></div>
                    <div class="info-row"><span class="label">Fecha Nac.</span><span>{{ patient.birthDate | date:'dd/MM/yyyy' }}</span></div>
                    <div class="info-row"><span class="label">Sexo</span><span>{{ patient.sex }}</span></div>
                    <div class="info-row"><span class="label">Género</span><span>{{ patient.gender }}</span></div>
                    @if (patient.socialName) {
                      <div class="info-row"><span class="label">Nombre Social</span><span>{{ patient.socialName }}</span></div>
                    }
                  </mat-card-content>
                </mat-card>

                <mat-card class="info-card">
                  <mat-card-header><mat-card-title>Contacto</mat-card-title></mat-card-header>
                  <mat-card-content>
                    <div class="info-row"><span class="label">Email</span><span>{{ patient.contactInfo.email }}</span></div>
                    <div class="info-row"><span class="label">Teléfono</span><span>{{ patient.contactInfo.phoneNumber }}</span></div>
                    @if (patient.contactInfo.alternativePhone) {
                      <div class="info-row"><span class="label">Alt. Tel.</span><span>{{ patient.contactInfo.alternativePhone }}</span></div>
                    }
                  </mat-card-content>
                </mat-card>

                @if (patient.address) {
                  <mat-card class="info-card">
                    <mat-card-header><mat-card-title>Dirección</mat-card-title></mat-card-header>
                    <mat-card-content>
                      <div class="info-row"><span class="label">Calle</span><span>{{ patient.address.street }}</span></div>
                      <div class="info-row"><span class="label">Ciudad</span><span>{{ patient.address.city }}</span></div>
                      <div class="info-row"><span class="label">Estado</span><span>{{ patient.address.state }}</span></div>
                    </mat-card-content>
                  </mat-card>
                }

                @if (patient.representative) {
                  <mat-card class="info-card">
                    <mat-card-header><mat-card-title>Representante</mat-card-title></mat-card-header>
                    <mat-card-content>
                      <div class="info-row"><span class="label">Nombre</span><span>{{ patient.representative.fullName }}</span></div>
                      <div class="info-row"><span class="label">Parentesco</span><span>{{ patient.representative.relationship }}</span></div>
                      <div class="info-row"><span class="label">Teléfono</span><span>{{ patient.representative.phoneNumber }}</span></div>
                    </mat-card-content>
                  </mat-card>
                }
              </div>
            </div>
          </mat-tab>

          <!-- Citas -->
          <mat-tab label="Historial de Citas">
            <div class="tab-content">
              @for (apt of appointments; track apt.id) {
                <mat-card class="apt-card">
                  <mat-card-content>
                    <div class="flex-row">
                      <div>
                        <div class="apt-date">{{ apt.startDateTime | date:'dd/MM/yyyy HH:mm' }}</div>
                        <div class="apt-reason">{{ apt.consultationReason }}</div>
                        <div class="apt-dentist">Dr. {{ apt.dentistName }}</div>
                      </div>
                      <span class="spacer"></span>
                      <span class="status-chip" [class]="'status-' + apt.status.toLowerCase()">{{ apt.status }}</span>
                    </div>
                  </mat-card-content>
                </mat-card>
              } @empty {
                <p class="empty-msg">Sin citas registradas</p>
              }
            </div>
          </mat-tab>
        </mat-tab-group>
      } @else {
        <p>Cargando...</p>
      }
    </div>
  `,
  styles: [`
    .tab-content { padding: 24px 0; }
    .info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; }
    .info-card mat-card-content { padding-top: 8px; }
    .info-row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid var(--dentis-border); font-size: 14px; }
    .label { color: var(--dentis-text-muted); font-weight: 500; }
    .apt-card { margin-bottom: 12px; }
    .apt-date { font-weight: 600; font-size: 15px; }
    .apt-reason { color: var(--dentis-text-muted); margin-top: 2px; }
    .apt-dentist { font-size: 12px; color: var(--dentis-text-muted); margin-top: 2px; }
    .empty-msg { color: var(--dentis-text-muted); text-align: center; padding: 40px; }
  `]
})
export class PatientDetailComponent implements OnInit {
  id = input<string>('');
  patient: Patient | null = null;
  appointments: Appointment[] = [];

  constructor(
    private readonly patientService: PatientService,
    private readonly appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.patientService.getById(id).subscribe((p) => (this.patient = p));
      this.appointmentService.getByPatient(id).subscribe((list) => (this.appointments = list));
    }
  }
}

