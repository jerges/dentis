import { Component, OnInit, OnDestroy, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { IaService } from '../../../core/services/ia.service';
import { AuthService } from '../../../core/services/auth.service';
import { ChatSession, ChatMessage } from '../../../core/models/ia.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { LinkifyPipe } from '../../../shared/pipes/linkify.pipe';

@Component({
  selector: 'app-ia-chat',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule, MatListModule,
    MatProgressSpinnerModule, MatDividerModule, MatTooltipModule,
    MatSnackBarModule, PageHeaderComponent, LinkifyPipe
  ],
  template: `
    <div class="page-container">
      <app-page-header title="Asistente IA" subtitle="Consultas clínicas odontológicas con inteligencia artificial">
        <button mat-stroked-button (click)="newSession()" [disabled]="loadingSend()">
          <mat-icon>add</mat-icon> Nueva sesión
        </button>
      </app-page-header>

      <div class="chat-layout">
        <!-- Session list -->
        <mat-card class="sessions-panel">
          <mat-card-header>
            <mat-card-title>Conversaciones</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            @if (loadingSessions()) {
              <div class="center-spinner"><mat-spinner diameter="32" /></div>
            } @else if (sessions().length === 0) {
              <p class="empty-hint">Sin conversaciones. Crea una nueva.</p>
            } @else {
              <mat-nav-list dense>
                @for (s of sessions(); track s.id) {
                  <a mat-list-item
                     [class.active-session]="activeSession()?.id === s.id"
                     (click)="selectSession(s)">
                    <mat-icon matListItemIcon>chat_bubble_outline</mat-icon>
                    <span matListItemTitle>{{ s.title || 'Consulta clínica' }}</span>
                    <span matListItemLine class="session-date">{{ s.updatedAt | date:'dd/MM HH:mm' }}</span>
                    <button mat-icon-button matListItemMeta
                            (click)="$event.stopPropagation(); deleteSession(s.id)"
                            matTooltip="Eliminar">
                      <mat-icon class="delete-icon">delete_outline</mat-icon>
                    </button>
                  </a>
                }
              </mat-nav-list>
            }
          </mat-card-content>
        </mat-card>

        <!-- Chat panel -->
        <mat-card class="chat-panel">
          @if (!activeSession()) {
            <div class="empty-chat">
              <mat-icon class="empty-icon">smart_toy</mat-icon>
              <h3>Asistente Odontológico IA</h3>
              <p>Selecciona una conversación o crea una nueva para comenzar.</p>
              <button mat-flat-button color="primary" (click)="newSession()">
                <mat-icon>add</mat-icon> Iniciar consulta
              </button>
            </div>
          } @else {
            <mat-card-header class="chat-header">
              <mat-icon mat-card-avatar>smart_toy</mat-icon>
              <mat-card-title>{{ activeSession()!.title || 'Consulta clínica' }}</mat-card-title>
              <mat-card-subtitle>{{ activeSession()!.updatedAt | date:'dd/MM/yyyy HH:mm' }}</mat-card-subtitle>
            </mat-card-header>

            <mat-divider />

            <div class="messages-area" #messagesArea>
              @if (loadingMessages()) {
                <div class="center-spinner"><mat-spinner diameter="36" /></div>
              } @else {
                @if (messages().length === 0 && !loadingSend()) {
                  <div class="disclaimer">
                    <mat-icon class="disclaimer-icon">warning_amber</mat-icon>
                    <span>Este asistente es una herramienta de apoyo. Verifica siempre
                    la información con fuentes clínicas oficiales — la decisión
                    final corresponde al profesional tratante.</span>
                  </div>
                }
                @for (msg of messages(); track msg.id) {
                  <div class="message-row" [class.user-row]="msg.role === 'USER'" [class.assistant-row]="msg.role === 'ASSISTANT'">
                    <div class="message-bubble" [class.user-bubble]="msg.role === 'USER'" [class.assistant-bubble]="msg.role === 'ASSISTANT'">
                      <p class="msg-text" [innerHTML]="msg.content | linkify"></p>
                      @if (msg.citations?.length) {
                        <div class="citations">
                          <span class="citations-label">Fuentes:</span>
                          @for (c of msg.citations; track $index) {
                            <span class="citation-chip">{{ c }}</span>
                          }
                        </div>
                      }
                      <div class="msg-meta">
                        <span>{{ msg.createdAt | date:'HH:mm' }}</span>
                        @if (msg.role === 'ASSISTANT' && msg.outputTokens) {
                          <span class="token-info">{{ msg.inputTokens }}↑ {{ msg.outputTokens }}↓ tokens</span>
                        }
                      </div>
                    </div>
                  </div>
                }
                @if (loadingSend()) {
                  <div class="message-flow">
                    @if (streamingThinking()) {
                      <div class="flow-thinking">
                        <mat-icon class="thinking-icon">psychology</mat-icon>
                        <span>Razonando...</span>
                      </div>
                    }
                    @if (toolLog().length > 0) {
                      <div class="flow-tools">
                        @for (entry of toolLog(); track entry.toolName + entry.label) {
                          <div class="tool-log-entry" [class.tool-log-done]="entry.status === 'done'">
                            @if (entry.status !== 'done') {
                              <mat-spinner diameter="14" />
                            }
                            <mat-icon class="tool-status-icon" [class.tool-done-icon]="entry.status === 'done'">
                              {{ toolStatusIcon(entry.status) }}
                            </mat-icon>
                            <span class="tool-label">{{ entry.label }}</span>
                          </div>
                        }
                      </div>
                    }
                    @if (streamingText()) {
                      <div class="flow-response message-bubble assistant-bubble">
                        <p class="msg-text" [innerHTML]="streamingText() | linkify"></p><span class="cursor-blink">▋</span>
                      </div>
                    } @else if (!streamingThinking() && toolLog().length === 0) {
                      <div class="flow-response message-bubble assistant-bubble typing-bubble">
                        <mat-spinner diameter="20" />
                        <span>Procesando consulta...</span>
                      </div>
                    }
                  </div>
                }
              }
            </div>

            <mat-divider />

            <div class="input-area">
              <mat-form-field class="input-field" appearance="outline">
                <mat-label>Escribe tu consulta clínica...</mat-label>
                <textarea matInput
                          [(ngModel)]="inputText"
                          [disabled]="loadingSend()"
                          (keydown.enter)="onEnter($event)"
                          rows="2"
                          placeholder="¿Cuáles son los síntomas de una pulpitis irreversible?"></textarea>
              </mat-form-field>
              <button mat-fab color="primary"
                      [disabled]="!inputText.trim() || loadingSend()"
                      (click)="sendMessage()"
                      matTooltip="Enviar (Enter)">
                <mat-icon>send</mat-icon>
              </button>
            </div>
          }
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .page-container { max-width: 1400px; }

    .chat-layout {
      display: grid;
      grid-template-columns: 300px 1fr;
      gap: 16px;
      height: calc(100vh - 180px);
    }

    .sessions-panel {
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    .sessions-panel mat-card-content {
      flex: 1;
      overflow-y: auto;
      padding: 0;
    }
    .active-session {
      background: rgba(13, 148, 136, 0.12) !important;
      border-left: 3px solid #0d9488;
    }
    .session-date { font-size: 11px; color: var(--mat-sys-outline); }
    .delete-icon { font-size: 18px; width: 18px; height: 18px; opacity: 0.5; }
    .empty-hint { padding: 16px; color: var(--mat-sys-outline); font-size: 13px; text-align: center; }

    .chat-panel {
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    .chat-header { padding-bottom: 8px; }

    .empty-chat {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      flex: 1;
      gap: 12px;
      padding: 40px;
      text-align: center;
    }
    .empty-icon { font-size: 64px; width: 64px; height: 64px; opacity: 0.3; }
    .empty-chat h3 { margin: 0; font-size: 20px; }
    .empty-chat p { margin: 0; color: var(--mat-sys-outline); }

    .messages-area {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .message-row { display: flex; }
    .user-row { justify-content: flex-end; }
    .assistant-row { justify-content: flex-start; }

    .message-bubble {
      max-width: 72%;
      padding: 10px 14px;
      border-radius: 16px;
      font-size: 14px;
      line-height: 1.5;
    }
    .user-bubble {
      background: #0d9488;
      color: white;
      border-bottom-right-radius: 4px;
    }
    .assistant-bubble {
      background: var(--mat-sys-surface-variant);
      color: var(--mat-sys-on-surface-variant);
      border-bottom-left-radius: 4px;
    }
    .typing-bubble { display: flex; align-items: center; gap: 10px; }

    .tool-log-entry {
      display: flex;
      align-items: center;
      gap: 7px;
      font-size: 12px;
      color: var(--mat-sys-on-surface-variant);
      transition: opacity 0.3s;
    }
    .tool-log-done { opacity: 0.55; }
    .tool-status-icon { font-size: 15px; width: 15px; height: 15px; color: #0d9488; }
    .tool-done-icon  { color: #10b981; }
    .tool-label { line-height: 1; }
    .cursor-blink { animation: blink 0.9s step-start infinite; }
    @keyframes blink { 50% { opacity: 0; } }

    .disclaimer {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      padding: 12px 16px;
      margin-bottom: 8px;
      background: rgba(255, 160, 0, 0.08);
      border: 1px solid rgba(255, 160, 0, 0.3);
      border-radius: 8px;
      font-size: 13px;
      color: var(--mat-sys-on-surface-variant);
      line-height: 1.5;
    }
    .disclaimer-icon { font-size: 18px; width: 18px; height: 18px; color: #f59e0b; flex-shrink: 0; margin-top: 1px; }

    .message-flow {
      display: flex;
      flex-direction: column;
      gap: 6px;
      max-width: 78%;
    }

    .flow-thinking, .flow-tools {
      display: flex;
      flex-direction: column;
      gap: 5px;
      padding: 2px 0;
    }
    .flow-thinking {
      display: flex;
      flex-direction: row;
      align-items: center;
      gap: 7px;
      font-size: 12px;
      color: #6366f1;
    }
    .thinking-icon { font-size: 15px; width: 15px; height: 15px; }
    .flow-response { border-radius: 16px 16px 16px 4px; }

    .msg-text { margin: 0 0 6px; white-space: pre-wrap; }
    .msg-text ::ng-deep .chat-link {
      color: inherit;
      text-decoration: underline;
      text-underline-offset: 2px;
      word-break: break-all;
    }
    .msg-text ::ng-deep .chat-link:hover { opacity: 0.8; }
    .msg-meta {
      display: flex;
      gap: 10px;
      font-size: 11px;
      opacity: 0.65;
      align-items: center;
    }
    .token-info { font-size: 10px; }

    .citations { margin-top: 6px; }
    .citations-label { font-size: 11px; font-weight: 600; display: block; margin-bottom: 4px; }
    .citation-chip {
      display: inline-block;
      font-size: 11px;
      background: rgba(0,0,0,0.1);
      border-radius: 4px;
      padding: 1px 6px;
      margin: 2px;
    }

    .input-area {
      display: flex;
      align-items: flex-end;
      gap: 12px;
      padding: 12px 16px;
    }
    .input-field { flex: 1; }

    .center-spinner { display: flex; justify-content: center; padding: 24px; }

    @media (max-width: 768px) {
      .chat-layout {
        grid-template-columns: 1fr;
        grid-template-rows: auto 1fr;
        height: auto;
      }
      .sessions-panel { max-height: 200px; }
    }
  `]
})
export class IaChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesArea') messagesAreaRef?: ElementRef<HTMLDivElement>;

  sessions          = signal<ChatSession[]>([]);
  activeSession     = signal<ChatSession | null>(null);
  messages          = signal<ChatMessage[]>([]);
  streamingText     = signal('');
  streamingThinking = signal('');
  toolLog           = signal<ToolLogEntry[]>([]);

  loadingSessions = signal(false);
  loadingMessages = signal(false);
  loadingSend     = signal(false);

  inputText = '';
  private shouldScrollToBottom = false;
  private streamSub?: Subscription;

  // frontend <thinking> parser — mirrors the backend state machine as defense-in-depth
  private thinkBuf    = '';
  private inThink     = false;

  constructor(
    private readonly ia: IaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void { this.loadSessions(); }

  ngOnDestroy(): void { this.streamSub?.unsubscribe(); }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  loadSessions(): void {
    this.loadingSessions.set(true);
    this.ia.listSessions().subscribe({
      next: list  => { this.sessions.set(list); this.loadingSessions.set(false); },
      error: ()   => this.loadingSessions.set(false)
    });
  }

  selectSession(session: ChatSession): void {
    this.activeSession.set(session);
    this.loadingMessages.set(true);
    this.ia.getMessages(session.id).subscribe({
      next: msgs => { this.messages.set(msgs); this.loadingMessages.set(false); this.shouldScrollToBottom = true; },
      error: ()  => this.loadingMessages.set(false)
    });
  }

  newSession(): void {
    this.ia.createSession({}).subscribe({
      next: session => { this.sessions.update(l => [session, ...l]); this.activeSession.set(session); this.messages.set([]); },
      error: () => this.snack.open('Error al crear sesión', 'Cerrar', { duration: 3000 })
    });
  }

  sendMessage(): void {
    const text    = this.inputText.trim();
    const session = this.activeSession();
    if (!text || !session) return;

    this.inputText = '';
    this.loadingSend.set(true);
    this.streamingText.set('');
    this.streamingThinking.set('');
    this.toolLog.set([]);
    this.thinkBuf = '';
    this.inThink  = false;

    // Optimistically add user message
    const userMsg: ChatMessage = {
      id: crypto.randomUUID(), sessionId: session.id, role: 'USER',
      content: text, citations: null, inputTokens: 0, outputTokens: 0,
      createdAt: new Date().toISOString()
    };
    this.messages.update(msgs => [...msgs, userMsg]);
    this.shouldScrollToBottom = true;

    this.streamSub?.unsubscribe();
    this.streamSub = this.ia.streamMessage(session.id, text).subscribe({
      next: ev => {
        if ('t' in ev) {
          this.feedToken(ev.t);
          this.shouldScrollToBottom = true;
        } else if ('thinking' in ev) {
          this.streamingThinking.update(s => s + ev.thinking);
          this.shouldScrollToBottom = true;
        } else if ('tool' in ev) {
          this.toolLog.update(log => {
            const activeIdx = log.findIndex(t => t.toolName === ev.tool && t.status !== 'done');
            if (ev.status === 'done') {
              return activeIdx !== -1
                ? log.map((t, i) => i === activeIdx ? { ...t, status: 'done' } : t)
                : log;
            }
            return activeIdx !== -1
              ? log
              : [...log, { toolName: ev.tool, label: ev.label, status: ev.status }];
          });
          this.shouldScrollToBottom = true;
        } else if ('done' in ev && ev.done) {
          const assistantMsg: ChatMessage = {
            id: crypto.randomUUID(), sessionId: session.id, role: 'ASSISTANT',
            content: this.streamingText(),
            citations: null,
            inputTokens:  ev.usage?.input_tokens  ?? 0,
            outputTokens: ev.usage?.output_tokens ?? 0,
            createdAt: new Date().toISOString()
          };
          this.messages.update(msgs => [...msgs, assistantMsg]);
          this.streamingText.set('');
          this.streamingThinking.set('');
          this.toolLog.set([]);
          this.loadingSend.set(false);
          this.shouldScrollToBottom = true;
        }
      },
      error: err => {
        this.streamingText.set('');
        this.streamingThinking.set('');
        this.toolLog.set([]);
        this.loadingSend.set(false);
        const msg = err?.message ?? 'Error al enviar el mensaje';
        this.snack.open(msg, 'Cerrar', { duration: 4000 });
      }
    });
  }

  deleteSession(sessionId: string): void {
    this.ia.deleteSession(sessionId).subscribe({
      next: () => {
        this.sessions.update(l => l.filter(s => s.id !== sessionId));
        if (this.activeSession()?.id === sessionId) { this.activeSession.set(null); this.messages.set([]); }
      },
      error: () => this.snack.open('Error al eliminar sesión', 'Cerrar', { duration: 3000 })
    });
  }

  onEnter(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!ke.shiftKey) { ke.preventDefault(); this.sendMessage(); }
  }

  toolStatusIcon(status: string): string {
    return status === 'searching' ? 'search' : status === 'done' ? 'check_circle' : 'settings';
  }

  // Intercepts <thinking>...</thinking> blocks arriving inside Token events.
  // Routes thinking content to streamingThinking and response text to streamingText.
  private feedToken(chunk: string): void {
    const OPEN  = '<thinking>';
    const CLOSE = '</thinking>';
    this.thinkBuf += chunk;

    while (this.thinkBuf.length > 0) {
      if (!this.inThink) {
        const openIdx = this.thinkBuf.indexOf(OPEN);
        if (openIdx === -1) {
          const lastAngle = this.thinkBuf.lastIndexOf('<');
          if (lastAngle >= 0 && OPEN.startsWith(this.thinkBuf.substring(lastAngle))) {
            const safe = this.thinkBuf.substring(0, lastAngle);
            if (safe) this.streamingText.update(s => s + safe);
            this.thinkBuf = this.thinkBuf.substring(lastAngle);
            return;
          }
          this.streamingText.update(s => s + this.thinkBuf);
          this.thinkBuf = '';
          return;
        }
        const before = this.thinkBuf.substring(0, openIdx);
        if (before) this.streamingText.update(s => s + before);
        this.thinkBuf = this.thinkBuf.substring(openIdx + OPEN.length);
        this.inThink  = true;
      } else {
        const closeIdx = this.thinkBuf.indexOf(CLOSE);
        if (closeIdx === -1) {
          const lastAngle = this.thinkBuf.lastIndexOf('<');
          if (lastAngle >= 0 && CLOSE.startsWith(this.thinkBuf.substring(lastAngle))) {
            const safe = this.thinkBuf.substring(0, lastAngle);
            if (safe) this.streamingThinking.update(s => s + safe);
            this.thinkBuf = this.thinkBuf.substring(lastAngle);
            return;
          }
          if (this.thinkBuf) this.streamingThinking.update(s => s + this.thinkBuf);
          this.thinkBuf = '';
          return;
        }
        const thinkText = this.thinkBuf.substring(0, closeIdx);
        if (thinkText) this.streamingThinking.update(s => s + thinkText);
        this.thinkBuf = this.thinkBuf.substring(closeIdx + CLOSE.length);
        this.inThink  = false;
      }
    }
  }

  private scrollToBottom(): void {
    const el = this.messagesAreaRef?.nativeElement;
    if (el) el.scrollTop = el.scrollHeight;
  }
}

interface ToolLogEntry {
  toolName: string;
  label: string;
  status: string;
}
