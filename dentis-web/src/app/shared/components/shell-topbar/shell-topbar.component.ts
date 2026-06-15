import { Component, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';
import { GlobalSearchComponent } from '../global-search/global-search.component';
import { NotificationCenterComponent } from '../notification-center/notification-center.component';

@Component({
  selector: 'app-shell-topbar',
  standalone: true,
  imports: [
    MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule,
    GlobalSearchComponent, NotificationCenterComponent,
  ],
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

      <!-- Búsqueda global -->
      <button
        mat-stroked-button
        class="search-btn"
        (click)="searchComp.openSearch()"
        matTooltip="Buscar (⌘K / Ctrl+K)"
        aria-label="Abrir búsqueda global">
        <mat-icon>search</mat-icon>
        <span class="search-btn-label">Buscar…</span>
        <kbd class="search-kbd">⌘K</kbd>
      </button>

      <!-- Tema -->
      <button
        mat-icon-button
        class="theme-toggle"
        (click)="theme.toggleTheme()"
        [matTooltip]="theme.theme() === 'light' ? 'Activar tema oscuro' : 'Activar tema claro'">
        <mat-icon>{{ theme.theme() === 'light' ? 'dark_mode' : 'light_mode' }}</mat-icon>
      </button>

      <!-- Notificaciones -->
      <app-notification-center />

      <div class="toolbar-role">{{ auth.currentUser()?.role }}</div>
    </mat-toolbar>

    <!-- Command palette (montado fuera del toolbar para z-index) -->
    <app-global-search #searchComp />
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
      gap: 4px;
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

    .search-btn {
      height: 36px;
      border-radius: 10px !important;
      border-color: var(--dentis-border) !important;
      color: var(--dentis-text-muted) !important;
      display: flex; align-items: center; gap: 6px;
      padding: 0 12px !important;
      margin-right: 4px;
      background: var(--dentis-surface-alt) !important;
      transition: border-color .15s, background .15s;
    }
    .search-btn:hover {
      border-color: var(--dentis-primary) !important;
      background: rgba(13,148,136,.06) !important;
    }
    .search-btn mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .search-btn-label { font-size: 13px; font-weight: 500; }
    .search-kbd {
      font-size: 11px; padding: 2px 6px; border-radius: 5px;
      background: var(--dentis-surface); border: 1px solid var(--dentis-border);
      font-family: var(--dentis-font-ui); margin-left: 4px; opacity: .7;
    }
    @media (max-width: 600px) {
      .search-btn-label, .search-kbd { display: none; }
    }

    .toolbar-role {
      padding: 6px 12px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: .04em;
      color: var(--dentis-primary);
      background: rgba(13, 148, 136, 0.10);
      text-transform: uppercase;
      margin-left: 4px;
    }
    :host-context(body.theme-dark) .toolbar-role {
      color: #2dd4bf;
      background: rgba(45, 212, 191, 0.12);
    }
    .theme-toggle {
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
