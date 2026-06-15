import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, debounceTime, distinctUntilChanged, map, Observable, of, startWith, switchMap, tap } from 'rxjs';
import { BillingService } from '../../../core/services/billing.service';
import { Budget, Payment, PaymentMethod } from '../../../core/models/billing.model';
import { Patient } from '../../../core/models/patient.model';
import { PatientService } from '../../../core/services/patient.service';
import { AutocompleteOption, EntityAutocompleteComponent } from '../../../shared/components/entity-autocomplete/entity-autocomplete.component';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatAutocompleteModule, MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatDialogModule, MatSnackBarModule, EntityAutocompleteComponent
  ],
  template: `
    <div class="page-container">
      <div class="page-header flex-row">
        <div>
          <h1 class="page-title">Pagos</h1>
          <p class="page-subtitle">Registro de pagos recibidos</p>
        </div>
        <span class="spacer"></span>
        <button mat-raised-button color="primary" type="button" (click)="toggleForm()">
          <mat-icon>{{ showForm ? 'close' : 'add' }}</mat-icon>
          {{ showForm ? 'Cancelar' : 'Registrar Pago' }}
        </button>
      </div>

      <!-- Payment form -->
      @if (showForm) {
        <mat-card class="form-card">
          <mat-card-header><mat-card-title>Nuevo Pago</mat-card-title></mat-card-header>
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
                [label]="'Presupuesto *'"
                [placeholder]="'Select a patient first'"
                [control]="budgetSearchControl"
                [options]="filteredBudgets()"
                [displayWith]="displayBudget"
                [trackByValue]="trackBudget"
                [disabled]="!form.controls.patientId.value"
                [showEmptyState]="!!form.controls.patientId.value"
                [emptyMessage]="'No budgets available for the selected patient'"
                [errorMessage]="'Select a budget from the list'"
                (optionSelected)="onBudgetSelected($event)" />
              <mat-form-field appearance="outline">
                <mat-label>Monto *</mat-label>
                <input matInput formControlName="amount" type="number" min="0" />
                <span matPrefix>$ </span>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Método de pago *</mat-label>
                <mat-select formControlName="paymentMethod">
                  <mat-option value="CASH">Efectivo</mat-option>
                  <mat-option value="BANK_TRANSFER">Transferencia</mat-option>
                  <mat-option value="CREDIT_CARD">Tarjeta Crédito</mat-option>
                  <mat-option value="DEBIT_CARD">Tarjeta Débito</mat-option>
                  <mat-option value="MOBILE_PAYMENT">Pago móvil</mat-option>
                  <mat-option value="CRYPTOCURRENCY">Criptomoneda</mat-option>
                  <mat-option value="OTHER">Otro</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Referencia</mat-label>
                <input matInput formControlName="invoiceReference" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Notas</mat-label>
                <input matInput formControlName="notes" />
              </mat-form-field>
              <div class="form-actions full-span">
                <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
                  <mat-icon>payments</mat-icon> Registrar
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }

      <!-- Payments table -->
      <mat-card>
        <mat-card-content>
          <table mat-table [dataSource]="dataSource" class="w-full">
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef>Fecha</th>
              <td mat-cell *matCellDef="let p">{{ p.paidAt | date:'dd/MM/yyyy HH:mm' }}</td>
            </ng-container>
            <ng-container matColumnDef="amount">
              <th mat-header-cell *matHeaderCellDef>Monto</th>
              <td mat-cell *matCellDef="let p" class="amount">{{ p.amount | currency:'USD' }}</td>
            </ng-container>
            <ng-container matColumnDef="method">
              <th mat-header-cell *matHeaderCellDef>Método</th>
              <td mat-cell *matCellDef="let p">{{ methodLabel(p.paymentMethod) }}</td>
            </ng-container>
            <ng-container matColumnDef="reference">
              <th mat-header-cell *matHeaderCellDef>Referencia</th>
              <td mat-cell *matCellDef="let p">{{ p.invoiceReference ?? '—' }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let r; columns: cols;"></tr>
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell" colspan="4" style="text-align:center;padding:40px;color:#999">
                Sin pagos registrados
              </td>
            </tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-header { margin-bottom: 24px; }
    .form-card { margin-bottom: 24px; max-width: 860px; }
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
    .full-span { grid-column: 1 / -1; }
    .form-actions { display: flex; justify-content: flex-end; }
    .w-full { width: 100%; }
    .amount { font-weight: 700; color: var(--dentis-success); }
    @media (max-width: 900px) {
      .form-grid { grid-template-columns: 1fr; }
    }
  `]
})
export class PaymentsComponent {
  cols = ['date', 'amount', 'method', 'reference'];
  dataSource = new MatTableDataSource<Payment>([]);
  showForm = false;
  loading = false;
  budgets: Budget[] = [];

