import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, debounceTime, distinctUntilChanged, forkJoin, map, Observable, of, startWith, switchMap, tap } from 'rxjs';
import { BillingService } from '../../../core/services/billing.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClinicService } from '../../../core/services/clinic.service';
import { PatientService } from '../../../core/services/patient.service';
import { Budget, BudgetItem, CreateBudgetRequest, Tariff } from '../../../core/models/billing.model';
import { Clinic, ClinicUser } from '../../../core/models/clinic.model';
import { Patient } from '../../../core/models/patient.model';
import { AutocompleteOption, EntityAutocompleteComponent } from '../../../shared/components/entity-autocomplete/entity-autocomplete.component';

@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule,
    MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatChipsModule, MatSnackBarModule, MatTooltipModule,
    MatFormFieldModule, MatInputModule, EntityAutocompleteComponent
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <div>
          <h1 class="page-title">Presupuestos</h1>
          <p class="page-subtitle">Gestión de presupuestos de tratamiento</p>
        </div>
        <span class="spacer"></span>
        <button mat-raised-button color="primary" (click)="showForm = !showForm">
          <mat-icon>{{ showForm ? 'close' : 'add' }}</mat-icon>
          {{ showForm ? 'Cancelar' : 'Nuevo Presupuesto' }}
        </button>
      </div>

      @if (showForm) {
        <mat-card class="form-card">
          <mat-card-content>
            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="form-grid">
              <app-entity-autocomplete
                [label]="'Paciente *'"
                [placeholder]="'Busca por nombre o documento'"
                [control]="patientSearchControl"
                [options]="filteredPatients()"
                [displayWith]="displayPatient"
                [trackByValue]="trackPatient"
                [showEmptyState]="searchTerm(patientSearchControl.value).length >= 2"
                [emptyMessage]="'No patients found'"
                [errorMessage]="'Select a patient from the list'"
                (optionSelected)="onPatientSelected($event)" />

              <app-entity-autocomplete
                [label]="'Dentista *'"
                [placeholder]="'Selecciona profesional'"
                [control]="dentistSearchControl"
                [options]="filteredDentists()"
                [displayWith]="displayDentist"
                [trackByValue]="trackDentist"
                [showEmptyState]="searchTerm(dentistSearchControl.value).length >= 1"
                [emptyMessage]="'No dentists found'"
                [errorMessage]="'Select a dentist from the list'"
                (optionSelected)="onDentistSelected($event)" />

              <app-entity-autocomplete
                [label]="'Arancel *'"
                [placeholder]="'Selecciona arancel'"
                [control]="tariffSearchControl"
                [options]="filteredTariffs()"
                [displayWith]="displayTariff"
                [trackByValue]="trackTariff"
                [showEmptyState]="searchTerm(tariffSearchControl.value).length >= 1"
                [emptyMessage]="'No tariffs found'"
                [errorMessage]="'Select a tariff from the list'"
                (optionSelected)="onTariffSelected($event)" />

              <mat-form-field appearance="outline">
                <mat-label>Cantidad *</mat-label>
                <input matInput type="number" min="1" formControlName="quantity" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Precio unitario *</mat-label>
                <input matInput type="number" min="0.01" step="0.01" formControlName="unitPrice" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Descuento (%)</mat-label>
                <input matInput type="number" min="0" max="100" step="0.01" formControlName="discountPercentage" />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-span">
                <mat-label>Notas</mat-label>
                <input matInput formControlName="notes" />
              </mat-form-field>

              <div class="actions full-span">
                <button mat-stroked-button type="button" (click)="showForm = false">Cancelar</button>
                <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
                  <mat-icon>save</mat-icon>
                  Guardar presupuesto
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }

      <mat-card>
        <mat-card-content>
          <table mat-table [dataSource]="dataSource" class="w-full">
            <ng-container matColumnDef="patient">
              <th mat-header-cell *matHeaderCellDef>Paciente</th>
              <td mat-cell *matCellDef="let b">{{ b.patientName ?? b.patientId }}</td>
            </ng-container>
            <ng-container matColumnDef="dentist">
              <th mat-header-cell *matHeaderCellDef>Dentista</th>
              <td mat-cell *matCellDef="let b">Dr. {{ b.dentistName ?? b.dentistId }}</td>
            </ng-container>
            <ng-container matColumnDef="total">
              <th mat-header-cell *matHeaderCellDef>Total</th>
              <td mat-cell *matCellDef="let b" class="amount">{{ calculateBudgetTotal(b) | currency:'USD' }}</td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let b">
                <span class="status-chip" [class]="'status-' + b.status.toLowerCase()">{{ b.status }}</span>
              </td>
            </ng-container>
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef>Fecha</th>
              <td mat-cell *matCellDef="let b">{{ b.createdAt | date:'dd/MM/yyyy' }}</td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Acciones</th>
              <td mat-cell *matCellDef="let b">
                <button mat-icon-button matTooltip="Aprobar" color="primary"
                        (click)="approveBudget(b.id)" [disabled]="b.status !== 'PRESENTED' && b.status !== 'DRAFT'">
                  <mat-icon>check_circle</mat-icon>
                </button>
                <button mat-icon-button matTooltip="Ver resumen" (click)="viewSummary(b.id)">
                  <mat-icon>summarize</mat-icon>
                </button>
                <button mat-icon-button matTooltip="Ver procedimientos" color="accent"
                        (click)="toggleItems(b)">
                  <mat-icon>{{ selectedBudget?.id === b.id ? 'expand_less' : 'list_alt' }}</mat-icon>
                </button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let r; columns: cols;" class="table-row"></tr>
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell" colspan="6" style="text-align:center;padding:40px;color:#999">
                Sin presupuestos registrados
              </td>
            </tr>
          </table>
        </mat-card-content>
      </mat-card>

      @if (selectedBudget) {
        <mat-card class="items-card">
          <mat-card-header>
            <mat-card-title>Procedimientos del presupuesto</mat-card-title>
            <span class="spacer"></span>
            <button mat-icon-button (click)="selectedBudget = null"><mat-icon>close</mat-icon></button>
          </mat-card-header>
          <mat-card-content>
            @if (!selectedBudget.items?.length) {
              <p style="color:#999;padding:16px 0;text-align:center">Sin procedimientos</p>
            } @else {
              <table mat-table [dataSource]="selectedBudget.items" class="w-full items-table">
                <ng-container matColumnDef="description">
                  <th mat-header-cell *matHeaderCellDef>Procedimiento</th>
                  <td mat-cell *matCellDef="let item">{{ item.description || item.tariffId }}</td>
                </ng-container>
                <ng-container matColumnDef="qty">
                  <th mat-header-cell *matHeaderCellDef>Cant.</th>
                  <td mat-cell *matCellDef="let item">{{ item.quantity }}</td>
                </ng-container>
                <ng-container matColumnDef="price">
                  <th mat-header-cell *matHeaderCellDef>Neto</th>
                  <td mat-cell *matCellDef="let item" class="amount">
                    {{ itemNet(item) | currency:'USD' }}
                  </td>
                </ng-container>
                <ng-container matColumnDef="paymentStatus">
                  <th mat-header-cell *matHeaderCellDef>Pago</th>
                  <td mat-cell *matCellDef="let item">
                    <span class="status-chip" [class]="'status-' + (item.paymentStatus ?? 'pending').toLowerCase()">
                      {{ item.paymentStatus ?? 'PENDING' }}
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="performed">
                  <th mat-header-cell *matHeaderCellDef>Realizado</th>
                  <td mat-cell *matCellDef="let item">
                    @if (item.performed) {
                      <span class="performed-badge">
                        <mat-icon color="primary">check_circle</mat-icon>
                        {{ item.performedAt ? (item.performedAt | date:'dd/MM/yyyy') : '' }}
                      </span>
                    } @else {
                      <button mat-stroked-button color="primary" [disabled]="marking"
                              (click)="markItemPerformed(selectedBudget!.id, item.id)">
                        <mat-icon>check</mat-icon> Marcar realizado
                      </button>
                    }
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="itemCols"></tr>
                <tr mat-row *matRowDef="let r; columns: itemCols;"></tr>
              </table>
            }
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 24px; }
    .form-card { margin-bottom: 16px; }
    .form-grid { display:grid; grid-template-columns: repeat(2, minmax(220px, 1fr)); gap: 12px; }
    .full-span { grid-column: 1 / -1; }
    .actions { display:flex; justify-content:flex-end; gap: 10px; }
    .w-full { width: 100%; }
    .amount { font-weight: 700; color: var(--dentis-success); }
    .table-row:hover { background: rgba(13, 148, 136, 0.04); cursor: pointer; }
    .items-card { margin-top: 16px; }
    .items-table { width: 100%; }
    .performed-badge { display:flex; align-items:center; gap:4px; font-size:13px; color: var(--dentis-success); }
    @media (max-width: 900px) {
      .form-grid { grid-template-columns: 1fr; }
      .actions { justify-content: stretch; }
    }
  `]
})
export class BudgetsComponent implements OnInit {
  cols = ['patient', 'dentist', 'total', 'status', 'date', 'actions'];
  itemCols = ['description', 'qty', 'price', 'paymentStatus', 'performed'];
  dataSource = new MatTableDataSource<Budget>([]);
  showForm = false;
  loading = false;
  marking = false;
  selectedBudget: Budget | null = null;
  dentists: ClinicUser[] = [];
  tariffs: Tariff[] = [];

  readonly patientSearchControl = this.fb.control<string | Patient>('');
  readonly dentistSearchControl = this.fb.control<string | ClinicUser>('');
  readonly tariffSearchControl = this.fb.control<string | Tariff>('');

  readonly filteredPatients$ = this.patientSearchControl.valueChanges.pipe(
    startWith(''),
    debounceTime(250),
    distinctUntilChanged(),
    tap((value) => {
      if (typeof value === 'string') {
        this.form?.controls.patientId.setValue('');
      }
    }),
    switchMap((value) => this.searchPatients(this.searchTerm(value), value))
  );

  readonly filteredDentists$ = this.dentistSearchControl.valueChanges.pipe(
    startWith(''),
    tap((value) => {
      if (typeof value === 'string') {
        this.form?.controls.dentistId.setValue('');
      }
    }),
    map((value) => this.filterDentists(this.searchTerm(value), value))
  );

  readonly filteredTariffs$ = this.tariffSearchControl.valueChanges.pipe(
    startWith(''),
    tap((value) => {
      if (typeof value === 'string') {
        this.form?.controls.tariffId.setValue('');
      }
    }),
    map((value) => this.filterTariffs(this.searchTerm(value), value))
  );

  readonly filteredPatients = toSignal(this.filteredPatients$, { initialValue: [] as Patient[] });
  readonly filteredDentists = toSignal(this.filteredDentists$, { initialValue: [] as ClinicUser[] });
  readonly filteredTariffs = toSignal(this.filteredTariffs$, { initialValue: [] as Tariff[] });

  readonly form = this.fb.group({
    patientId: ['', Validators.required],
    dentistId: ['', Validators.required],
    tariffId: ['', Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    unitPrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    discountPercentage: [0],
    notes: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly billingService: BillingService,
    private readonly patientService: PatientService,
    private readonly clinicService: ClinicService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadDentists();
    this.loadTariffs();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.patientSearchControl.markAsTouched();
      this.dentistSearchControl.markAsTouched();
      this.tariffSearchControl.markAsTouched();
      this.snack.open('Revisa los campos obligatorios del formulario', 'OK', { duration: 3000 });
      return;
    }

    const raw = this.form.getRawValue();
    const selectedTariff = this.tariffs.find((tariff) => tariff.id === raw.tariffId);
    const payload: CreateBudgetRequest = {
      patientId: raw.patientId ?? '',
      dentistId: raw.dentistId ?? '',
      notes: raw.notes ?? undefined,
      items: [
        {
          tariffId: raw.tariffId ?? '',
          description: selectedTariff?.name ?? 'Budget item',
          quantity: Number(raw.quantity ?? 1),
          unitPrice: Number(raw.unitPrice ?? 0),
          discountPercentage: Number(raw.discountPercentage ?? 0)
        }
      ]
    };

    this.loading = true;
    this.billingService.createBudget(payload).subscribe({
      next: (created) => {
        this.snack.open('Presupuesto creado', 'OK', { duration: 3000 });
        this.showForm = false;
        this.loading = false;
        this.loadBudgetsByPatient(created.patientId);
      },
      error: () => {
        this.snack.open('No se pudo crear el presupuesto', 'OK', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  onPatientSelected(value: AutocompleteOption): void {
    const patient = value as Patient;
    this.form.controls.patientId.setValue(patient.id);
    this.loadBudgetsByPatient(patient.id);
  }

  onDentistSelected(value: AutocompleteOption): void {
    this.form.controls.dentistId.setValue((value as ClinicUser).id);
  }

  onTariffSelected(value: AutocompleteOption): void {
    const tariff = value as Tariff;
    this.form.controls.tariffId.setValue(tariff.id);
    this.form.controls.unitPrice.setValue(tariff.basePrice);
  }

  displayPatient = (value: AutocompleteOption | null): string => {
    if (!value || typeof value === 'string') return value ?? '';
    const p = value as Patient;
    return `${p.firstName} ${p.lastName} · ${p.idDocument}`;
  };

  displayDentist = (value: AutocompleteOption | null): string => {
    if (!value || typeof value === 'string') return value ?? '';
    const d = value as ClinicUser;
    return `${d.fullName} · ${d.username}`;
  };

  displayTariff = (value: AutocompleteOption | null): string => {
    if (!value || typeof value === 'string') return value ?? '';
    const t = value as Tariff;
    return `${t.code} · ${t.name} · ${t.basePrice.toFixed(2)} USD`;
  };

  trackPatient = (value: AutocompleteOption): string => (value as Patient).id;
  trackDentist = (value: AutocompleteOption): string => (value as ClinicUser).id;
  trackTariff = (value: AutocompleteOption): string => (value as Tariff).id;

  searchTerm(value: string | Patient | ClinicUser | Tariff | null): string {
    if (typeof value === 'string') {
      return value.trim();
    }
    if (!value) {
      return '';
    }
    if ('firstName' in value) {
      return this.displayPatient(value);
    }
    if ('basePrice' in value) {
      return this.displayTariff(value);
    }
    return this.displayDentist(value);
  }

  approveBudget(id: string): void {
    this.billingService.approveBudget(id).subscribe({
      next: (b) => {
        const idx = this.dataSource.data.findIndex((x) => x.id === id);
        if (idx >= 0) { const data = [...this.dataSource.data]; data[idx] = b; this.dataSource.data = data; }
        this.snack.open('Presupuesto aprobado', 'OK', { duration: 3000 });
      },
      error: () => this.snack.open('Error al aprobar', 'OK', { duration: 3000 })
    });
  }

  viewSummary(id: string): void {
    this.billingService.getBudgetSummary(id).subscribe((s) =>
      this.snack.open(`Total: ${s.grandTotal} | Pagado: ${s.totalPaid} | Saldo: ${s.balance}`, 'OK', { duration: 5000 })
    );
  }

  toggleItems(budget: Budget): void {
    this.selectedBudget = this.selectedBudget?.id === budget.id ? null : budget;
  }

  markItemPerformed(budgetId: string, itemId: string): void {
    if (!itemId) return;
    this.marking = true;
    this.billingService.markItemPerformed(budgetId, itemId).subscribe({
      next: (updated) => {
        this.marking = false;
        this.selectedBudget = updated;
        const idx = this.dataSource.data.findIndex((b) => b.id === budgetId);
        if (idx >= 0) {
          const data = [...this.dataSource.data];
          data[idx] = updated;
          this.dataSource.data = data;
        }
        this.snack.open('Procedimiento marcado como realizado', 'OK', { duration: 3000 });
      },
      error: () => {
        this.marking = false;
        this.snack.open('Error al actualizar el procedimiento', 'OK', { duration: 3000 });
      }
    });
  }

  itemNet(item: BudgetItem): number {
    const base = Number(item.unitPrice ?? 0) * Number(item.quantity ?? 1);
    const discount = Number(item.discountPercentage ?? 0);
    return base - base * (discount / 100);
  }

  calculateBudgetTotal(budget: Budget): number {
    if (typeof budget.totalAmount === 'number') {
      return budget.totalAmount;
    }

    return (budget.items ?? []).reduce((sum, item) => {
      const base = Number(item.unitPrice ?? 0) * Number(item.quantity ?? 0);
      const discount = Number(item.discountPercentage ?? 0);
      return sum + base - base * (discount / 100);
    }, 0);
  }

  private searchPatients(term: string, value: string | Patient | null): Observable<Patient[]> {
    if (value && typeof value !== 'string') {
      return of([value]);
    }
    if (term.length < 2) {
      return of([]);
    }
    return this.patientService.search(term, 0, 10).pipe(
      map((page) => page.content),
      catchError(() => of([]))
    );
  }

  private filterDentists(term: string, value: string | ClinicUser | null): ClinicUser[] {
    if (value && typeof value !== 'string') {
      return [value];
    }
    if (!term) {
      return this.dentists.slice(0, 10);
    }
    const normalized = term.toLowerCase();
    return this.dentists
      .filter((dentist) => dentist.fullName.toLowerCase().includes(normalized) || dentist.username.toLowerCase().includes(normalized))
      .slice(0, 10);
  }

  private filterTariffs(term: string, value: string | Tariff | null): Tariff[] {
    if (value && typeof value !== 'string') {
      return [value];
    }
    if (!term) {
      return this.tariffs.slice(0, 10);
    }
    const normalized = term.toLowerCase();
    return this.tariffs
      .filter((tariff) => {
        const label = `${tariff.code} ${tariff.name} ${tariff.category}`.toLowerCase();
        return label.includes(normalized);
      })
      .slice(0, 10);
  }

  private loadBudgetsByPatient(patientId: string): void {
    this.billingService.getBudgetsByPatient(patientId, 0, 50).subscribe({
      next: (page) => {
        this.dataSource.data = page.content ?? [];
      },
      error: () => {
        this.dataSource.data = [];
      }
    });
  }

  private loadTariffs(): void {
    this.billingService.getTariffs(0, 200).subscribe({
      next: (page) => {
        this.tariffs = (page.content ?? []).filter((tariff) => tariff.active);
      },
      error: () => {
        this.tariffs = [];
        this.snack.open('No se pudieron cargar los aranceles', 'OK', { duration: 3000 });
      }
    });
  }

  private loadDentists(): void {
    this.getDentistSource().subscribe({
      next: (dentists) => {
        this.dentists = dentists;
      },
      error: () => {
        this.dentists = [];
        this.snack.open('No se pudieron cargar los dentistas', 'OK', { duration: 3000 });
      }
    });
  }

  private getDentistSource(): Observable<ClinicUser[]> {
    const clinicId = this.auth.currentUser()?.clinicId;
    if (clinicId) {
      return this.clinicService.getClinicUsers(clinicId).pipe(
        map((response) => this.extractActiveDentists(response.data ?? [])),
        catchError(() => of([]))
      );
    }

    return this.clinicService.getActiveClinics().pipe(
      switchMap((response) => {
        const clinics = response.data ?? [];
        if (!clinics.length) {
          return of([]);
        }

        return forkJoin(
          clinics.map((clinic: Clinic) =>
            this.clinicService.getClinicUsers(clinic.id).pipe(
              map((usersResponse) => usersResponse.data ?? []),
              catchError(() => of([]))
            )
          )
        ).pipe(
          map((usersByClinic: ClinicUser[][]) => usersByClinic.flat()),
          map((users) => this.extractActiveDentists(users))
        );
      }),
      catchError(() => of([]))
    );
  }

  private extractActiveDentists(users: ClinicUser[]): ClinicUser[] {
    const unique = new Map<string, ClinicUser>();
    users
      .filter((user) => user.active && user.staffType === 'DENTIST')
      .forEach((user) => unique.set(user.id, user));

    return Array.from(unique.values()).sort((left, right) => left.fullName.localeCompare(right.fullName));
  }
}

