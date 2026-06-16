import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ChatSession, ChatMessage, CreateSessionRequest, SendMessageRequest, IaStatsResponse } from '../models/ia.model';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface SseToken   { t: string }
export interface SseDone    { done: true; usage: { input_tokens: number; output_tokens: number; cost_usd: number } }
export interface SseError   { err: string }
export type SseEvent = SseToken | SseDone | SseError;

@Injectable({ providedIn: 'root' })
export class IaService {
  private readonly base = '/api/v1/ia';
  private readonly http = inject(HttpClient);

  createSession(req: CreateSessionRequest = {}): Observable<ChatSession> {
    return this.http.post<ApiResponse<ChatSession>>(`${this.base}/chat/sessions`, req)
      .pipe(map(r => r.data));
  }

  listSessions(): Observable<ChatSession[]> {
    return this.http.get<ApiResponse<ChatSession[]>>(`${this.base}/chat/sessions`)
      .pipe(map(r => r.data));
  }

  getMessages(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ApiResponse<ChatMessage[]>>(`${this.base}/chat/sessions/${sessionId}/messages`)
      .pipe(map(r => r.data));
  }

  sendMessage(sessionId: string, content: string): Observable<ChatMessage> {
    return this.http.post<ApiResponse<ChatMessage>>(
      `${this.base}/chat/sessions/${sessionId}/messages`, { content } as SendMessageRequest)
      .pipe(map(r => r.data));
  }

  /**
   * POST SSE streaming endpoint — yields SseEvent objects as tokens arrive.
   * Uses fetch + ReadableStream so the Authorization header is included (EventSource doesn't support custom headers).
   */
  streamMessage(sessionId: string, content: string): Observable<SseEvent> {
    return new Observable<SseEvent>(subscriber => {
      const token = localStorage.getItem('dentis_token') ?? '';
      const ctrl  = new AbortController();

      fetch(`${this.base}/chat/sessions/${sessionId}/stream`, {
        method:  'POST',
        signal:  ctrl.signal,
        headers: {
          'Content-Type':  'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ content })
      }).then(res => {
        if (!res.ok || !res.body) {
          subscriber.error(new Error(`HTTP ${res.status}`));
          return;
        }
        const reader  = res.body.getReader();
        const decoder = new TextDecoder();
        let   buffer  = '';

        const pump = (): Promise<void> =>
          reader.read().then(({ done, value }) => {
            if (done) { subscriber.complete(); return; }
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() ?? '';
            for (const line of lines) {
              if (line.startsWith('data:')) {
                const raw = line.slice(5).trim();
                if (!raw) continue;
                try {
                  const ev = JSON.parse(raw) as SseEvent;
                  subscriber.next(ev);
                  if ('done' in ev && ev.done) { subscriber.complete(); return; }
                  if ('err' in ev) { subscriber.error(new Error(ev.err)); return; }
                } catch { /* ignore malformed line */ }
              }
            }
            return pump();
          });

        pump().catch(err => { if (err?.name !== 'AbortError') subscriber.error(err); });
      }).catch(err => { if (err?.name !== 'AbortError') subscriber.error(err); });

      return () => ctrl.abort();
    });
  }

  deleteSession(sessionId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/chat/sessions/${sessionId}`);
  }

  getStats(): Observable<IaStatsResponse> {
    return this.http.get<ApiResponse<IaStatsResponse>>(`${this.base}/stats`)
      .pipe(map(r => r.data));
  }

  reindex(clinicId?: string): Observable<string> {
    const params: Record<string, string> = clinicId ? { clinicId } : {};
    return this.http.post<ApiResponse<string>>(`${this.base}/index/reindex`, null, { params })
      .pipe(map(r => r.data));
  }
}
