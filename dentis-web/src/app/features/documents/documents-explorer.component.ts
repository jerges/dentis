import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { DocumentsService } from '../../core/services/documents.service';
import { AuthService } from '../../core/services/auth.service';
import {
  DocumentFolder, DocumentFile, ShareTarget, Visibility
} from '../../core/models/documents.model';

interface BreadcrumbItem { id: string | null; name: string; }

@Component({
  selector: 'app-documents-explorer',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatMenuModule,
    MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSnackBarModule, MatTooltipModule,
    MatChipsModule, MatProgressBarModule, MatCheckboxModule
  ],
  template: `
    <div class="docs-root">

      <!-- Header -->
      <div class="docs-header">
        <div class="docs-header-left">
          <mat-icon class="docs-header-icon">folder_open</mat-icon>
          <div>
            <h1 class="docs-title">Documentos</h1>
            <p class="docs-subtitle">Gestiona los documentos de tu clínica</p>
          </div>
        </div>
        <div class="docs-header-actions">
          <button mat-stroked-button (click)="openCreateFolderDialog()">
            <mat-icon>create_new_folder</mat-icon> Nueva carpeta
          </button>
          <button mat-raised-button color="primary" (click)="triggerFileInput()">
            <mat-icon>upload_file</mat-icon> Subir fichero
          </button>
          <input #fileInput type="file" hidden (change)="onFileSelected($event)" multiple />
        </div>
      </div>

      <!-- Buscador inline -->
      <div class="docs-search-bar">
        <mat-icon class="search-icon">search</mat-icon>
        <input class="search-input" placeholder="Buscar documentos…"
               [(ngModel)]="searchQuery" (input)="onSearch()" />
        @if (searchQuery) {
          <button mat-icon-button (click)="clearSearch()"><mat-icon>close</mat-icon></button>
        }
      </div>

      <!-- Resultados de búsqueda -->
      @if (searchQuery && searchResults().length > 0) {
        <div class="search-results">
          <p class="search-label">Resultados para "{{ searchQuery }}"</p>
          <div class="items-grid">
            @for (r of searchResults(); track r.id) {
              <div class="doc-card" (click)="navigateToFolder(r.folderId)">
                <mat-icon class="card-icon">insert_drive_file</mat-icon>
                <span class="card-name">{{ r.name }}</span>
                <span class="card-meta">{{ r.folderPath }}</span>
              </div>
            }
          </div>
        </div>
      }

      @if (!searchQuery) {
        <!-- Breadcrumb -->
        <nav class="breadcrumb">
          @for (crumb of breadcrumb(); track crumb.id; let last = $last) {
            <span class="crumb" [class.crumb-active]="last"
                  (click)="!last && navigateTo(crumb.id)">
              {{ crumb.name }}
            </span>
            @if (!last) { <mat-icon class="crumb-sep">chevron_right</mat-icon> }
          }
        </nav>

        <!-- Loading bar -->
        @if (loading()) {
          <mat-progress-bar mode="indeterminate" />
        }

        <!-- Contenido: carpetas y ficheros -->
        <div class="docs-content">

          <!-- Carpetas -->
          @if (folders().length > 0) {
            <section>
              <p class="section-label">Carpetas</p>
              <div class="items-grid">
                @for (folder of folders(); track folder.id) {
                  <div class="doc-card" [class.card-kb]="folder.type === 'KNOWLEDGE_BASE'"
                       (click)="openFolder(folder)"
                       (contextmenu)="$event.preventDefault()">
                    <div class="card-top">
                      <mat-icon class="card-icon" [class.icon-kb]="folder.type === 'KNOWLEDGE_BASE'">
                        {{ folder.type === 'KNOWLEDGE_BASE' ? 'psychology' : 'folder' }}
                      </mat-icon>
                      @if (folder.type === 'KNOWLEDGE_BASE') {
                        <span class="badge-kb">IA</span>
                      }
                      @if (!folder.system) {
                        <button mat-icon-button class="card-menu-btn"
                                [matMenuTriggerFor]="folderMenu"
                                [matMenuTriggerData]="{ folder: folder }"
                                (click)="$event.stopPropagation()">
                          <mat-icon>more_vert</mat-icon>
                        </button>
                      }
                    </div>
                    <span class="card-name">{{ folder.name }}</span>
                    <div class="card-footer">
                      <mat-icon class="vis-icon">{{ visIcon(folder.visibility) }}</mat-icon>
                      <span class="vis-label">{{ visLabel(folder.visibility) }}</span>
                    </div>
                  </div>
                }
              </div>
            </section>
          }

          <!-- Ficheros -->
          @if (files().length > 0) {
            <section>
              <p class="section-label">Ficheros</p>
              <div class="items-grid">
                @for (file of files(); track file.id) {
                  <div class="doc-card">
                    <div class="card-top">
                      <mat-icon class="card-icon icon-file">{{ fileIcon(file.contentType) }}</mat-icon>
                      @if (file.indexedForIa) {
                        <span class="badge-ia" matTooltip="Indexado en la IA">IA</span>
                      }
                      <button mat-icon-button class="card-menu-btn"
                              [matMenuTriggerFor]="fileMenu"
                              [matMenuTriggerData]="{ file: file }"
                              (click)="$event.stopPropagation()">
                        <mat-icon>more_vert</mat-icon>
                      </button>
                    </div>
                    <span class="card-name">{{ file.name }}</span>
                    <div class="card-footer">
                      <mat-icon class="vis-icon">{{ visIcon(file.visibility) }}</mat-icon>
                      <span class="vis-label">{{ visLabel(file.visibility) }}</span>
                      <span class="file-size">{{ formatSize(file.fileSize) }}</span>
                    </div>
                  </div>
                }
              </div>
            </section>
          }

          @if (folders().length === 0 && files().length === 0 && !loading()) {
            <div class="empty-state">
              <mat-icon>folder_open</mat-icon>
              <p>Esta carpeta está vacía</p>
              <button mat-stroked-button (click)="openCreateFolderDialog()">Crear carpeta</button>
            </div>
          }
        </div>
      }

      <!-- Upload progress -->
      @if (uploading()) {
        <div class="upload-toast">
          <mat-icon>cloud_upload</mat-icon>
          <span>Subiendo {{ uploadingName() }}…</span>
          <mat-progress-bar mode="indeterminate" style="flex:1" />
        </div>
      }
    </div>

    <!-- Menú carpeta -->
    <mat-menu #folderMenu="matMenu">
      <ng-template matMenuContent let-folder="folder">
        <button mat-menu-item (click)="openRenameDialog(folder)">
          <mat-icon>drive_file_rename_outline</mat-icon> Renombrar
        </button>
        <button mat-menu-item (click)="openVisibilityDialog(folder, null)">
          <mat-icon>visibility</mat-icon> Cambiar visibilidad
        </button>
        <button mat-menu-item (click)="openShareDialog('FOLDER', folder.id)">
          <mat-icon>share</mat-icon> Compartir
        </button>
        <button mat-menu-item class="delete-item" (click)="deleteFolder(folder)">
          <mat-icon>delete_outline</mat-icon> Eliminar
        </button>
      </ng-template>
    </mat-menu>

    <!-- Menú fichero -->
    <mat-menu #fileMenu="matMenu">
      <ng-template matMenuContent let-file="file">
        <button mat-menu-item (click)="downloadFile(file)">
          <mat-icon>download</mat-icon> Descargar
        </button>
        <button mat-menu-item (click)="openVisibilityDialog(null, file)">
          <mat-icon>visibility</mat-icon> Cambiar visibilidad
        </button>
        <button mat-menu-item (click)="openShareDialog('FILE', file.id)">
          <mat-icon>share</mat-icon> Compartir
        </button>
        <button mat-menu-item class="delete-item" (click)="deleteFile(file)">
          <mat-icon>delete_outline</mat-icon> Eliminar
        </button>
      </ng-template>
    </mat-menu>

    <!-- Diálogo: crear carpeta -->
    @if (showCreateFolder()) {
      <div class="dialog-backdrop" (click)="showCreateFolder.set(false)">
        <div class="dialog-panel" (click)="$event.stopPropagation()">
          <h2 class="dialog-title">Nueva carpeta</h2>
          <form [formGroup]="folderForm" (ngSubmit)="submitCreateFolder()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="name" placeholder="Ej: Protocolos 2026" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Visibilidad</mat-label>
              <mat-select formControlName="visibility">
                <mat-option value="PUBLIC">Pública (todos en la clínica)</mat-option>
                <mat-option value="PRIVATE">Privada (solo yo)</mat-option>
                <mat-option value="SHARED">Compartida</mat-option>
              </mat-select>
            </mat-form-field>
            <div class="dialog-actions">
              <button type="button" mat-stroked-button (click)="showCreateFolder.set(false)">Cancelar</button>
              <button type="submit" mat-raised-button color="primary"
                      [disabled]="folderForm.invalid || saving()">Crear</button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- Diálogo: renombrar -->
    @if (showRename()) {
      <div class="dialog-backdrop" (click)="showRename.set(false)">
        <div class="dialog-panel" (click)="$event.stopPropagation()">
          <h2 class="dialog-title">Renombrar</h2>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Nuevo nombre</mat-label>
            <input matInput [(ngModel)]="renameValue" />
          </mat-form-field>
          <div class="dialog-actions">
            <button mat-stroked-button (click)="showRename.set(false)">Cancelar</button>
            <button mat-raised-button color="primary" (click)="submitRename()">Guardar</button>
          </div>
        </div>
      </div>
    }

    <!-- Diálogo: visibilidad -->
    @if (showVisibility()) {
      <div class="dialog-backdrop" (click)="showVisibility.set(false)">
        <div class="dialog-panel" (click)="$event.stopPropagation()">
          <h2 class="dialog-title">Cambiar visibilidad</h2>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Visibilidad</mat-label>
            <mat-select [(ngModel)]="visibilityValue">
              <mat-option value="PUBLIC">Pública (todos en la clínica)</mat-option>
              <mat-option value="PRIVATE">Privada (solo yo)</mat-option>
              <mat-option value="SHARED">Compartida</mat-option>
            </mat-select>
          </mat-form-field>
          <div class="dialog-actions">
            <button mat-stroked-button (click)="showVisibility.set(false)">Cancelar</button>
            <button mat-raised-button color="primary" (click)="submitVisibility()">Guardar</button>
          </div>
        </div>
      </div>
    }

    <!-- Diálogo: compartir -->
    @if (showShare()) {
      <div class="dialog-backdrop" (click)="showShare.set(false)">
        <div class="dialog-panel" (click)="$event.stopPropagation()">
          <h2 class="dialog-title">Compartir con…</h2>
          @if (shareTargets().length === 0) {
            <p class="empty-share">No hay otros usuarios en la clínica.</p>
          }
          <div class="share-targets">
            @for (t of shareTargets(); track t.id) {
              <label class="share-target-row">
                <mat-checkbox [checked]="isSharedWith(t.id)"
                              (change)="toggleShare(t, $event.checked)" />
                <div class="share-target-info">
                  <span class="share-name">{{ t.fullName }}</span>
                  <span class="share-user">{{ '@' + t.username }}</span>
                </div>
              </label>
            }
          </div>
          <div class="dialog-actions">
            <button mat-raised-button color="primary" (click)="showShare.set(false)">Listo</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .docs-root {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
      min-height: 100%;
    }
    .docs-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      margin-bottom: 20px;
      gap: 16px;
    }
    .docs-header-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .docs-header-icon {
      font-size: 36px;
      width: 36px;
      height: 36px;
      color: var(--dentis-primary);
    }
    .docs-title {
      font-family: var(--dentis-font-display);
      font-size: var(--fs-xl);
      font-weight: 700;
      color: var(--dentis-text);
      margin: 0;
    }
    .docs-subtitle {
      font-size: var(--fs-sm);
      color: var(--dentis-text-muted);
      margin: 2px 0 0;
    }
    .docs-header-actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }

    .docs-search-bar {
      display: flex;
      align-items: center;
      background: var(--dentis-surface);
      border: 1px solid var(--dentis-border);
      border-radius: 10px;
      padding: 8px 14px;
      margin-bottom: 20px;
      gap: 10px;
      box-shadow: 0 1px 4px rgba(0,0,0,.06);
    }
    .search-icon { color: var(--dentis-text-muted); }
    .search-input {
      flex: 1;
      border: none;
      outline: none;
      font-size: var(--fs-base);
      background: transparent;
      color: var(--dentis-text);
    }
    .search-results { margin-bottom: 24px; }
    .search-label {
      font-size: var(--fs-sm);
      color: var(--dentis-text-muted);
      margin-bottom: 12px;
    }

    .breadcrumb {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }
    .crumb {
      font-size: var(--fs-sm);
      color: var(--dentis-primary);
      cursor: pointer;
      padding: 2px 6px;
      border-radius: 4px;
      transition: background .15s;
    }
    .crumb:hover { background: var(--dentis-surface-alt); }
    .crumb.crumb-active {
      color: var(--dentis-text);
      cursor: default;
      font-weight: 600;
    }
    .crumb-sep { font-size: 18px; color: var(--dentis-text-muted); }

    .section-label {
      font-size: var(--fs-sm);
      font-weight: 600;
      color: var(--dentis-text-muted);
      text-transform: uppercase;
      letter-spacing: .06em;
      margin: 0 0 12px;
    }
    section { margin-bottom: 28px; }

    .items-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 14px;
    }

    .doc-card {
      position: relative;
      background: var(--dentis-surface);
      border: 1.5px solid var(--dentis-border);
      border-radius: 12px;
      padding: 16px 14px 12px;
      cursor: pointer;
      transition: border-color .2s, box-shadow .2s, transform .1s;
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-height: 120px;
    }
    .doc-card:hover {
      border-color: var(--dentis-primary);
      box-shadow: 0 4px 16px rgba(13,148,136,.12);
      transform: translateY(-2px);
    }
    .doc-card.card-kb {
      border-color: var(--dentis-accent);
      background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
    }
    .card-top {
      display: flex;
      align-items: flex-start;
      gap: 6px;
    }
    .card-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: var(--dentis-primary);
    }
    .icon-kb { color: var(--dentis-accent); }
    .icon-file { color: #6366f1; }
    .card-menu-btn {
      margin-left: auto;
      width: 28px;
      height: 28px;
      line-height: 28px;
    }
    .card-menu-btn mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .card-name {
      font-size: var(--fs-sm);
      font-weight: 600;
      color: var(--dentis-text);
      word-break: break-word;
      line-height: 1.3;
    }
    .card-footer {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-top: auto;
    }
    .card-meta {
      font-size: var(--fs-xs);
      color: var(--dentis-text-muted);
    }
    .vis-icon { font-size: 14px; width: 14px; height: 14px; color: var(--dentis-text-muted); }
    .vis-label { font-size: var(--fs-xs); color: var(--dentis-text-muted); }
    .file-size { font-size: var(--fs-xs); color: var(--dentis-text-muted); margin-left: auto; }

    .badge-kb {
      background: var(--dentis-accent);
      color: #fff;
      font-size: 10px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 6px;
    }
    .badge-ia {
      background: var(--dentis-primary);
      color: #fff;
      font-size: 10px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 6px;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 60px 20px;
      color: var(--dentis-text-muted);
    }
    .empty-state mat-icon { font-size: 64px; width: 64px; height: 64px; opacity: .4; }
    .empty-state p { font-size: var(--fs-md); margin: 0; }

    .upload-toast {
      position: fixed;
      bottom: 24px;
      right: 24px;
      background: var(--dentis-text);
      color: #fff;
      padding: 12px 20px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 280px;
      box-shadow: 0 8px 24px rgba(0,0,0,.2);
      z-index: 1000;
    }

    /* Diálogos inline */
    .dialog-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,.4);
      z-index: 900;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .dialog-panel {
      background: var(--dentis-surface);
      border-radius: 16px;
      padding: 28px 32px;
      min-width: 360px;
      max-width: 480px;
      width: 100%;
      box-shadow: 0 24px 48px rgba(0,0,0,.2);
    }
    .dialog-title {
      font-size: var(--fs-lg);
      font-weight: 700;
      margin: 0 0 20px;
      color: var(--dentis-text);
    }
    .dialog-actions {
      display: flex;
      gap: 10px;
      justify-content: flex-end;
      margin-top: 20px;
    }
    .full-width { width: 100%; }

    .share-targets { display: flex; flex-direction: column; gap: 10px; max-height: 280px; overflow-y: auto; }
    .share-target-row { display: flex; align-items: center; gap: 12px; padding: 8px; border-radius: 8px; cursor: pointer; }
    .share-target-row:hover { background: var(--dentis-surface-alt); }
    .share-target-info { display: flex; flex-direction: column; }
    .share-name { font-size: var(--fs-base); font-weight: 500; }
    .share-user { font-size: var(--fs-xs); color: var(--dentis-text-muted); }
    .empty-share { color: var(--dentis-text-muted); text-align: center; padding: 20px 0; }
    .delete-item { color: var(--dentis-warn) !important; }
  `]
})
export class DocumentsExplorerComponent implements OnInit {

