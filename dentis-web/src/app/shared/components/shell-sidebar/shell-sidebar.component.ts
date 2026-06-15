import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { AppNavItem } from '../../../core/navigation/navigation.config';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-shell-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatListModule
  ],
  template: `
    <div class="sidenav-header">
      <div class="brand-badge">
        <mat-icon class="logo-icon">local_hospital</mat-icon>
      </div>
      <div class="brand-copy">
        <span class="brand">Dentis</span>
        <span class="brand-subtitle">Dental workspace</span>
      </div>
    </div>

    <mat-divider class="sidebar-divider" />

    <mat-nav-list class="menu-list">
      @for (item of items(); track item.route) {
        @if (item.externalUrl) {
          <a mat-list-item
             class="menu-link"
             [href]="item.externalUrl"
             target="_blank"
             rel="noopener noreferrer"
             (click)="onItemSelected()">
            <mat-icon matListItemIcon class="menu-icon">{{ item.icon }}</mat-icon>
            <span matListItemTitle class="menu-label">{{ item.label }}</span>
            <mat-icon class="external-icon">open_in_new</mat-icon>
          </a>
        } @else {
          <a mat-list-item
             class="menu-link"
             [routerLink]="item.route"
             routerLinkActive="active-link"
             (click)="onItemSelected()"
             [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }">
            <mat-icon matListItemIcon class="menu-icon">{{ item.icon }}</mat-icon>
            <span matListItemTitle class="menu-label">{{ item.label }}</span>
          </a>
        }
      }
    </mat-nav-list>

    <div class="sidenav-footer">
      <mat-divider class="sidebar-divider" />
      <div class="user-info">
        <div class="user-avatar">
          {{ auth.currentUser()?.username?.slice(0, 1)?.toUpperCase() || 'U' }}
        </div>
        <div class="user-copy">
          <span class="user-name">{{ auth.currentUser()?.username }}</span>
          <span class="user-role">{{ auth.currentUser()?.role }}</span>
        </div>
      </div>
      <button mat-button color="warn" class="logout-btn" (click)="auth.logout()">
        <mat-icon>logout</mat-icon>
        Cerrar Sesión
      </button>
    </div>
  `,
  styles: [`
    :host {
      display: flex;
      flex-direction: column;
      min-height: 100%;
      color: var(--dentis-sidenav-text);
    }
    .sidenav-header {
      display: flex; align-items: center; gap: 14px;
      padding: 24px 20px 20px;
    }
    .brand-badge {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0d9488 0%, #0ea5e9 100%);
      box-shadow: 0 12px 20px rgba(13, 148, 136, 0.30);
    }
    .logo-icon { font-size: 26px; width: 26px; height: 26px; color: white; }
    .brand-copy { display: flex; flex-direction: column; }
    .brand { font-size: 18px; font-weight: 800; letter-spacing: .02em; color: var(--dentis-sidenav-text); }
    .brand-subtitle { font-size: 12px; color: var(--dentis-sidenav-text-muted); }
    .sidebar-divider {
      --mat-divider-color: var(--dentis-sidenav-border);
      opacity: 1;
    }
    .menu-list {
      padding: 14px 10px 4px;
    }
    .sidenav-footer { margin-top: auto; padding: 14px 12px 12px; }
    .user-info {
      display: flex; align-items: center; gap: 12px;
      padding: 14px 10px 10px; font-size: 13px;
      color: var(--dentis-sidenav-text-muted);
    }
    .user-avatar {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      background: rgba(255,255,255,.12);
      color: var(--dentis-sidenav-text);
      box-shadow: inset 0 1px 0 rgba(255,255,255,.12);
    }
    .user-copy { display: flex; flex-direction: column; gap: 2px; }
    .user-name { color: var(--dentis-sidenav-text); font-weight: 700; }
    .user-role { color: var(--dentis-sidenav-text-muted); font-size: 12px; text-transform: uppercase; }
    .menu-link {
      min-height: 48px;
      display: flex;
      align-items: center;
      border-radius: 16px;
      margin: 4px 0;
      color: var(--dentis-sidenav-text) !important;
      transition: background-color .2s ease, transform .2s ease, box-shadow .2s ease;
    }
    .menu-link:hover {
      background: var(--dentis-sidenav-hover) !important;
      transform: translateX(2px);
      box-shadow: inset 0 0 0 1px rgba(255,255,255,.04);
    }
    .menu-link .menu-label {
      color: var(--dentis-sidenav-text) !important;
      font-size: 14px;
      font-weight: 600;
      letter-spacing: .01em;
    }
    .menu-link .menu-icon {
      color: var(--dentis-sidenav-icon) !important;
      opacity: .98;
    }
    .logout-btn {
      width: 100%;
      justify-content: flex-start;
      gap: 8px;
      margin-top: 6px;
      color: var(--dentis-sidenav-text-muted);
      font-weight: 600;
      border-radius: 14px;
    }
    .logout-btn:hover {
      background: rgba(248, 113, 113, 0.08);
      color: #fecaca;
    }
    .external-icon {
      font-size: 14px; width: 14px; height: 14px;
      margin-left: auto; opacity: .45;
    }
    .menu-link.active-link {
      background: linear-gradient(90deg, var(--dentis-sidenav-active) 0%, var(--dentis-sidenav-active-strong) 100%) !important;
      color: var(--dentis-sidenav-text) !important;
      box-shadow: inset 0 0 0 1px rgba(255,255,255,.08), 0 12px 24px rgba(15, 23, 42, 0.16);
    }
    .menu-link.active-link .menu-label,
    .menu-link.active-link .menu-icon {
      color: #ffffff !important;
    }
  `]
})
export class ShellSidebarComponent {
  readonly items = input.required<AppNavItem[]>();
  readonly itemSelected = output<void>();

  constructor(public auth: AuthService) {}

  onItemSelected(): void {
    this.itemSelected.emit();
  }
}