  form = this.fb.group({
    budgetId: ['', Validators.required],
    patientId: ['', Validators.required],
    amount: [null, [Validators.required, Validators.min(0.01)]],
    paymentMethod: ['CASH', Validators.required],
    invoiceReference: ['', Validators.maxLength(100)],
    notes: ['']
  });

  readonly patientSearchControl = this.fb.control<string | Patient>('');
  readonly budgetSearchControl = this.fb.control<string | Budget>({ value: '', disabled: true });

  readonly filteredPatients$: Observable<Patient[]> = this.patientSearchControl.valueChanges.pipe(
    startWith(''),
    debounceTime(250),
    distinctUntilChanged(),
    tap((value) => {
      if (typeof value === 'string') {
        this.form.controls.patientId.setValue('');
        this.form.controls.budgetId.setValue('');
        this.budgetSearchControl.reset('');
        this.budgetSearchControl.disable({ emitEvent: false });
        this.budgets = [];
      }
    }),
    switchMap((value) => this.searchPatients(this.searchTerm(value), value))
  );

  readonly filteredBudgets$: Observable<Budget[]> = this.budgetSearchControl.valueChanges.pipe(
    startWith(''),
    tap((value) => {
      if (typeof value === 'string') {
        this.form.controls.budgetId.setValue('');
      }
    }),
    map((value) => this.filterBudgets(this.searchTerm(value), value))
  );

  readonly filteredPatients = toSignal(this.filteredPatients$, { initialValue: [] as Patient[] });

  readonly filteredBudgets = toSignal(this.filteredBudgets$, { initialValue: [] as Budget[] });


  constructor(
    private readonly fb: FormBuilder,
    private readonly billingService: BillingService,
    private readonly patientService: PatientService,
    private readonly snack: MatSnackBar
  ) {}

  toggleForm(): void {
    this.showForm = !this.showForm;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.patientSearchControl.markAsTouched();
      this.budgetSearchControl.markAsTouched();
      this.snack.open('Revisa los campos obligatorios del formulario', 'OK', { duration: 3000 });
      return;
    }

    const selectedBudget = this.budgets.find((budget) => budget.id === this.form.controls.budgetId.value);
    if (!selectedBudget || selectedBudget.status !== 'APPROVED') {
      this.snack.open('Selecciona un presupuesto aprobado para registrar el pago', 'OK', { duration: 4000 });
      return;
    }

