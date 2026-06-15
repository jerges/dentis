import { Injectable, inject, signal } from '@angular/core';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AppointmentService } from './appointment.service';
import { AuthService } from './auth.service';
import { ClinicService } from './clinic.service';

export interface AppNotification {
  id: string;
  type: 'appointment' | 'payment' | 'budget' | 'info';
  title: string;
  body: string;
  time: string;
  route?: string;
  read: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly notifications = signal<AppNotification[]>([]);
  readonly unreadCount = signal(0);

  private auth = inject(AuthService);
  private appointmentSvc = inject(AppointmentService);
  private clinicSvc = inject(ClinicService);

  private loaded = false;

  load(): void {
    if (this.loaded) return;
    this.loaded = true;

    const currentUser = this.auth.currentUser();
    const clinicId = currentUser?.clinicId;
    const username = currentUser?.username;
    if (!clinicId || !username) return;

    const now = new Date();
    const from = this.toIso(new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0));
    const to   = this.toIso(new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59));

    this.clinicSvc.getClinicUsers(clinicId).pipe(
      map((resp) => {
        const users = resp.data ?? [];
        return users.find((u) => u.username === username)?.id ?? null;
      }),
      switchMap((dentistId) => {
        if (!dentistId) return of([] as AppNotification[]);
        return this.appointmentSvc.getByDentist(dentistId, from, to).pipe(
          catchError(() => of([])),
          map((appointments) =>
            appointments.slice(0, 10).map((a) => ({
              id: `apt-${a.id}`,
              type: 'appointment' as const,
              title: 'Cita de hoy',
              body: `${a.patientName ?? 'Paciente'} · ${this.formatTime(a.startDateTime)}`,
              time: a.startDateTime,
              route: '/scheduling',
              read: false,
            }))
          )
        );
      }),
      catchError(() => of([] as AppNotification[]))
    ).subscribe((notifs) => {
      this.notifications.set(notifs);
      this.unreadCount.set(notifs.filter((n) => !n.read).length);
    });
  }

  markAllRead(): void {
    this.notifications.update((n) => n.map((item) => ({ ...item, read: true })));
    this.unreadCount.set(0);
  }

  markRead(id: string): void {
    this.notifications.update((n) =>
      n.map((item) => item.id === id ? { ...item, read: true } : item)
    );
    this.unreadCount.set(this.notifications().filter((n) => !n.read).length);
  }

  private toIso(date: Date): string {
    return date.toISOString().slice(0, 16);
  }

  private formatTime(iso: string): string {
    try {
      return new Date(iso).toLocaleTimeString('es-VE', { hour: '2-digit', minute: '2-digit' });
    } catch { return iso; }
  }
}
