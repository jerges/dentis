import { Component, computed, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from './core/services/auth.service';
import { APP_NAV_ITEMS } from './core/navigation/navigation.config';
import { ShellSidebarComponent } from './shared/components/shell-sidebar/shell-sidebar.component';
import { ShellTopbarComponent } from './shared/components/shell-topbar/shell-topbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    MatSidenavModule, MatToolbarModule, MatListModule,
    MatIconModule, ShellSidebarComponent, ShellTopbarComponent
  ],
  template: `
    <mat-sidenav-container class="shell-container">
      <mat-sidenav #sidenav [opened]="sidenavOpen()" mode="side" class="sidenav">
        <app-shell-sidebar [items]="visibleNavItems()" />
      </mat-sidenav>

      <mat-sidenav-content>
        <app-shell-topbar (menuClick)="sidenavOpen.set(!sidenavOpen())" />
        <main class="main-content">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
      height: 100%;
    }
    .shell-container {
      width: 100%;
      height: 100%;
      background: transparent;
    }
    .sidenav {
      width: 280px;
      display: flex;
      flex-direction: column;
      border-right: 1px solid var(--dentis-sidenav-border);
      background: var(--dentis-sidenav-surface);
      color: var(--dentis-sidenav-text);
      box-shadow: 22px 0 48px rgba(15, 23, 42, 0.22);
    }
    mat-sidenav-content {
      display: flex !important;
      flex-direction: column;
    }
    .main-content {
      flex: 1 1 auto;
      min-height: 0;
      padding: clamp(16px, 2vh, 28px) clamp(16px, 2vw, 28px);
      overflow-y: auto;
      background: transparent;
    }
  `]
})
export class AppShellComponent {
  sidenavOpen = signal(true);

  readonly navItems = APP_NAV_ITEMS;

  readonly visibleNavItems = computed(() => {
    const role = this.auth.getRole();
    return this.navItems.filter((item) => !item.roles || !!role && item.roles.includes(role));
  });

  constructor(public auth: AuthService) {}
}