    this.loading = true;
    const val = this.form.getRawValue();
    const request: Partial<Payment> = {
      budgetId: val.budgetId ?? '',
      patientId: val.patientId ?? '',
      amount: Number(val.amount),
      paymentMethod: (val.paymentMethod as PaymentMethod) ?? 'CASH',
      invoiceReference: val.invoiceReference || undefined,
      notes: val.notes || undefined
    };
    this.billingService.registerPayment(request).subscribe({
      next: (p) => {
        this.dataSource.data = [p, ...this.dataSource.data];
        this.snack.open('Pago registrado', 'OK', { duration: 3000 });
        const patientId = this.form.controls.patientId.value;
        this.form.reset({ paymentMethod: 'CASH' });
        this.patientSearchControl.reset('');
        this.budgetSearchControl.reset('');
        this.budgetSearchControl.disable({ emitEvent: false });
        this.budgets = [];
        this.showForm = false;
        if (patientId) {
          this.loadPaymentsByPatient(patientId);
        }
        this.loading = false;
      },
      error: () => {
        this.snack.open('Error al registrar el pago', 'OK', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  displayPatient = (value: AutocompleteOption | null): string => {
    if (!value || typeof value === 'string') return value ?? '';
    const patient = value as Patient;
    return `${patient.firstName} ${patient.lastName} · ${patient.idDocument}`;
  };

  displayBudget = (value: AutocompleteOption | null): string => {
    if (!value || typeof value === 'string') return value ?? '';
    const budget = value as Budget;
    const amount = this.calculateBudgetTotal(budget);
    return `${budget.status} · ${budget.createdAt?.slice(0, 10) ?? 'No date'} · ${amount.toFixed(2)} USD`;
  };

  onPatientSelected(value: AutocompleteOption): void {
    const patient = value as Patient;
    this.form.controls.patientId.setValue(patient.id);
    this.form.controls.budgetId.setValue('');
    this.budgetSearchControl.reset('');
    this.loadBudgets(patient.id);
    this.loadPaymentsByPatient(patient.id);
  }

  onBudgetSelected(value: AutocompleteOption): void {
    this.form.controls.budgetId.setValue((value as Budget).id);
  }

  trackPatient = (value: AutocompleteOption): string => (value as Patient).id;

  trackBudget = (value: AutocompleteOption): string => (value as Budget).id;

  searchTerm(value: string | Patient | Budget | null): string {
    if (typeof value === 'string') {
      return value.trim();
    }

    if (!value) {
      return '';
    }

    return 'firstName' in value ? this.displayPatient(value) : this.displayBudget(value);
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

  private loadBudgets(patientId: string): void {
    this.billingService.getBudgetsByPatient(patientId, 0, 20).subscribe({
      next: (page) => {
        this.budgets = (page.content ?? []).filter((budget) => budget.status === 'APPROVED');
        this.budgetSearchControl.enable({ emitEvent: false });
        if (!this.budgets.length) {
          this.snack.open('El paciente no tiene presupuestos aprobados', 'OK', { duration: 3500 });
        }
      },
      error: () => {
        this.budgets = [];
        this.budgetSearchControl.disable({ emitEvent: false });
        this.snack.open('No se pudieron cargar los presupuestos del paciente', 'OK', { duration: 4000 });
      }
    });
  }

  private loadPaymentsByPatient(patientId: string): void {
    this.billingService.getPaymentsByPatient(patientId).subscribe({
      next: (payments) => {
        const sorted = [...payments].sort((left, right) =>
          new Date(right.paidAt).getTime() - new Date(left.paidAt).getTime()
        );
        this.dataSource.data = sorted;
      },
      error: () => {
        this.dataSource.data = [];
        this.snack.open('No se pudieron cargar los pagos del paciente', 'OK', { duration: 3500 });
      }
    });
  }

  private filterBudgets(term: string, value: string | Budget | null): Budget[] {
    if (value && typeof value !== 'string') {
      return [value];
    }

    if (!term) {
      return this.budgets.slice(0, 10);
    }

    const normalizedTerm = term.toLowerCase();
    return this.budgets
      .filter((budget) => {
        const label = this.displayBudget(budget).toLowerCase();
        return label.includes(normalizedTerm) || budget.id.toLowerCase().includes(normalizedTerm);
      })
      .slice(0, 10);
  }

  private calculateBudgetTotal(budget: Budget): number {
    if (typeof budget.totalAmount === 'number') {
      return budget.totalAmount;
    }

    return (budget.items ?? []).reduce((sum, item) => {
      const unitPrice = Number(item.unitPrice ?? 0);
      const quantity = Number(item.quantity ?? 0);
      const baseTotal = typeof item.total === 'number' ? item.total : unitPrice * quantity;
      const discount = Number(item.discountPercentage ?? 0);
      return sum + baseTotal - baseTotal * (discount / 100);
    }, 0);
  }

  methodLabel(method: string): string {
    const map: Record<string, string> = {
      CASH: 'Efectivo', BANK_TRANSFER: 'Transferencia',
      CREDIT_CARD: 'T. Crédito', DEBIT_CARD: 'T. Débito', MOBILE_PAYMENT: 'Pago móvil',
      CRYPTOCURRENCY: 'Cripto', OTHER: 'Otro'
    };
    return map[method] ?? method;
  }
}