  private readonly docsSvc = inject(DocumentsService);
  private readonly auth = inject(AuthService);
  private readonly snack = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  // State
  folders = signal<DocumentFolder[]>([]);
  files = signal<DocumentFile[]>([]);
  loading = signal(false);
  saving = signal(false);
  uploading = signal(false);
  uploadingName = signal('');
  breadcrumb = signal<BreadcrumbItem[]>([{ id: null, name: 'Inicio' }]);
  currentFolderId = signal<string | null>(null);
  searchQuery = '';
  searchResults = signal<any[]>([]);

  // Dialog state
  showCreateFolder = signal(false);
  showRename = signal(false);
  showVisibility = signal(false);
  showShare = signal(false);

  // Dialog data
  renameValue = '';
  visibilityValue: Visibility = 'PRIVATE';
  shareTargets = signal<ShareTarget[]>([]);
  activeShareResourceType = signal<'FOLDER' | 'FILE'>('FOLDER');
  activeShareResourceId = signal('');
  activeSharedWith = signal<string[]>([]);
  renamingFolderId = signal<string | null>(null);
  visibilityFolderId = signal<string | null>(null);
  visibilityFileId = signal<string | null>(null);

  // Form
  folderForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    visibility: ['PRIVATE' as Visibility]
  });

  ngOnInit() {
    this.loadContent(null);
  }

  loadContent(parentId: string | null) {
    this.loading.set(true);
    this.currentFolderId.set(parentId);
    this.docsSvc.listFolders(undefined, parentId ?? undefined).subscribe({
      next: (f) => this.folders.set(f),
      error: () => this.snack.open('Error al cargar carpetas', 'OK', { duration: 3000 })
    });
    if (parentId) {
      this.docsSvc.listFiles(parentId).subscribe({
        next: (f) => { this.files.set(f); this.loading.set(false); },
        error: () => { this.loading.set(false); }
      });
    } else {
      this.files.set([]);
      this.loading.set(false);
    }
  }

  openFolder(folder: DocumentFolder) {
    const crumbs = this.breadcrumb();
    this.breadcrumb.set([...crumbs, { id: folder.id, name: folder.name }]);
    this.loadContent(folder.id);
  }

  navigateTo(id: string | null) {
    const crumbs = this.breadcrumb();
    const idx = crumbs.findIndex(c => c.id === id);
    this.breadcrumb.set(crumbs.slice(0, idx + 1));
    this.loadContent(id);
  }

  navigateToFolder(folderId: string) {
    this.clearSearch();
    this.loadContent(folderId);
  }

  onSearch() {
    if (this.searchQuery.length < 2) { this.searchResults.set([]); return; }
    this.docsSvc.search(this.searchQuery).subscribe({
      next: (r) => this.searchResults.set(r),
      error: () => {}
    });
  }

  clearSearch() {
    this.searchQuery = '';
    this.searchResults.set([]);
  }

  // ── Folders ──────────────────────────────────────────────────────────────

  openCreateFolderDialog() {
    this.folderForm.reset({ name: '', visibility: 'PRIVATE' });
    this.showCreateFolder.set(true);
  }

  submitCreateFolder() {
    if (this.folderForm.invalid) return;
    this.saving.set(true);
    const { name, visibility } = this.folderForm.getRawValue();
    this.docsSvc.createFolder({
      name: name!,
      parentId: this.currentFolderId() ?? undefined,
      visibility: visibility as Visibility
    }).subscribe({
      next: (f) => {
        this.folders.update(list => [...list, f]);
        this.showCreateFolder.set(false);
        this.saving.set(false);
        this.snack.open('Carpeta creada', 'OK', { duration: 2500 });
      },
      error: () => { this.saving.set(false); this.snack.open('Error al crear carpeta', 'OK', { duration: 3000 }); }
    });
  }

  openRenameDialog(folder: DocumentFolder) {
    this.renameValue = folder.name;
    this.renamingFolderId.set(folder.id);
    this.showRename.set(true);
  }

  submitRename() {
    const id = this.renamingFolderId();
    if (!id || !this.renameValue.trim()) return;
    this.docsSvc.updateFolder(id, { name: this.renameValue }).subscribe({
      next: (f) => {
        this.folders.update(list => list.map(x => x.id === id ? f : x));
        this.showRename.set(false);
        this.snack.open('Renombrado', 'OK', { duration: 2500 });
      },
      error: () => this.snack.open('Error', 'OK', { duration: 3000 })
    });
  }

  deleteFolder(folder: DocumentFolder) {
    if (!confirm(`¿Eliminar la carpeta "${folder.name}" y todo su contenido?`)) return;
    this.docsSvc.deleteFolder(folder.id).subscribe({
      next: () => {
        this.folders.update(list => list.filter(f => f.id !== folder.id));
        this.snack.open('Carpeta eliminada', 'OK', { duration: 2500 });
      },
      error: (e) => this.snack.open(e?.error?.error?.message ?? 'Error', 'OK', { duration: 3500 })
    });
  }

  // ── Files ────────────────────────────────────────────────────────────────

  triggerFileInput() {
    document.querySelector<HTMLInputElement>('input[type=file]')?.click();
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const folderId = this.currentFolderId();
    if (!folderId) {
      this.snack.open('Abre una carpeta antes de subir ficheros', 'OK', { duration: 3000 });
      return;
    }
    Array.from(input.files).forEach(file => this.uploadFile(file, folderId));
    input.value = '';
  }

  private uploadFile(file: File, folderId: string) {
    this.uploading.set(true);
    this.uploadingName.set(file.name);
    this.docsSvc.presignUpload(folderId, file.name, file.type).subscribe({
      next: ({ s3Key, uploadUrl }) => {
        this.http.put(uploadUrl, file, {
          headers: { 'Content-Type': file.type }
        }).subscribe({
          next: () => {
            const currentFolder = [...this.breadcrumb()].at(-1);
            const folderVisibility = this.folders().find(f => f.id === folderId)?.visibility ?? 'PRIVATE';
            this.docsSvc.registerFile({
              folderId,
              name: file.name,
              fileName: file.name,
              contentType: file.type,
              fileSize: file.size,
              s3Key,
              visibility: folderVisibility
            }).subscribe({
              next: (f) => {
                this.files.update(list => [...list, f]);
                this.uploading.set(false);
                this.snack.open(`"${file.name}" subido`, 'OK', { duration: 2500 });
              },
              error: () => { this.uploading.set(false); this.snack.open('Error al registrar fichero', 'OK', { duration: 3000 }); }
            });
          },
          error: () => { this.uploading.set(false); this.snack.open('Error al subir a S3', 'OK', { duration: 3000 }); }
        });
      },
      error: () => { this.uploading.set(false); this.snack.open('Error al obtener URL de subida', 'OK', { duration: 3000 }); }
    });
  }

  downloadFile(file: DocumentFile) {
    this.docsSvc.downloadUrl(file.id).subscribe({
      next: (url) => window.open(url, '_blank'),
      error: () => this.snack.open('Error al descargar', 'OK', { duration: 3000 })
    });
  }

  deleteFile(file: DocumentFile) {
    if (!confirm(`¿Eliminar "${file.name}"?`)) return;
    this.docsSvc.deleteFile(file.id).subscribe({
      next: () => {
        this.files.update(list => list.filter(f => f.id !== file.id));
        this.snack.open('Fichero eliminado', 'OK', { duration: 2500 });
      },
      error: () => this.snack.open('Error', 'OK', { duration: 3000 })
    });
  }

  // ── Visibility ────────────────────────────────────────────────────────────

  openVisibilityDialog(folder: DocumentFolder | null, file: DocumentFile | null) {
    this.visibilityValue = (folder?.visibility ?? file?.visibility ?? 'PRIVATE');
    this.visibilityFolderId.set(folder?.id ?? null);
    this.visibilityFileId.set(file?.id ?? null);
    this.showVisibility.set(true);
  }

  submitVisibility() {
    const fid = this.visibilityFolderId();
    const fileid = this.visibilityFileId();
    if (fid) {
      this.docsSvc.updateFolder(fid, { visibility: this.visibilityValue }).subscribe({
        next: (f) => { this.folders.update(list => list.map(x => x.id === fid ? f : x)); this.showVisibility.set(false); }
      });
    } else if (fileid) {
      this.docsSvc.updateFile(fileid, { visibility: this.visibilityValue }).subscribe({
        next: (f) => { this.files.update(list => list.map(x => x.id === fileid ? f : x)); this.showVisibility.set(false); }
      });
    }
  }

  // ── Share ─────────────────────────────────────────────────────────────────

  openShareDialog(type: 'FOLDER' | 'FILE', resourceId: string) {
    this.activeShareResourceType.set(type);
    this.activeShareResourceId.set(resourceId);
    this.activeSharedWith.set([]);
    this.docsSvc.shareTargets().subscribe({
      next: (t) => { this.shareTargets.set(t); this.showShare.set(true); },
      error: () => this.snack.open('Error al cargar usuarios', 'OK', { duration: 3000 })
    });
  }

  isSharedWith(userId: string): boolean {
    return this.activeSharedWith().includes(userId);
  }

  toggleShare(target: ShareTarget, checked: boolean) {
    const type = this.activeShareResourceType();
    const resourceId = this.activeShareResourceId();
    if (checked) {
      this.docsSvc.share(type, resourceId, target.id).subscribe({
        next: () => this.activeSharedWith.update(list => [...list, target.id]),
        error: () => this.snack.open('Error al compartir', 'OK', { duration: 3000 })
      });
    } else {
      // En una implementación completa buscaríamos el shareId; por ahora recargamos
      this.activeSharedWith.update(list => list.filter(id => id !== target.id));
    }
  }

  // ── Utils ─────────────────────────────────────────────────────────────────

  visIcon(v: Visibility): string {
    return v === 'PUBLIC' ? 'public' : v === 'SHARED' ? 'group' : 'lock';
  }

  visLabel(v: Visibility): string {
    return v === 'PUBLIC' ? 'Pública' : v === 'SHARED' ? 'Compartida' : 'Privada';
  }

  fileIcon(ct: string): string {
    if (!ct) return 'insert_drive_file';
    if (ct.startsWith('image/')) return 'image';
    if (ct === 'application/pdf') return 'picture_as_pdf';
    if (ct.includes('spreadsheet') || ct.includes('excel')) return 'table_chart';
    if (ct.includes('word') || ct.includes('document')) return 'description';
    return 'insert_drive_file';
  }

  formatSize(bytes: number | null): string {
    if (!bytes) return '';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }
}
