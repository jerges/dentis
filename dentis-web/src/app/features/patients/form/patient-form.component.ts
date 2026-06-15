import { Component, OnInit, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatStepperModule } from '@angular/material/stepper';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { PatientService } from '../../../core/services/patient.service';
import { CreatePatientRequest, Sex, Gender, UpdatePatientRequest } from '../../../core/models/patient.model';

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatStepperModule,
    MatSnackBarModule, MatDatepickerModule, MatNativeDateModule, MatCheckboxModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <button mat-icon-button routerLink="/patients"><mat-icon>arrow_back</mat-icon></button>
        <h1 class="page-title">{{ isEdit ? 'Editar Paciente' : 'Nuevo Paciente' }}</h1>
      </div>

      <mat-card>
        <mat-card-content>
          <mat-stepper [linear]="true" #stepper>
            <!-- Step 1: Datos personales -->
            <mat-step [stepControl]="personalForm" label="Datos Personales">
              <form [formGroup]="personalForm" class="step-form">
                <div class="form-grid">
                  <mat-form-field appearance="outline">
                    <mat-label>Nombre *</mat-label>
                    <input matInput formControlName="firstName" />
                    <mat-error>Obligatorio</mat-error>
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Apellido *</mat-label>
                    <input matInput formControlName="lastName" />
                    <mat-error>Obligatorio</mat-error>
                  </mat-form-field>
                  <div class="doc-field">
                    <mat-form-field appearance="outline" class="doc-type-select">
                      <mat-label>Tipo</mat-label>
                      <mat-select formControlName="docType">
                        <mat-option value="V">V - Venezolano</mat-option>
                        <mat-option value="E">E - Extranjero</mat-option>
                        <mat-option value="P">P - Pasaporte</mat-option>
                        <mat-option value="J">J - Jurídico</mat-option>
                      </mat-select>
                    </mat-form-field>
                    <mat-form-field appearance="outline" class="doc-number-input">
                      <mat-label>N° de Documento *</mat-label>
                      <span matTextPrefix>{{ personalForm.controls.docType.value }}-&nbsp;</span>
                      <input matInput formControlName="idDocument" [placeholder]="personalForm.controls.docType.value === 'P' ? 'AB123456' : '12345678'" />
                      <mat-error>Obligatorio</mat-error>
                    </mat-form-field>
                  </div>
                  <mat-form-field appearance="outline">
                    <mat-label>Fecha de Nacimiento *</mat-label>
                    <input matInput formControlName="birthDate" [matDatepicker]="picker" />
                    <mat-datepicker-toggle matSuffix [for]="picker" />
                    <mat-datepicker #picker />
                    <mat-error>Obligatorio</mat-error>
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Sexo *</mat-label>
                    <mat-select formControlName="sex">
                      <mat-option value="MALE">Masculino</mat-option>
                      <mat-option value="FEMALE">Femenino</mat-option>
                      <mat-option value="INTERSEX">Intersex</mat-option>
                      <mat-option value="NOT_SPECIFIED">No especificado</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Género</mat-label>
                    <mat-select formControlName="gender">
                      <mat-option value="MALE">Masculino</mat-option>
                      <mat-option value="FEMALE">Femenino</mat-option>
                      <mat-option value="NON_BINARY">No binario</mat-option>
                      <mat-option value="OTHER">Otro</mat-option>
                      <mat-option value="NOT_SPECIFIED">No especificado</mat-option>
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Nombre Social</mat-label>
                    <input matInput formControlName="socialName" />
                  </mat-form-field>
                </div>
                <div class="step-actions">
                  <button mat-raised-button color="primary" matStepperNext [disabled]="personalForm.invalid">
                    Siguiente <mat-icon>chevron_right</mat-icon>
                  </button>
                </div>
              </form>
            </mat-step>

            <!-- Step 2: Contacto -->
            <mat-step [stepControl]="contactForm" label="Contacto y Dirección">
              <form [formGroup]="contactForm" class="step-form">
                <div class="form-grid">
                  <mat-form-field appearance="outline">
                    <mat-label>Email *</mat-label>
                    <input matInput formControlName="email" type="email" />
                    <mat-error>Email válido requerido</mat-error>
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Teléfono *</mat-label>
                    <input matInput formControlName="phoneNumber" />
                    <mat-error>Obligatorio</mat-error>
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Teléfono Alternativo</mat-label>
                    <input matInput formControlName="alternativePhone" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Dirección</mat-label>
                    <input matInput formControlName="street" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Ciudad</mat-label>
                    <input matInput formControlName="city" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Estado</mat-label>
                    <input matInput formControlName="state" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Código Postal</mat-label>
                    <input matInput formControlName="zipCode" />
                  </mat-form-field>
                </div>
                <div class="step-actions">
                  <button mat-stroked-button matStepperPrevious>
                    <mat-icon>chevron_left</mat-icon> Anterior
                  </button>
                  <button mat-raised-button color="primary" matStepperNext [disabled]="contactForm.invalid">
                    Siguiente <mat-icon>chevron_right</mat-icon>
                  </button>
                </div>
              </form>
            </mat-step>

            <!-- Step 3: Representante (opcional) -->
            <mat-step label="Representante (Opcional)">
              <form [formGroup]="representativeForm" class="step-form">
                <mat-checkbox [(ngModel)]="hasRepresentative" [ngModelOptions]="{standalone: true}"
                              class="rep-toggle">
                  El paciente tiene representante o responsable
                </mat-checkbox>
                @if (hasRepresentative) {
                  <div class="form-grid rep-fields">
                    <mat-form-field appearance="outline">
                      <mat-label>Nombre completo</mat-label>
                      <input matInput formControlName="representativeName" />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Parentesco</mat-label>
                      <input matInput formControlName="representativeRelationship" />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Teléfono</mat-label>
                      <input matInput formControlName="representativePhone" />
                    </mat-form-field>
                    <mat-form-field appearance="outline">
                      <mat-label>Email</mat-label>
                      <input matInput formControlName="representativeEmail" type="email" />
                    </mat-form-field>
                  </div>
                }
                <div class="step-actions">
                  <button mat-stroked-button matStepperPrevious>
                    <mat-icon>chevron_left</mat-icon> Anterior
                  </button>
                  <button mat-raised-button color="primary" (click)="onSubmit()" [disabled]="loading">
                    <mat-icon>{{ isEdit ? 'save' : 'person_add' }}</mat-icon>
                    {{ isEdit ? 'Guardar Cambios' : 'Registrar Paciente' }}
                  </button>
                </div>
              </form>
            </mat-step>
          </mat-stepper>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-header { gap: 12px; margin-bottom: 24px; align-items: center; }
    .page-title { margin: 0; font-size: 22px; font-weight: 700; }
    .step-form { padding: 24px 0; }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; }
    .step-actions { display: flex; gap: 12px; margin-top: 24px; justify-content: flex-end; }
    .doc-field { display: flex; gap: 12px; align-items: flex-start; grid-column: span 1; }
    .doc-type-select { width: 160px; flex-shrink: 0; }
    .doc-number-input { flex: 1; }
    .rep-toggle { margin-bottom: 20px; display: block; }
    .rep-fields { margin-top: 16px; }
  `]
})
export class PatientFormComponent implements OnInit {
  id = input<string>('');
  isEdit = false;
  loading = false;
  hasRepresentative = false;

  personalForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    docType: ['V', Validators.required],
    idDocument: ['', Validators.required],
    birthDate: ['', Validators.required],
    sex: ['NOT_SPECIFIED', Validators.required],
    gender: ['NOT_SPECIFIED'],
    socialName: ['']
  });

  contactForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', Validators.required],
    alternativePhone: [''],
    street: [''],
    city: [''],
    state: [''],
    zipCode: ['']
  });

  representativeForm = this.fb.group({
    representativeName: [''],
    representativeRelationship: [''],
    representativePhone: [''],
    representativeEmail: ['']
  });

  constructor(
    private fb: FormBuilder,
    private patientService: PatientService,
    private router: Router,
    private snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.isEdit = true;
      this.patientService.getById(id).subscribe((p) => {
        const [docPrefix, ...docRest] = (p.idDocument ?? '').split('-');
        const docType = ['V', 'E', 'J', 'P'].includes(docPrefix) ? docPrefix : 'V';
        const docNumber = docRest.length ? docRest.join('-') : p.idDocument ?? '';
        this.personalForm.patchValue({
          firstName: p.firstName, lastName: p.lastName,
          docType, idDocument: docNumber, birthDate: p.birthDate,
          sex: p.sex, gender: p.gender, socialName: p.socialName
        });
        this.hasRepresentative = !!p.representative?.fullName;
        this.contactForm.patchValue({
          email: p.contactInfo?.email, phoneNumber: p.contactInfo?.phoneNumber,
          alternativePhone: p.contactInfo?.alternativePhone,
          street: p.address?.street, city: p.address?.city, state: p.address?.state, zipCode: p.address?.zipCode
        });
        this.representativeForm.patchValue({
          representativeName: p.representative?.fullName,
          representativeRelationship: p.representative?.relationship,
          representativePhone: p.representative?.phoneNumber
        });
      });
    }
  }

  onSubmit(): void {
    if (this.personalForm.invalid || this.contactForm.invalid) {
      this.personalForm.markAllAsTouched();
      this.contactForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const personal = this.personalForm.getRawValue();
    const contact = this.contactForm.getRawValue();
    const representative = this.representativeForm.getRawValue();

    const normalizedBirthDate = this.formatBirthDate(personal.birthDate);
    const fullIdDocument = `${personal.docType ?? 'V'}-${personal.idDocument ?? ''}`;
    const representativePayload = this.hasRepresentative
      ? this.buildRepresentativePayload(representative)
      : undefined;

    const basePayload = {
      firstName: personal.firstName ?? '',
      lastName: personal.lastName ?? '',
      birthDate: normalizedBirthDate,
      sex: (personal.sex ?? 'NOT_SPECIFIED') as Sex,
      gender: (personal.gender ?? 'NOT_SPECIFIED') as Gender,
      socialName: personal.socialName ?? undefined,
      contactInfo: {
        email: contact.email ?? undefined,
        phoneNumber: contact.phoneNumber ?? undefined,
        alternativePhone: contact.alternativePhone ?? undefined
      },
      address: {
        street: contact.street ?? undefined,
        city: contact.city ?? undefined,
        state: contact.state ?? undefined,
        zipCode: contact.zipCode ?? undefined
      },
      representative: representativePayload
    };

    const op$ = this.isEdit
      ? this.patientService.update(this.id(), {
          firstName: basePayload.firstName,
          lastName: basePayload.lastName,
          birthDate: basePayload.birthDate,
          sex: basePayload.sex,
          gender: basePayload.gender,
          socialName: basePayload.socialName,
          contactInfo: basePayload.contactInfo,
          address: basePayload.address,
          representative: basePayload.representative
        } as UpdatePatientRequest)
      : this.patientService.create({
          ...basePayload,
          idDocument: fullIdDocument
        } as CreatePatientRequest);

    op$.subscribe({
      next: (p) => {
        this.snack.open(this.isEdit ? 'Paciente actualizado' : 'Paciente registrado', 'OK', { duration: 3000 });
        this.router.navigate(['/patients', p.id]);
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private formatBirthDate(value: Date | string | null): string {
    if (!value) {
      return '';
    }

    if (typeof value === 'string') {
      return value.slice(0, 10);
    }

    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '' : date.toISOString().slice(0, 10);
  }

  private buildRepresentativePayload(value: {
    representativeName: string | null;
    representativeRelationship: string | null;
    representativePhone: string | null;
    representativeEmail: string | null;
  }): UpdatePatientRequest['representative'] {
    const hasRepresentative = !!(
      value.representativeName ||
      value.representativeRelationship ||
      value.representativePhone
    );

    if (!hasRepresentative) {
      return undefined;
    }

    return {
      fullName: value.representativeName ?? undefined,
      relationship: value.representativeRelationship ?? undefined,
      phoneNumber: value.representativePhone ?? undefined
    };
  }
}

