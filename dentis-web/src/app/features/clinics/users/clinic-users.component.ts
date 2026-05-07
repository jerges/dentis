import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute} from '@angular/router';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatCardModule} from '@angular/material/card';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatTableDataSource, MatTableModule} from '@angular/material/table';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {AuthService} from '../../../core/services/auth.service';
import {ClinicService} from '../../../core/services/clinic.service';
import {
    ClinicUser,
    ClinicUserStaffType,
    CreateClinicUserRequest,
    UpdateClinicUserRequest
} from '../../../core/models/clinic.model';
import {getHttpErrorMessage} from '../../../core/utils/http-error.util';
import {ConfirmDialogComponent} from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import {LoadingSpinnerComponent} from '../../../shared/components/loading-spinner/loading-spinner.component';
import {PageHeaderComponent} from '../../../shared/components/page-header/page-header.component';
import {StatusBadgeComponent} from '../../../shared/components/status-badge/status-badge.component';

@Component({
    selector: 'app-clinic-users',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatCardModule,
        MatDialogModule,
        MatTableModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        PageHeaderComponent,
        LoadingSpinnerComponent,
        StatusBadgeComponent
    ],
    template: `
        <div class="page-container">
            <app-page-header
                    title="Equipo de la Clínica"
                    subtitle="Alta y gestión de dentistas y personal administrativo en una única pantalla"
                    [backRoute]="backRoute"/>

            @if (errorMessage) {
                <div class="error-alert">{{ errorMessage }}</div>
            }

            <mat-card class="mb-16">
                <mat-card-content>
                    <form [formGroup]="form" (ngSubmit)="saveUser()" class="user-form">
                        <mat-form-field appearance="outline">
                            <mat-label>Usuario</mat-label>
                            <input matInput formControlName="username"/>
                        </mat-form-field>

                        <mat-form-field appearance="outline">
                            <mat-label>Nombre completo</mat-label>
                            <input matInput formControlName="fullName"/>
                        </mat-form-field>

                        <mat-form-field appearance="outline">
                            <mat-label>Email</mat-label>
                            <input matInput formControlName="email"/>
                        </mat-form-field>

                        <mat-form-field appearance="outline">
                            <mat-label>{{ editingUserId ? 'Contraseña nueva (opcional)' : 'Contraseña' }}</mat-label>
                            <input matInput type="password" formControlName="password"/>
                        </mat-form-field>

                        <mat-form-field appearance="outline">
                            <mat-label>Tipo de personal</mat-label>
                            <mat-select formControlName="staffType">
                                <mat-option value="DENTIST">Dentista</mat-option>
                                <mat-option value="ADMINISTRATIVE">Personal administrativo</mat-option>
                            </mat-select>
                        </mat-form-field>

                        <mat-form-field appearance="outline">
                            <mat-label>Nivel de acceso</mat-label>
                            <mat-select formControlName="role">
                                <mat-option value="USER">Usuario</mat-option>
                                <mat-option value="ADMIN" [disabled]="form.value.staffType === 'DENTIST'">
                                    Administrador
                                </mat-option>
                            </mat-select>
                        </mat-form-field>

                        <div class="form-actions">
                            @if (editingUserId) {
                                <button mat-stroked-button type="button" (click)="cancelEdit()">
                                    <mat-icon>close</mat-icon>
                                    Cancelar edición
                                </button>
                            }

                            <button mat-raised-button color="primary" type="submit">
                                <mat-icon>{{ editingUserId ? 'save' : 'person_add' }}</mat-icon>
                                {{ editingUserId ? 'Guardar cambios' : 'Crear usuario' }}
                            </button>
                        </div>
                    </form>
                </mat-card-content>
            </mat-card>

            <mat-card>
                <mat-card-content>
                    @if (loading) {
                        <app-loading-spinner message="Cargando usuarios..."/>
                    } @else {
                        <table mat-table [dataSource]="dataSource" class="w-full">
                            <ng-container matColumnDef="username">
                                <th mat-header-cell *matHeaderCellDef>Usuario</th>
                                <td mat-cell *matCellDef="let u">{{ u.username }}</td>
                            </ng-container>

                            <ng-container matColumnDef="fullName">
                                <th mat-header-cell *matHeaderCellDef>Nombre</th>
                                <td mat-cell *matCellDef="let u">{{ u.fullName }}</td>
                            </ng-container>

                            <ng-container matColumnDef="email">
                                <th mat-header-cell *matHeaderCellDef>Email</th>
                                <td mat-cell *matCellDef="let u">{{ u.email }}</td>
                            </ng-container>

                            <ng-container matColumnDef="staffType">
                                <th mat-header-cell *matHeaderCellDef>Tipo</th>
                                <td mat-cell *matCellDef="let u">{{ getStaffTypeLabel(u.staffType) }}</td>
                            </ng-container>

                            <ng-container matColumnDef="role">
                                <th mat-header-cell *matHeaderCellDef>Acceso</th>
                                <td mat-cell *matCellDef="let u">{{ getRoleLabel(u.role) }}</td>
                            </ng-container>

                            <ng-container matColumnDef="status">
                                <th mat-header-cell *matHeaderCellDef>Estado</th>
                                <td mat-cell *matCellDef="let u">
                                    <app-status-badge [label]="u.active ? 'Activo' : 'Inactivo'"
                                                      [tone]="u.active ? 'active' : 'inactive'"/>
                                </td>
                            </ng-container>

                            <ng-container matColumnDef="actions">
                                <th mat-header-cell *matHeaderCellDef>Acciones</th>
                                <td mat-cell *matCellDef="let u">
                                    <button mat-icon-button [disabled]="!u.active" (click)="startEdit(u)">
                                        <mat-icon>edit</mat-icon>
                                    </button>
                                    <button mat-icon-button color="warn" [disabled]="!u.active"
                                            (click)="deactivateUser(u.id)">
                                        <mat-icon>person_off</mat-icon>
                                    </button>
                                </td>
                            </ng-container>

                            <tr mat-header-row *matHeaderRowDef="cols"></tr>
                            <tr mat-row *matRowDef="let _row; columns: cols;"></tr>
                        </table>
                    }
                </mat-card-content>
            </mat-card>
        </div>
    `,
    styles: [`
        .mb-16 {
            margin-bottom: 16px;
        }

        .w-full {
            width: 100%;
        }

        .user-form {
            display: grid;
            grid-template-columns: repeat(3, minmax(220px, 1fr));
            gap: 12px;
            align-items: start;
        }

        .form-actions {
            display: flex;
            gap: 12px;
            justify-content: flex-end;
            grid-column: 1 / -1;
        }
    `]
})
export class ClinicUsersComponent implements OnInit {
    cols = ['username', 'fullName', 'email', 'staffType', 'role', 'status', 'actions'];
    dataSource = new MatTableDataSource<ClinicUser>([]);
    clinicId = '';
    errorMessage = '';
    loading = false;
    editingUserId: string | null = null;
    backRoute = '/dashboard';

