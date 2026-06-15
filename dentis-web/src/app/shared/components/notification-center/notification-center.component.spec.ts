import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { NotificationCenterComponent } from './notification-center.component';
import { NotificationService, AppNotification } from '../../../core/services/notification.service';

describe('NotificationCenterComponent', () => {
  let component: NotificationCenterComponent;
  let fixture: ComponentFixture<NotificationCenterComponent>;
  let notifSpy: jasmine.SpyObj<NotificationService>;

  const mockNotification: AppNotification = {
    id: 'notif-1',
    type: 'appointment',
    title: 'Cita de hoy',
    body: 'Ana Lopez · 10:00',
    time: '2026-06-15T10:00:00',
    route: '/scheduling',
    read: false
  };

  beforeEach(async () => {
    notifSpy = jasmine.createSpyObj<NotificationService>(
      'NotificationService',
      ['load', 'markRead', 'markAllRead'],
      {
        notifications: signal([mockNotification]),
        unreadCount: signal(1)
      }
    );

    await TestBed.configureTestingModule({
      imports: [NotificationCenterComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: NotificationService, useValue: notifSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationCenterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with panel closed', () => {
    expect(component).toBeTruthy();
    expect(component.open()).toBeFalse();
  });

  it('should open panel and call load() on first toggle', () => {
    component.toggle();
    expect(component.open()).toBeTrue();
    expect(notifSpy.load).toHaveBeenCalled();
  });

  it('should close panel on second toggle without calling load again', () => {
    component.toggle();
    component.toggle();
    expect(component.open()).toBeFalse();
    expect(notifSpy.load).toHaveBeenCalledTimes(1);
  });

  it('should call markRead and close panel when notification is clicked', () => {
    component.toggle();
    component.onItemClick(mockNotification);

    expect(notifSpy.markRead).toHaveBeenCalledWith('notif-1');
    expect(component.open()).toBeFalse();
  });

  it('should return correct icon for each notification type', () => {
    expect(component.typeIcon('appointment')).toBe('event');
    expect(component.typeIcon('payment')).toBe('payments');
    expect(component.typeIcon('budget')).toBe('description');
    expect(component.typeIcon('info')).toBe('info');
  });
});
