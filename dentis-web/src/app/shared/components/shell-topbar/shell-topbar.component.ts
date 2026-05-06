import { Component, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-shell-topbar',
  standalone: true,
  imports: [MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <mat-toolbar color="primary" class="app-toolbar">
      <button mat-icon-button (click)="menuClick.emit()">
        <mat-icon>menu</mat-icon>
      </button>

      <div class="toolbar-copy">
        <span class="toolbar-title">Sistema de Gestión Dental</span>
        <span class="toolbar-subtitle">Backoffice operativo</span>
      </div>

      <span class="spacer"></span>

      <button
        mat-icon-button
        class="theme-toggle"
        (click)="theme.toggleTheme()"
        [matTooltip]="theme.theme() === 'light' ? 'Activar tema oscuro' : 'Activar tema claro'">
        <mat-icon>{{ theme.theme() === 'light' ? 'dark_mode' : 'light_mode' }}</mat-icon>
      </button>

      <div class="toolbar-role">{{ auth.currentUser()?.role }}</div>
    </mat-toolbar>
  `,
  styles: [`
    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 100;
      color: var(--dentis-text);
      background: rgba(255,255,255,.82) !important;
      backdrop-filter: blur(14px);
      box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
    }
    :host-context(body.theme-dark) .app-toolbar {
      background: rgba(15, 23, 42, 0.7) !important;
      box-shadow: 0 10px 30px rgba(2, 6, 23, 0.22);
    }
    .toolbar-copy {
      display: flex;
      flex-direction: column;
      margin-left: 8px;
    }
    .toolbar-title { font-size: 15px; font-weight: 700; line-height: 1.1; }
    .toolbar-subtitle { font-size: 12px; color: var(--dentis-text-muted); }
    .spacer { flex: 1 1 auto; }
    .toolbar-role {
      padding: 6px 12px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: .04em;
      color: #4338ca;
      background: rgba(79, 70, 229, 0.08);
      text-transform: uppercase;
    }
    :host-context(body.theme-dark) .toolbar-role {
      color: #c7d2fe;
      background: rgba(129, 140, 248, 0.12);
    }
    .theme-toggle {
      margin-right: 8px;
      color: var(--dentis-text-muted);
    }
  `]
})
export class ShellTopbarComponent {
  readonly menuClick = output<void>();

  constructor(
    public auth: AuthService,
    public theme: ThemeService
  ) {}
}

