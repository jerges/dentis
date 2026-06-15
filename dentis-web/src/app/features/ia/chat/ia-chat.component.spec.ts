import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { IaChatComponent } from './ia-chat.component';
import { IaService } from '../../../core/services/ia.service';
import { AuthService } from '../../../core/services/auth.service';
import { ChatSession, ChatMessage } from '../../../core/models/ia.model';

describe('IaChatComponent', () => {
  let component: IaChatComponent;
  let fixture: ComponentFixture<IaChatComponent>;
  let iaSpy: jasmine.SpyObj<IaService>;
  let authSpy: jasmine.SpyObj<AuthService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const session: ChatSession = {
    id: 'session-1',
    dentistId: 'dentist-1',
    clinicId: 'clinic-1',
    title: 'Test Session',
    createdAt: '2026-01-01T10:00:00',
    updatedAt: '2026-01-01T10:00:00'
  };

  const message: ChatMessage = {
    id: 'msg-1',
    sessionId: 'session-1',
    role: 'ASSISTANT',
    content: 'Hola, ¿en qué puedo ayudarte?',
    citations: null,
    inputTokens: 10,
    outputTokens: 20,
    createdAt: '2026-01-01T10:00:00'
  };

  beforeEach(async () => {
    iaSpy = jasmine.createSpyObj<IaService>('IaService', [
      'listSessions', 'getMessages', 'createSession', 'sendMessage', 'deleteSession'
    ]);
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getRole'], {
      currentUser: signal({ username: 'dr.house', role: 'USER', token: 'tok', clinicId: 'clinic-1' } as never)
    });
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    iaSpy.listSessions.and.returnValue(of([session]));
    iaSpy.getMessages.and.returnValue(of([message]));
    iaSpy.createSession.and.returnValue(of(session));
    iaSpy.sendMessage.and.returnValue(of(message));
    iaSpy.deleteSession.and.returnValue(of(undefined as never));
    authSpy.getRole.and.returnValue('USER');

    await TestBed.configureTestingModule({
      imports: [IaChatComponent, NoopAnimationsModule],
      providers: [
        { provide: IaService, useValue: iaSpy },
        { provide: AuthService, useValue: authSpy },
        { provide: MatSnackBar, useValue: snackSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IaChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load sessions on init', () => {
    expect(component).toBeTruthy();
    expect(iaSpy.listSessions).toHaveBeenCalled();
    expect(component.sessions().length).toBe(1);
    expect(component.sessions()[0].id).toBe('session-1');
  });

  it('should load messages when a session is selected', () => {
    component.selectSession(session);

    expect(iaSpy.getMessages).toHaveBeenCalledWith('session-1');
    expect(component.activeSession()?.id).toBe('session-1');
    expect(component.messages().length).toBe(1);
  });

  it('should send a message and append user + assistant messages', () => {
    component.selectSession(session);
    component.inputText = 'Qué es una pulpitis?';
    component.sendMessage();

    expect(iaSpy.sendMessage).toHaveBeenCalledWith('session-1', 'Qué es una pulpitis?');
    expect(component.messages().length).toBe(3); // loaded 1 + user + assistant
    const userMsg = component.messages()[1];
    expect(userMsg.role).toBe('USER');
    expect(userMsg.content).toBe('Qué es una pulpitis?');
  });

  it('should delete a session and remove it from the list', () => {
    component.deleteSession('session-1');

    expect(iaSpy.deleteSession).toHaveBeenCalledWith('session-1');
    expect(component.sessions().length).toBe(0);
  });

  it('should show snackbar error when sendMessage fails', () => {
    iaSpy.sendMessage.and.returnValue(throwError(() => ({ error: { message: 'Error de red' } })));
    component.selectSession(session);
    component.inputText = 'Consulta';
    component.sendMessage();

    expect(snackSpy.open).toHaveBeenCalledWith('Error de red', 'Cerrar', { duration: 4000 });
    expect(component.loadingSend()).toBeFalse();
  });
});
