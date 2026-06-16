import { Component, HostListener, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of, catchError, forkJoin } from 'rxjs';
import { PatientService } from '../../../core/services/patient.service';
import { DocumentsService } from '../../../core/services/documents.service';
import { Patient } from '../../../core/models/patient.model';
import { PageResponse } from '../../../core/models/api.model';

interface SearchResult {
  type: 'patient' | 'document';
  label: string;
  sublabel: string;
  route: string[];
  icon: string;
}

@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, MatIconModule],
  template: `
    @if (open()) {
      <div class="search-backdrop" (click)="close()" role="dialog" aria-modal="true" aria-label="Búsqueda global">
        <div class="search-panel" (click)="$event.stopPropagation()">
          <div class="search-input-wrap">
            <mat-icon class="search-icon" aria-hidden="true">search</mat-icon>
            <input #searchInput
              class="search-input"
              type="text"
              placeholder="Buscar paciente, cita, presupuesto…"
              [(ngModel)]="query"
              (ngModelChange)="onQuery($event)"
              autocomplete="off"
              aria-label="Buscar"
              autofocus />
            @if (query.length > 0) {
              <button class="search-clear" (click)="clearQuery()" aria-label="Limpiar búsqueda">
                <mat-icon>close</mat-icon>
              </button>
            }
            <kbd class="search-esc" aria-label="Presiona Escape para cerrar">Esc</kbd>
          </div>

          <div class="search-results" role="listbox">
            @if (loading()) {
              <div class="search-state">
                <span class="search-spinner"></span>
                Buscando…
              </div>
            } @else if (query.length >= 2 && patientResults().length === 0 && documentResults().length === 0) {
              <div class="search-state">
                <mat-icon>search_off</mat-icon>
                Sin resultados para "<strong>{{ query }}</strong>"
              </div>
            } @else if (results().length > 0) {
              @if (patientResults().length > 0) {
                <div class="results-group">
                  <div class="results-group-label">Pacientes</div>
                  @for (r of patientResults(); track r.route[1]) {
                    <a class="result-item" [routerLink]="r.route" (click)="close()" role="option">
                      <div class="result-icon"><mat-icon>{{ r.icon }}</mat-icon></div>
                      <div class="result-copy">
                        <span class="result-label">{{ r.label }}</span>
                        <span class="result-sub">{{ r.sublabel }}</span>
                      </div>
                      <mat-icon class="result-arrow">chevron_right</mat-icon>
                    </a>
                  }
                </div>
              }
              @if (documentResults().length > 0) {
                <div class="results-group">
                  <div class="results-group-label">Documentos</div>
                  @for (r of documentResults(); track r.route[1]) {
                    <a class="result-item" [routerLink]="r.route" (click)="close()" role="option">
                      <div class="result-icon doc-icon"><mat-icon>{{ r.icon }}</mat-icon></div>
                      <div class="result-copy">
                        <span class="result-label">{{ r.label }}</span>
                        <span class="result-sub">{{ r.sublabel }}</span>
                      </div>
                      <mat-icon class="result-arrow">chevron_right</mat-icon>
                    </a>
                  }
                </div>
              }
            } @else {
              <div class="search-hints">
                <div class="hint-item">
                  <kbd>↑</kbd><kbd>↓</kbd> navegar
                </div>
                <div class="hint-item">
                  <kbd>↵</kbd> abrir
                </div>
                <div class="hint-item">
                  <kbd>Esc</kbd> cerrar
                </div>
              </div>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .search-backdrop {
      position: fixed; inset: 0; z-index: 1000;
      background: rgba(15, 23, 42, 0.55);
      backdrop-filter: blur(4px);
      display: flex; align-items: flex-start; justify-content: center;
      padding-top: 80px;
      animation: fadeIn .15s ease;
    }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

    .search-panel {
      width: 100%; max-width: 600px;
      background: var(--dentis-surface);
      border-radius: 20px;
      box-shadow: 0 24px 80px rgba(15, 23, 42, 0.28);
      border: 1px solid var(--dentis-border);
      overflow: hidden;
      animation: slideDown .15s ease;
    }
    @keyframes slideDown { from { opacity: 0; transform: translateY(-12px); } to { opacity: 1; transform: none; } }

    .search-input-wrap {
      display: flex; align-items: center; gap: 10px;
      padding: 16px 20px; border-bottom: 1px solid var(--dentis-border);
    }
    .search-icon { color: var(--dentis-text-muted); flex-shrink: 0; }
    .search-input {
      flex: 1; border: none; outline: none; background: transparent;
      font-size: 16px; font-family: var(--dentis-font-ui);
      color: var(--dentis-text);
    }
    .search-input::placeholder { color: var(--dentis-text-muted); }
    .search-clear {
      background: none; border: none; cursor: pointer; padding: 4px;
      color: var(--dentis-text-muted); display: flex; align-items: center;
      border-radius: 6px; transition: background .15s;
    }
    .search-clear:hover { background: rgba(13,148,136,.08); color: var(--dentis-primary); }
    .search-esc {
      font-size: 11px; padding: 3px 7px; border-radius: 5px;
      background: var(--dentis-surface-alt); border: 1px solid var(--dentis-border);
      color: var(--dentis-text-muted); font-family: var(--dentis-font-ui); flex-shrink: 0;
    }

    .search-results { max-height: 400px; overflow-y: auto; }
    .search-state {
      display: flex; align-items: center; justify-content: center; gap: 8px;
      padding: 40px 24px; color: var(--dentis-text-muted); font-size: 14px;
    }
    .search-spinner {
      width: 18px; height: 18px; border-radius: 50%;
      border: 2px solid var(--dentis-border); border-top-color: var(--dentis-primary);
      animation: spin .7s linear infinite; flex-shrink: 0;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .results-group { padding: 8px 0; }
    .results-group-label {
      font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .06em;
      color: var(--dentis-text-muted); padding: 4px 20px 8px;
    }
    .result-item {
      display: flex; align-items: center; gap: 12px;
      padding: 10px 20px; cursor: pointer; text-decoration: none;
      color: var(--dentis-text); transition: background .12s;
    }
    .result-item:hover { background: rgba(13,148,136,.06); }
    .result-icon {
      width: 36px; height: 36px; border-radius: 10px; flex-shrink: 0;
      background: rgba(13,148,136,.10); display: flex; align-items: center; justify-content: center;
      color: var(--dentis-primary);
    }
    .result-icon.doc-icon {
      background: rgba(99,102,241,.10);
      color: #6366f1;
    }
    .result-copy { flex: 1; display: flex; flex-direction: column; }
    .result-label { font-size: 14px; font-weight: 600; }
    .result-sub { font-size: 12px; color: var(--dentis-text-muted); margin-top: 1px; }
    .result-arrow { color: var(--dentis-text-muted); font-size: 18px !important; }

    .search-hints {
      display: flex; gap: 24px; justify-content: center;
      padding: 16px 20px; border-top: 1px solid var(--dentis-border);
      color: var(--dentis-text-muted); font-size: 12px;
    }
    .hint-item { display: flex; align-items: center; gap: 5px; }
    kbd {
      font-size: 11px; padding: 2px 6px; border-radius: 4px;
      background: var(--dentis-surface-alt); border: 1px solid var(--dentis-border);
      font-family: var(--dentis-font-ui);
    }
  `],
})
export class GlobalSearchComponent implements OnInit, OnDestroy {
  open = signal(false);
  query = '';
  results = signal<SearchResult[]>([]);
  patientResults = signal<SearchResult[]>([]);
  documentResults = signal<SearchResult[]>([]);
  loading = signal(false);

