import { Component, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NotificationService, AppNotification } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, MatButtonModule, MatTooltipModule],
  template: `
    <div class="notif-host">
      <button mat-icon-button
        class="notif-trigger"
        [class.has-unread]="notifSvc.unreadCount() > 0"
        (click)="toggle()"
        [matTooltip]="'Notificaciones'"
        aria-label="Centro de notificaciones">
        <mat-icon>notifications</mat-icon>
        @if (notifSvc.unreadCount() > 0) {
          <span class="badge" [attr.aria-label]="notifSvc.unreadCount() + ' sin leer'">
            {{ notifSvc.unreadCount() > 9 ? '9+' : notifSvc.unreadCount() }}
          </span>
        }
      </button>

      @if (open()) {
        <div class="notif-panel" role="dialog" aria-label="Notificaciones">
          <div class="notif-header">
            <span class="notif-title">Notificaciones</span>
            @if (notifSvc.unreadCount() > 0) {
              <button class="mark-all-btn" (click)="notifSvc.markAllRead()">
                Marcar todo leído
              </button>
            }
          </div>

          <div class="notif-list">
            @if (notifSvc.notifications().length === 0) {
              <div class="notif-empty">
                <mat-icon>notifications_none</mat-icon>
                <span>Sin notificaciones para hoy</span>
              </div>
            } @else {
              @for (n of notifSvc.notifications(); track n.id) {
                <a class="notif-item"
                   [class.unread]="!n.read"
                   [routerLink]="n.route ?? null"
                   (click)="onItemClick(n)"
                   role="menuitem">
                  <div class="notif-dot-col">
                    <span class="notif-type-icon" [class]="'type-' + n.type">
                      <mat-icon>{{ typeIcon(n.type) }}</mat-icon>
                    </span>
                  </div>
                  <div class="notif-copy">
                    <span class="notif-item-title">{{ n.title }}</span>
                    <span class="notif-item-body">{{ n.body }}</span>
                    <span class="notif-item-time">{{ n.time | date:'HH:mm' }}</span>
                  </div>
                  @if (!n.read) {
                    <span class="unread-dot" aria-hidden="true"></span>
                  }
                </a>
              }
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .notif-host { position: relative; }

    .notif-trigger { position: relative; color: var(--dentis-text-muted); }
    .notif-trigger.has-unread { color: var(--dentis-primary); }

    .badge {
      position: absolute; top: 4px; right: 4px;
      width: 17px; height: 17px; border-radius: 50%;
      background: var(--dentis-warn); color: #fff;
      font-size: 9px; font-weight: 700; line-height: 1;
      display: flex; align-items: center; justify-content: center;
      border: 2px solid var(--dentis-surface); pointer-events: none;
    }

    .notif-panel {
      position: absolute; top: calc(100% + 8px); right: 0;
      width: 360px; max-height: 480px;
      background: var(--dentis-surface);
      border: 1px solid var(--dentis-border);
      border-radius: 18px;
      box-shadow: 0 20px 60px rgba(15,23,42,.18);
      z-index: 500; overflow: hidden;
      animation: popIn .15s ease;
    }
    @keyframes popIn { from { opacity: 0; transform: translateY(-6px) scale(.97); } to { opacity: 1; transform: none; } }

    .notif-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 14px 16px 10px; border-bottom: 1px solid var(--dentis-border);
    }
    .notif-title { font-size: 14px; font-weight: 700; }
    .mark-all-btn {
      font-size: 12px; color: var(--dentis-primary); background: none; border: none;
      cursor: pointer; padding: 2px 6px; border-radius: 6px; transition: background .15s;
    }
    .mark-all-btn:hover { background: rgba(13,148,136,.08); }

    .notif-list { max-height: 400px; overflow-y: auto; }
    .notif-empty {
      display: flex; flex-direction: column; align-items: center; gap: 6px;
      padding: 32px; color: var(--dentis-text-muted); font-size: 13px;
    }
    .notif-empty mat-icon { font-size: 32px; width: 32px; height: 32px; opacity: .5; }

    .notif-item {
      display: flex; align-items: flex-start; gap: 10px; padding: 12px 16px;
      text-decoration: none; color: var(--dentis-text);
      border-bottom: 1px solid var(--dentis-border); transition: background .12s;
      cursor: pointer; position: relative;
    }
    .notif-item:last-child { border-bottom: none; }
    .notif-item:hover { background: rgba(13,148,136,.05); }
    .notif-item.unread { background: rgba(13,148,136,.03); }

    .notif-type-icon {
      width: 34px; height: 34px; border-radius: 10px;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    }
    .notif-type-icon mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .type-appointment { background: rgba(13,148,136,.12); color: var(--dentis-primary); }
    .type-payment     { background: rgba(22,163,74,.12);  color: var(--dentis-success); }
    .type-budget      { background: rgba(14,165,233,.12); color: var(--dentis-accent); }
    .type-info        { background: rgba(100,116,139,.12); color: var(--dentis-text-muted); }

    .notif-copy { flex: 1; display: flex; flex-direction: column; gap: 2px; }
    .notif-item-title { font-size: 13px; font-weight: 600; }
    .notif-item-body  { font-size: 12px; color: var(--dentis-text-muted); }
    .notif-item-time  { font-size: 11px; color: var(--dentis-text-muted); margin-top: 2px; }

    .unread-dot {
      width: 7px; height: 7px; border-radius: 50%;
      background: var(--dentis-primary); flex-shrink: 0; margin-top: 4px;
    }
  `]
})
export class NotificationCenterComponent {
  notifSvc = inject(NotificationService);
  open = signal(false);

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent): void {
    const host = (e.target as HTMLElement).closest('app-notification-center');
    if (!host) this.open.set(false);
  }

  toggle(): void {
    this.open.update((v) => !v);
    if (this.open()) this.notifSvc.load();
  }

  onItemClick(n: AppNotification): void {
    this.notifSvc.markRead(n.id);
    this.open.set(false);
  }

  typeIcon(type: AppNotification['type']): string {
    const map: Record<AppNotification['type'], string> = {
      appointment: 'event',
      payment: 'payments',
      budget: 'description',
      info: 'info',
    };
    return map[type];
  }
}
