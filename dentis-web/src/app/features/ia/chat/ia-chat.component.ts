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

@Component({
  selector: 'app-ia-chat',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule, MatListModule,
    MatProgressSpinnerModule, MatDividerModule, MatTooltipModule,
    MatSnackBarModule, PageHeaderComponent
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
                @for (msg of messages(); track msg.id) {
                  <div class="message-row" [class.user-row]="msg.role === 'USER'" [class.assistant-row]="msg.role === 'ASSISTANT'">
                    <div class="message-bubble" [class.user-bubble]="msg.role === 'USER'" [class.assistant-bubble]="msg.role === 'ASSISTANT'">
                      <p class="msg-text">{{ msg.content }}</p>
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
                @if (streamingText()) {
                  <div class="message-row assistant-row">
                    <div class="message-bubble assistant-bubble">
                      <p class="msg-text">{{ streamingText() }}<span class="cursor-blink">▋</span></p>
                    </div>
                  </div>
                } @else if (loadingSend()) {
                  <div class="message-row assistant-row">
                    <div class="message-bubble assistant-bubble typing-bubble">
                      <mat-spinner diameter="20" />
                      <span>Procesando consulta...</span>
                    </div>
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
    .cursor-blink { animation: blink 0.9s step-start infinite; }
    @keyframes blink { 50% { opacity: 0; } }

    .msg-text { margin: 0 0 6px; white-space: pre-wrap; }
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

  sessions      = signal<ChatSession[]>([]);
  activeSession = signal<ChatSession | null>(null);
  messages      = signal<ChatMessage[]>([]);
  streamingText = signal('');

  loadingSessions = signal(false);
  loadingMessages = signal(false);
  loadingSend     = signal(false);

  inputText = '';
  private shouldScrollToBottom = false;
  private streamSub?: Subscription;

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
          this.streamingText.update(s => s + ev.t);
          this.shouldScrollToBottom = true;
        } else if ('done' in ev && ev.done) {
          // Commit the streamed message to the messages list
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
          this.loadingSend.set(false);
          this.shouldScrollToBottom = true;
        }
      },
      error: err => {
        this.streamingText.set('');
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

  private scrollToBottom(): void {
    const el = this.messagesAreaRef?.nativeElement;
    if (el) el.scrollTop = el.scrollHeight;
  }
}
