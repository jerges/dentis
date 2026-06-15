import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="login-wrapper">
      <div class="login-hero">
        <div class="hero-badge">
          <mat-icon>monitor_heart</mat-icon>
        </div>
        <h1>Dentis Workspace</h1>
        <p>Administra clínicas, pacientes y operaciones desde un único panel.</p>
      </div>

      <mat-card class="login-card">
        <mat-card-header>
          <div class="login-header">
            <span class="eyebrow">Bienvenido</span>
            <h2>Inicia sesión</h2>
            <p>Accede con tus credenciales para continuar</p>
          </div>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Usuario</mat-label>
              <input matInput formControlName="username" autocomplete="username" />
              <mat-icon matPrefix>person</mat-icon>
              @if (form.get('username')?.hasError('required') && form.get('username')?.touched) {
                <mat-error>El usuario es obligatorio</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Contraseña</mat-label>
              <input matInput [type]="hidePassword ? 'password' : 'text'"
                     formControlName="password" autocomplete="current-password" />
              <mat-icon matPrefix>lock</mat-icon>
              <button mat-icon-button matSuffix type="button" (click)="hidePassword = !hidePassword">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (form.get('password')?.hasError('required') && form.get('password')?.touched) {
                <mat-error>La contraseña es obligatoria</mat-error>
              }
            </mat-form-field>

            @if (errorMessage) {
              <div class="error-alert">
                <mat-icon>error_outline</mat-icon>
                {{ errorMessage }}
              </div>
            }

            <button mat-raised-button color="primary" type="submit"
                    class="full-width login-btn" [disabled]="loading">
              @if (loading) {
                <mat-spinner diameter="20" />
              } @else {
                <ng-container>
                  <mat-icon>login</mat-icon>
                  <span>Iniciar Sesión</span>
                </ng-container>
              }
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-wrapper {
      min-height: 100vh;
      display: grid;
      grid-template-columns: minmax(320px, 520px) minmax(360px, 420px);
      align-items: center;
      justify-content: center;
      gap: 48px;
      padding: 32px;
      background:
        radial-gradient(circle at top left, rgba(13, 148, 136, 0.20), transparent 28%),
        radial-gradient(circle at bottom right, rgba(14, 165, 233, 0.16), transparent 24%),
        linear-gradient(160deg, #04232a 0%, #0d4a50 45%, #0e3a55 100%);
    }
    .login-hero { color: white; padding-right: 12px; }
    .hero-badge {
      width: 64px; height: 64px; border-radius: 18px;
      display: flex; align-items: center; justify-content: center;
      background: linear-gradient(135deg, #0d9488 0%, #0ea5e9 100%);
      box-shadow: 0 16px 40px rgba(13, 148, 136, .40);
      margin-bottom: 20px;
    }
    .hero-badge mat-icon { font-size: 32px; width: 32px; height: 32px; }
    .login-hero h1 { margin: 0 0 10px; font-size: 42px; line-height: 1.05; font-weight: 800; color: white; }
    .login-hero p { margin: 0; color: rgba(255,255,255,.72); font-size: 16px; line-height: 1.6; max-width: 460px; }
    .login-card { width: 100%; padding: 28px; border-radius: 24px !important; box-shadow: 0 24px 80px rgba(15, 23, 42, .28) !important; }
    .login-header {
      width: 100%; padding: 8px 0 8px;
      display: flex; flex-direction: column; gap: 6px;
    }
    .eyebrow { color: var(--dentis-primary); font-size: 12px; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
    h2 { margin: 0; font-size: 28px; font-weight: 800; color: #0f172a; }
    .login-header p { margin: 0; color: #64748b; font-size: 14px; }
    form { display: flex; flex-direction: column; gap: 14px; margin-top: 20px; }
    .full-width { width: 100%; }
    .login-btn { height: 50px; font-size: 15px; margin-top: 8px; display: flex; align-items: center; gap: 8px; justify-content: center; border-radius: 14px; }
    .error-alert {
      display: flex; align-items: center; gap: 8px;
      background: rgba(220, 38, 38, .08); color: #dc2626; padding: 10px 14px;
      border: 1px solid rgba(220, 38, 38, .14); border-radius: 12px; font-size: 13px;
    }
    @media (max-width: 980px) {
      .login-wrapper {
        grid-template-columns: 1fr;
        gap: 24px;
      }
      .login-hero {
        padding-right: 0;
      }
    }
  `]
})
export class LoginComponent {
  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  loading = false;
  hidePassword = true;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.errorMessage = '';

    const { username, password } = this.form.value;
    this.auth.login({ username: username!, password: password! }).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => {
        this.errorMessage = 'Credenciales incorrectas. Intente de nuevo.';
        this.loading = false;
      }
    });
  }
}

