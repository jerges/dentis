import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { NotificationService, AppNotification } from './notification.service';
import { AuthService } from './auth.service';
import { AppointmentService } from './appointment.service';
import { ClinicService } from './clinic.service';

const makeNotification = (id: string, read = false): AppNotification => ({
  id,
  type: 'appointment',
  title: 'Cita de hoy',
  body: 'Paciente · 09:00',
  time: '2026-06-15T09:00:00Z',
  route: '/scheduling',
  read,
});

describe('NotificationService', () => {
  let service: NotificationService;
  let authSpy: jasmine.SpyObj<AuthService>;
  let appointmentSpy: jasmine.SpyObj<AppointmentService>;
  let clinicSpy: jasmine.SpyObj<ClinicService>;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUser: jasmine.createSpy('currentUser').and.returnValue(null),
    });
    appointmentSpy = jasmine.createSpyObj<AppointmentService>('AppointmentService', ['getByDentist']);
    clinicSpy = jasmine.createSpyObj<ClinicService>('ClinicService', ['getClinicUsers']);

    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        { provide: AuthService, useValue: authSpy },
        { provide: AppointmentService, useValue: appointmentSpy },
        { provide: ClinicService, useValue: clinicSpy },
      ],
    });

    service = TestBed.inject(NotificationService);
  });

  it('should initialise with empty notifications and zero unread count', () => {
    expect(service.notifications()).toEqual([]);
    expect(service.unreadCount()).toBe(0);
  });

  it('should not load when user has no clinicId', () => {
    (authSpy.currentUser as jasmine.Spy).and.returnValue({ username: 'jbello', role: 'ADMIN', token: 'tok' });
    service.load();
    expect(clinicSpy.getClinicUsers).not.toHaveBeenCalled();
  });

  it('should populate notifications on successful load', () => {
    (authSpy.currentUser as jasmine.Spy).and.returnValue({
      username: 'jbello',
      role: 'ADMIN',
      token: 'tok',
      clinicId: 'clinic-1',
    });

    clinicSpy.getClinicUsers.and.returnValue(of({
      success: true,
      data: [{ id: 'dentist-1', username: 'jbello', fullName: 'J Bello' }],
    } as any));

    appointmentSpy.getByDentist.and.returnValue(of([
      { id: 'a1', patientName: 'Ana', startDateTime: '2026-06-15T09:00:00Z' } as any,
    ]));

    service.load();

    expect(service.notifications().length).toBe(1);
    expect(service.notifications()[0].id).toBe('apt-a1');
    expect(service.unreadCount()).toBe(1);
  });

  it('should mark all notifications as read', () => {
    service.notifications.set([
      makeNotification('apt-1', false),
      makeNotification('apt-2', false),
    ]);
    service.unreadCount.set(2);

    service.markAllRead();

    expect(service.notifications().every(n => n.read)).toBeTrue();
    expect(service.unreadCount()).toBe(0);
  });

  it('should mark a single notification as read and update unread count', () => {
    service.notifications.set([
      makeNotification('apt-1', false),
      makeNotification('apt-2', false),
    ]);
    service.unreadCount.set(2);

    service.markRead('apt-1');

    const n1 = service.notifications().find(n => n.id === 'apt-1');
    const n2 = service.notifications().find(n => n.id === 'apt-2');
    expect(n1?.read).toBeTrue();
    expect(n2?.read).toBeFalse();
    expect(service.unreadCount()).toBe(1);
  });

  it('should not request appointments when dentist is not found in clinic users', () => {
    (authSpy.currentUser as jasmine.Spy).and.returnValue({
      username: 'unknown',
      role: 'ADMIN',
      token: 'tok',
      clinicId: 'clinic-1',
    });

    clinicSpy.getClinicUsers.and.returnValue(of({
      success: true,
      data: [{ id: 'dentist-1', username: 'jbello', fullName: 'J Bello' }],
    } as any));

    service.load();

    expect(appointmentSpy.getByDentist).not.toHaveBeenCalled();
    expect(service.notifications()).toEqual([]);
  });

  it('should handle appointment fetch errors gracefully', () => {
    (authSpy.currentUser as jasmine.Spy).and.returnValue({
      username: 'jbello',
      role: 'ADMIN',
      token: 'tok',
      clinicId: 'clinic-1',
    });

    clinicSpy.getClinicUsers.and.returnValue(of({
      success: true,
      data: [{ id: 'dentist-1', username: 'jbello', fullName: 'J Bello' }],
    } as any));

    appointmentSpy.getByDentist.and.returnValue(throwError(() => new Error('Network error')));

    service.load();

    expect(service.notifications()).toEqual([]);
    expect(service.unreadCount()).toBe(0);
  });
});