    readonly form = this.fb.group({
        username: ['', [Validators.required, Validators.maxLength(100)]],
        fullName: ['', [Validators.required, Validators.maxLength(200)]],
        email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
        password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
        staffType: ['DENTIST' as ClinicUserStaffType, Validators.required],
        role: ['USER', Validators.required]
    });

    constructor(
        private readonly route: ActivatedRoute,
        private readonly fb: FormBuilder,
        private readonly authService: AuthService,
        private readonly clinicService: ClinicService,
        private readonly dialog: MatDialog
    ) {
    }

    ngOnInit(): void {
        this.form.controls.staffType.valueChanges.subscribe((staffType) => {
            if (staffType === 'DENTIST' && this.form.controls.role.value !== 'USER') {
                this.form.controls.role.setValue('USER');
            }
        });

        const clinicIdFromRoute = this.route.snapshot.paramMap.get('clinicId');
        if (clinicIdFromRoute) {
            this.clinicId = clinicIdFromRoute;
            this.backRoute = '/clinics';
            this.loadUsers();
            return;
        }

        const clinicIdFromSession = this.authService.currentUser()?.clinicId;
        if (!clinicIdFromSession) {
            this.errorMessage = 'No se pudo determinar la clínica del usuario autenticado.';
            return;
        }

        this.clinicId = clinicIdFromSession;
        this.backRoute = '/dashboard';
        this.loadUsers();
    }

