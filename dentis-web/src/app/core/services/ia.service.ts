import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ChatSession, ChatMessage, CreateSessionRequest, SendMessageRequest, IaStatsResponse } from '../models/ia.model';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class IaService {
  private readonly base = '/api/v1/ia';

  constructor(private http: HttpClient) {}

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
    const req: SendMessageRequest = { content };
    return this.http.post<ApiResponse<ChatMessage>>(`${this.base}/chat/sessions/${sessionId}/messages`, req)
      .pipe(map(r => r.data));
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
