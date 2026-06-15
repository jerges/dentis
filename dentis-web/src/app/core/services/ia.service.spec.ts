import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { IaService } from './ia.service';
import { ChatSession, ChatMessage, IaStatsResponse } from '../models/ia.model';

const BASE = '/api/v1/ia';

const mockSession: ChatSession = {
  id: 'session-1',
  dentistId: 'dentist-1',
  clinicId: 'clinic-1',
  title: 'Test session',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const mockMessage: ChatMessage = {
  id: 'msg-1',
  sessionId: 'session-1',
  role: 'ASSISTANT',
  content: 'Hello',
  citations: null,
  inputTokens: 10,
  outputTokens: 20,
  createdAt: '2026-01-01T00:00:00Z',
};

describe('IaService', () => {
  let service: IaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        IaService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(IaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create a session and return unwrapped data', () => {
    let result: ChatSession | undefined;
    service.createSession({ title: 'Test session' }).subscribe(s => (result = s));

    const req = httpMock.expectOne(`${BASE}/chat/sessions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Test session' });
    req.flush({ success: true, data: mockSession });

    expect(result).toEqual(mockSession);
  });

  it('should list sessions and return unwrapped array', () => {
    let result: ChatSession[] | undefined;
    service.listSessions().subscribe(s => (result = s));

    const req = httpMock.expectOne(`${BASE}/chat/sessions`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockSession] });

    expect(result).toEqual([mockSession]);
  });

  it('should get messages for a session', () => {
    let result: ChatMessage[] | undefined;
    service.getMessages('session-1').subscribe(m => (result = m));

    const req = httpMock.expectOne(`${BASE}/chat/sessions/session-1/messages`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockMessage] });

    expect(result).toEqual([mockMessage]);
  });

  it('should send a message and return the reply', () => {
    let result: ChatMessage | undefined;
    service.sendMessage('session-1', 'Hello').subscribe(m => (result = m));

    const req = httpMock.expectOne(`${BASE}/chat/sessions/session-1/messages`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ content: 'Hello' });
    req.flush({ success: true, data: mockMessage });

    expect(result).toEqual(mockMessage);
  });

  it('should delete a session', () => {
    let completed = false;
    service.deleteSession('session-1').subscribe({ complete: () => (completed = true) });

    const req = httpMock.expectOne(`${BASE}/chat/sessions/session-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(completed).toBeTrue();
  });

  it('should get stats and return unwrapped data', () => {
    const mockStats: IaStatsResponse = {
      totalSessions: 5,
      totalMessages: 20,
      totalInputTokens: 100,
      totalOutputTokens: 200,
      rows: [],
    };
    let result: IaStatsResponse | undefined;
    service.getStats().subscribe(s => (result = s));

    const req = httpMock.expectOne(`${BASE}/stats`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: mockStats });

    expect(result).toEqual(mockStats);
  });

  it('should reindex with optional clinicId param', () => {
    let result: string | undefined;
    service.reindex('clinic-1').subscribe(r => (result = r));

    const req = httpMock.expectOne(r => r.url === `${BASE}/index/reindex` && r.params.get('clinicId') === 'clinic-1');
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: 'Reindex started' });

    expect(result).toBe('Reindex started');
  });
});