    loadUsers(): void {
        this.errorMessage = '';
        this.loading = true;
        this.clinicService.getClinicUsers(this.clinicId).subscribe({
            next: (res) => {
                this.dataSource.data = res.data ?? [];
                this.loading = false;
            },
            error: () => {
                this.errorMessage = 'No se pudieron cargar los usuarios de la clínica.';
                this.loading = false;
            }
        });
    }

    saveUser(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        const selectedStaffType = (this.form.value.staffType as ClinicUserStaffType) ?? 'DENTIST';
        const selectedRole = (this.form.value.role as 'ADMIN' | 'USER') ?? 'USER';
        const normalizedRole = selectedStaffType === 'DENTIST' ? 'USER' : selectedRole;

        const payload = {
            username: this.form.value.username ?? '',
            fullName: this.form.value.fullName ?? '',
            email: this.form.value.email ?? '',
            password: this.form.value.password ?? '',
            role: normalizedRole,
            staffType: selectedStaffType
        };

        const request$ = this.editingUserId
            ? this.clinicService.updateClinicUser(this.clinicId, this.editingUserId, {
                username: payload.username,
                fullName: payload.fullName,
                email: payload.email,
                password: payload.password || undefined,
                role: payload.role,
                staffType: payload.staffType
            } as UpdateClinicUserRequest)
            : this.clinicService.createClinicUser(this.clinicId, payload as CreateClinicUserRequest);

        request$.subscribe({
            next: () => {
                this.resetForm();
                this.loadUsers();
            },
            error: (error) => {
                this.errorMessage = getHttpErrorMessage(error);
            }
        });
    }

    startEdit(user: ClinicUser): void {
        this.editingUserId = user.id;
        this.form.patchValue({
            username: user.username,
            fullName: user.fullName,
            email: user.email,
            password: '',
            staffType: user.staffType ?? 'ADMINISTRATIVE',
            role: user.role === 'SUPER_ADMIN' ? 'ADMIN' : user.role
        });
        this.syncPasswordValidator();
    }

    cancelEdit(): void {
        this.resetForm();
    }

    getStaffTypeLabel(staffType?: ClinicUserStaffType): string {
        return staffType === 'DENTIST' ? 'Dentista' : 'Personal administrativo';
    }

    getRoleLabel(role: ClinicUser['role']): string {
        if (role === 'ADMIN') {
            return 'Administrador';
        }

        if (role === 'SUPER_ADMIN') {
            return 'Super administrador';
        }

        return 'Usuario';
    }

    private resetForm(): void {
        this.editingUserId = null;
        this.form.reset({
            username: '',
            fullName: '',
            email: '',
            password: '',
            staffType: 'DENTIST',
            role: 'USER'
        });
        this.syncPasswordValidator();
    }

    private syncPasswordValidator(): void {
        const validators = [Validators.minLength(8), Validators.maxLength(100)];

        if (!this.editingUserId) {
            validators.unshift(Validators.required);
        }

        this.form.controls.password.setValidators(validators);
        this.form.controls.password.updateValueAndValidity({emitEvent: false});
    }

    deactivateUser(userId: string): void {
        const ref = this.dialog.open(ConfirmDialogComponent, {
            data: {
                title: 'Desactivar usuario',
                message: 'El usuario dejará de poder acceder al sistema para esta clínica. ¿Deseas continuar?',
                confirmLabel: 'Desactivar',
                cancelLabel: 'Cancelar',
                danger: true
            }
        });

        ref.afterClosed().subscribe((confirmed) => {
            if (!confirmed) {
                return;
            }

            this.clinicService.deactivateClinicUser(this.clinicId, userId).subscribe({
                next: () => this.loadUsers(),
                error: (error) => {
                    this.errorMessage = getHttpErrorMessage(error);
                }
            });
        });
    }
}