  private search$ = new Subject<string>();
  private patientSvc = inject(PatientService);
  private docsSvc = inject(DocumentsService);

  @HostListener('document:keydown', ['$event'])
  onKeydown(e: KeyboardEvent): void {
    const isK = e.key === 'k' || e.key === 'K';
    const isMeta = e.metaKey || e.ctrlKey;
    if (isK && isMeta) { e.preventDefault(); this.toggle(); return; }
    if (e.key === 'Escape' && this.open()) { this.close(); }
  }

  ngOnInit(): void {
    const emptyPage: PageResponse<Patient> = { content: [], page: 0, size: 0, totalElements: 0, totalPages: 0, last: true };

    this.search$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((term) => {
        if (term.length < 2) { this.loading.set(false); return of({ patients: emptyPage, documents: [] as any[] }); }
        this.loading.set(true);
        return forkJoin({
          patients: this.patientSvc.search(term, 0, 6).pipe(catchError(() => of(emptyPage))),
          documents: this.docsSvc.search(term).pipe(catchError(() => of([])))
        });
      })
    ).subscribe(({ patients, documents }) => {
      this.loading.set(false);
      const pResults: SearchResult[] = patients.content.map((p) => ({
        type: 'patient' as const,
        label: `${p.lastName}, ${p.firstName}`,
        sublabel: p.idDocument,
        route: ['/patients', p.id],
        icon: 'person',
      }));
      const dResults: SearchResult[] = (documents as any[]).map((d) => ({
        type: 'document' as const,
        label: d.name,
        sublabel: d.folderPath ?? 'Documentos',
        route: ['/documents'],
        icon: 'insert_drive_file',
      }));
      this.patientResults.set(pResults);
      this.documentResults.set(dResults);
      this.results.set([...pResults, ...dResults]);
    });
  }

  ngOnDestroy(): void { this.search$.complete(); }

  onQuery(term: string): void { this.search$.next(term); }

  toggle(): void { this.open() ? this.close() : this.openSearch(); }

  openSearch(): void {
    this.open.set(true);
    this.clearQuery();
  }

  close(): void {
    this.open.set(false);
    this.clearQuery();
  }

  clearQuery(): void {
    this.query = '';
    this.results.set([]);
    this.patientResults.set([]);
    this.documentResults.set([]);
    this.loading.set(false);
  }
}
