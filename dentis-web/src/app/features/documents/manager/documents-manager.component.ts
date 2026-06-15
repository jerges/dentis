import {
  Component, OnInit, signal, computed, ViewChild, ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTreeModule, MatTreeNestedDataSource } from '@angular/material/tree';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatMenuModule } from '@angular/material/menu';
import { MatTabsModule } from '@angular/material/tabs';
import { NestedTreeControl } from '@angular/cdk/tree';
import { debounceTime, distinctUntilChanged, Subject, switchMap } from 'rxjs';

import { DocumentsService } from '../../../core/services/documents.service';
import { AuthService } from '../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ClinicDocument, DocumentFolder, DocumentVisibility, DocumentZone } from '../../../core/models/documents.model';

interface FolderNode extends DocumentFolder {
  children?: FolderNode[];
  loading?: boolean;
}

@Component({
  selector: 'app-documents-manager',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule, MatTreeModule,
    MatProgressBarModule, MatProgressSpinnerModule,
    MatTooltipModule, MatSnackBarModule, MatDialogModule,
    MatChipsModule, MatDividerModule, MatMenuModule, MatTabsModule,
    PageHeaderComponent
  ],
  template: `
<div class="page-container">
  <app-page-header
    title="Documentos"
    subtitle="Gestión documental y base de conocimiento IA">
    <div class="header-actions">
      <!-- Search bar -->
      <mat-form-field appearance="outline" class="search-field">
        <mat-label>Buscar documentos…</mat-label>
        <input matInput [(ngModel)]="searchQuery" (ngModelChange)="onSearch($event)" />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>
    </div>
  </app-page-header>

  <!-- Search results overlay -->
  @if (searchQuery().length > 1) {
    <mat-card class="search-results-card">
      <mat-card-header>
        <mat-card-title>Resultados de búsqueda</mat-card-title>
        <button mat-icon-button (click)="clearSearch()" class="close-search">
          <mat-icon>close</mat-icon>
        </button>
      </mat-card-header>
      <mat-card-content>
        @if (searchLoading()) {
          <mat-progress-bar mode="indeterminate" />
        } @else if (searchResults().length === 0) {
          <p class="empty-hint">Sin resultados para "{{ searchQuery() }}"</p>
        } @else {
          <div class="doc-grid">
            @for (doc of searchResults(); track doc.id) {
              <div class="doc-card" (click)="downloadDoc(doc)">
                <mat-icon class="doc-icon" [class.indexed]="doc.indexedForIa">
                  {{ fileIcon(doc.contentType) }}
                </mat-icon>
                <div class="doc-info">
                  <span class="doc-name">{{ doc.fileName }}</span>
                  @if (doc.indexedForIa) {
                    <mat-chip class="ia-chip">KB IA</mat-chip>
                  }
                </div>
              </div>
            }
          </div>
        }
      </mat-card-content>
    </mat-card>
  }

  @if (!searchQuery().length) {
    <div class="docs-layout">
      <!-- Sidebar: folder tree -->
      <mat-card class="folders-panel">
        <mat-card-header>
          <mat-card-title>Carpetas</mat-card-title>
          <div class="folder-actions">
            @if (canManageKb()) {
              <button mat-icon-button matTooltip="Inicializar base de conocimiento IA"
                      (click)="initKb()">
                <mat-icon>psychology</mat-icon>
              </button>
            }
            <button mat-icon-button matTooltip="Nueva carpeta" (click)="openNewFolderDialog()">
              <mat-icon>create_new_folder</mat-icon>
            </button>
          </div>
        </mat-card-header>
        <mat-card-content>
          @if (loadingFolders()) {
            <mat-progress-bar mode="indeterminate" />
          } @else if (rootFolders().length === 0) {
            <p class="empty-hint">Sin carpetas. Crea la primera.</p>
          } @else {
            <mat-tree [dataSource]="treeDataSource" [treeControl]="treeControl" class="folder-tree">
              <mat-tree-node *matTreeNodeDef="let node" matTreeNodePadding
                             [class.selected-folder]="selectedFolder()?.id === node.id"
                             (click)="selectFolder(node)">
                <button mat-icon-button disabled></button>
                <mat-icon class="folder-icon" [class.kb-folder]="node.zone === 'KNOWLEDGE_BASE'">
                  folder{{ node.zone === 'KNOWLEDGE_BASE' ? '_special' : '' }}
                </mat-icon>
                <span class="folder-name">{{ node.name }}</span>
                @if (node.system) {
                  <mat-icon class="lock-icon" matTooltip="Carpeta de sistema — no eliminable">lock</mat-icon>
                }
                @if (node.visibility === 'PRIVATE') {
                  <mat-icon class="private-icon" matTooltip="Carpeta privada — solo visible para ti">visibility_off</mat-icon>
                }
                @if (!node.system && canManageKb()) {
                  <button mat-icon-button class="folder-delete"
                          matTooltip="Eliminar carpeta"
                          (click)="$event.stopPropagation(); confirmDeleteFolder(node)">
                    <mat-icon>delete_outline</mat-icon>
                  </button>
                }
              </mat-tree-node>
              <mat-nested-tree-node *matTreeNodeDef="let node; when: hasChildren" matTreeNodePadding
                                    [class.selected-folder]="selectedFolder()?.id === node.id">
                <div class="mat-tree-node" (click)="selectFolder(node)">
                  <button mat-icon-button matTreeNodeToggle>
                    <mat-icon>{{ treeControl.isExpanded(node) ? 'expand_more' : 'chevron_right' }}</mat-icon>
                  </button>
                  <mat-icon class="folder-icon" [class.kb-folder]="node.zone === 'KNOWLEDGE_BASE'">
                    {{ treeControl.isExpanded(node) ? 'folder_open' : 'folder' }}
                  </mat-icon>
                  <span class="folder-name">{{ node.name }}</span>
                  @if (node.system) {
                    <mat-icon class="lock-icon" matTooltip="Carpeta de sistema">lock</mat-icon>
                  }
                </div>
                <div [class.hidden]="!treeControl.isExpanded(node)">
                  <ng-container matTreeNodeOutlet />
                </div>
              </mat-nested-tree-node>
            </mat-tree>
          }
        </mat-card-content>
      </mat-card>

      <!-- Main panel: document grid -->
      <mat-card class="documents-panel">
        <mat-card-header>
          <mat-card-title>
            @if (selectedFolder()) {
              <div class="breadcrumb">
                <mat-icon [class.kb-folder]="selectedFolder()!.zone === 'KNOWLEDGE_BASE'">
                  {{ selectedFolder()!.zone === 'KNOWLEDGE_BASE' ? 'psychology' : 'folder' }}
                </mat-icon>
                {{ selectedFolder()!.name }}
                @if (selectedFolder()!.zone === 'KNOWLEDGE_BASE') {
                  <mat-chip class="kb-badge">Base de Conocimiento IA</mat-chip>
                }
              </div>
            } @else {
              <span>Selecciona una carpeta</span>
            }
          </mat-card-title>
          @if (selectedFolder()) {
            <div class="doc-panel-actions">
              <select class="visibility-select" [(ngModel)]="uploadVisibility"
                      matTooltip="Visibilidad del documento a subir">
                <option value="PUBLIC">Público</option>
                <option value="PRIVATE">Privado</option>
              </select>
              <button mat-flat-button color="primary" (click)="triggerUpload()"
                      [disabled]="uploading()">
                <mat-icon>upload</mat-icon>
                Subir documento
              </button>
              <input #fileInput type="file" hidden accept=".pdf,.doc,.docx,.txt,.xlsx,.png,.jpg"
                     (change)="onFileSelected($event)" />
            </div>
          }
        </mat-card-header>

        <mat-card-content>
          @if (uploading()) {
            <div class="upload-progress">
              <mat-progress-bar mode="indeterminate" />
              <span>Subiendo {{ uploadFileName() }}…</span>
            </div>
          }

          @if (loadingDocs()) {
            <mat-progress-bar mode="indeterminate" />
          } @else if (!selectedFolder()) {
            <div class="empty-state">
              <mat-icon>folder_open</mat-icon>
              <p>Selecciona una carpeta del panel izquierdo para ver sus documentos.</p>
            </div>
          } @else if (documents().length === 0) {
            <div class="empty-state">
              <mat-icon>description</mat-icon>
              <p>Esta carpeta está vacía. Sube el primer documento.</p>
            </div>
          } @else {
            <div class="doc-grid">
              @for (doc of documents(); track doc.id) {
                <div class="doc-card">
                  <div class="doc-card-main" (click)="downloadDoc(doc)">
                    <mat-icon class="doc-icon" [class.indexed]="doc.indexedForIa">
                      {{ fileIcon(doc.contentType) }}
                    </mat-icon>
                    <div class="doc-info">
                      <span class="doc-name" matTooltip="{{ doc.fileName }}">{{ doc.fileName }}</span>
                      <span class="doc-meta">
                        {{ doc.fileSize ? formatSize(doc.fileSize) : '—' }} ·
                        {{ doc.uploadedAt | date:'dd/MM/yyyy' }}
                      </span>
                      @if (doc.indexedForIa) {
                        <mat-chip class="ia-chip" matTooltip="Indexado para el asistente IA">
                          <mat-icon>psychology</mat-icon> Indexado
                        </mat-chip>
                      }
                      @if (doc.visibility === 'PRIVATE') {
                        <mat-chip class="private-chip" matTooltip="Solo visible para ti">
                          <mat-icon>visibility_off</mat-icon> Privado
                        </mat-chip>
                      }
                    </div>
                  </div>
                  <button mat-icon-button [matMenuTriggerFor]="docMenu" class="doc-menu-btn">
                    <mat-icon>more_vert</mat-icon>
                  </button>
                  <mat-menu #docMenu>
                    <button mat-menu-item (click)="downloadDoc(doc)">
                      <mat-icon>download</mat-icon> Descargar
                    </button>
                    <button mat-menu-item (click)="confirmDeleteDoc(doc)" class="delete-item">
                      <mat-icon>delete_outline</mat-icon> Eliminar
                    </button>
                  </mat-menu>
                </div>
              }
            </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  }

  <!-- New Folder Dialog (inline) -->
  @if (showNewFolderDialog()) {
    <div class="modal-overlay" (click)="closeNewFolderDialog()">
      <mat-card class="modal-card" (click)="$event.stopPropagation()">
        <mat-card-header>
          <mat-card-title>Nueva carpeta</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Nombre de la carpeta</mat-label>
            <input matInput [(ngModel)]="newFolderName" (keyup.enter)="createFolder()" />
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Zona</mat-label>
            <select matNativeControl [(ngModel)]="newFolderZone">
              <option value="GENERAL">General</option>
              @if (canManageKb()) {
                <option value="KNOWLEDGE_BASE">Base de Conocimiento IA</option>
              }
            </select>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Visibilidad</mat-label>
            <select matNativeControl [(ngModel)]="newFolderVisibility">
              <option value="PUBLIC">Pública — visible para todo el personal de la clínica</option>
              <option value="PRIVATE">Privada — solo visible para ti</option>
            </select>
          </mat-form-field>
        </mat-card-content>
        <mat-card-actions align="end">
          <button mat-button (click)="closeNewFolderDialog()">Cancelar</button>
          <button mat-flat-button color="primary" (click)="createFolder()"
                  [disabled]="!newFolderName.trim()">
            Crear
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  }
</div>
  `,
  styles: [`
    .page-container { padding: 24px; }
    .header-actions { display: flex; align-items: center; gap: 12px; }
    .search-field { width: 320px; }
    .docs-layout { display: grid; grid-template-columns: 280px 1fr; gap: 16px; margin-top: 16px; }

    /* Folder tree */
    .folders-panel mat-card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 16px 0; }
    .folder-actions { display: flex; }
    .folder-tree { background: transparent; }
    .folder-name { margin-left: 4px; font-size: 14px; flex: 1; }
    .folder-icon { font-size: 20px; color: #f9a825; }
    .folder-icon.kb-folder { color: #7c4dff; }
    .lock-icon { font-size: 16px; color: #9e9e9e; margin-left: 4px; }
    .folder-delete { opacity: 0; transition: opacity .15s; }
    .mat-tree-node:hover .folder-delete { opacity: 1; }
    .selected-folder { background: rgba(98,0,234,.08); border-radius: 4px; }
    .hidden { display: none; }

    /* Document panel */
    .documents-panel mat-card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px; }
    .breadcrumb { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 500; }
    .doc-panel-actions { display: flex; gap: 8px; }
    .upload-progress { display: flex; flex-direction: column; gap: 4px; margin-bottom: 16px; font-size: 13px; color: #666; }

    /* Document grid */
    .doc-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; padding: 8px 0; }
    .doc-card { border: 1px solid #e0e0e0; border-radius: 8px; padding: 12px; display: flex; align-items: flex-start; gap: 8px; cursor: pointer; transition: box-shadow .15s, border-color .15s; }
    .doc-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.12); border-color: #7c4dff44; }
    .doc-card-main { display: flex; gap: 8px; flex: 1; min-width: 0; }
    .doc-icon { font-size: 32px; height: 32px; width: 32px; color: #757575; }
    .doc-icon.indexed { color: #7c4dff; }
    .doc-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
    .doc-name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .doc-meta { font-size: 11px; color: #9e9e9e; }
    .doc-menu-btn { flex-shrink: 0; opacity: 0; transition: opacity .15s; }
    .doc-card:hover .doc-menu-btn { opacity: 1; }

    /* Chips */
    .ia-chip { background: #ede7f6 !important; color: #7c4dff !important; font-size: 11px; height: 20px; }
    .kb-badge { background: #7c4dff !important; color: white !important; font-size: 11px; height: 22px; }
    .private-chip { background: #fce4ec !important; color: #c62828 !important; font-size: 11px; height: 20px; }
    .private-icon { font-size: 16px; color: #ef9a9a; margin-left: 4px; }
    .visibility-select { border: 1px solid #e0e0e0; border-radius: 4px; padding: 4px 8px; font-size: 13px; background: white; cursor: pointer; }

    /* Empty states */
    .empty-state { display: flex; flex-direction: column; align-items: center; padding: 48px 24px; color: #9e9e9e; gap: 12px; }
    .empty-state mat-icon { font-size: 48px; height: 48px; width: 48px; }
    .empty-hint { color: #9e9e9e; font-size: 13px; padding: 8px 0; }

    /* Search results */
    .search-results-card { margin-top: 16px; }
    .search-results-card mat-card-header { display: flex; justify-content: space-between; align-items: center; }
    .close-search { margin-left: auto; }

    /* Modal */
    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-card { width: 400px; }
    .full-width { width: 100%; }
    .delete-item { color: #f44336; }
  `]
})
export class DocumentsManagerComponent implements OnInit {

  // Signals
  rootFolders = signal<FolderNode[]>([]);
  selectedFolder = signal<DocumentFolder | null>(null);
  documents = signal<ClinicDocument[]>([]);
  loadingFolders = signal(false);
  loadingDocs = signal(false);
  uploading = signal(false);
  uploadFileName = signal('');
  searchQuery = signal('');
  searchResults = signal<ClinicDocument[]>([]);
  searchLoading = signal(false);
  showNewFolderDialog = signal(false);
  newFolderName = '';
  newFolderZone: DocumentZone = 'GENERAL';
  newFolderVisibility: DocumentVisibility = 'PUBLIC';
  uploadVisibility: DocumentVisibility = 'PUBLIC';

  canManageKb = computed(() => {
    const role = this.auth.currentUser()?.role;
    return role === 'SUPER_ADMIN' || role === 'ADMIN';
  });

  // Tree
  treeControl = new NestedTreeControl<FolderNode>(node => node.children ?? []);
  treeDataSource = new MatTreeNestedDataSource<FolderNode>();
  hasChildren = (_: number, node: FolderNode) => (node.children?.length ?? 0) > 0;

  private searchSubject = new Subject<string>();

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  constructor(
    private readonly svc: DocumentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadFolders();
    this.searchSubject.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      switchMap(q => {
        this.searchLoading.set(true);
        return this.svc.search(q);
      })
    ).subscribe({
      next: r => { this.searchResults.set(r); this.searchLoading.set(false); },
      error: () => this.searchLoading.set(false)
    });
  }

  loadFolders() {
    this.loadingFolders.set(true);
    this.svc.listFolders(null).subscribe({
      next: folders => {
        const nodes: FolderNode[] = folders.map(f => ({ ...f, children: [] }));
        this.rootFolders.set(nodes);
        this.treeDataSource.data = nodes;
        this.loadingFolders.set(false);
      },
      error: () => this.loadingFolders.set(false)
    });
  }

  selectFolder(folder: FolderNode) {
    this.selectedFolder.set(folder);
    this.loadDocuments(folder.id);
  }

  loadDocuments(folderId: string) {
    this.loadingDocs.set(true);
    this.svc.listDocuments(folderId).subscribe({
      next: docs => { this.documents.set(docs); this.loadingDocs.set(false); },
      error: () => this.loadingDocs.set(false)
    });
  }

  initKb() {
    this.svc.initKnowledgeBase().subscribe({
      next: () => { this.snack.open('Base de conocimiento inicializada', 'OK', { duration: 3000 }); this.loadFolders(); },
      error: e => this.snack.open('Error: ' + e.error?.message, 'Cerrar', { duration: 4000 })
    });
  }

  openNewFolderDialog() { this.showNewFolderDialog.set(true); }
  closeNewFolderDialog() {
    this.showNewFolderDialog.set(false);
    this.newFolderName = '';
    this.newFolderVisibility = 'PUBLIC';
  }

  createFolder() {
    if (!this.newFolderName.trim()) return;
    this.svc.createFolder({
      parentId: this.selectedFolder()?.id ?? null,
      name: this.newFolderName.trim(),
      zone: this.newFolderZone,
      visibility: this.newFolderVisibility
    }).subscribe({
      next: () => { this.closeNewFolderDialog(); this.loadFolders(); },
      error: e => this.snack.open('Error al crear carpeta: ' + e.error?.message, 'Cerrar', { duration: 4000 })
    });
  }

  confirmDeleteFolder(folder: FolderNode) {
    if (!confirm(`¿Eliminar carpeta "${folder.name}"? Se eliminarán todos sus documentos.`)) return;
    this.svc.deleteFolder(folder.id).subscribe({
      next: () => { this.loadFolders(); if (this.selectedFolder()?.id === folder.id) this.selectedFolder.set(null); },
      error: e => this.snack.open('Error: ' + e.error?.message, 'Cerrar', { duration: 4000 })
    });
  }

  triggerUpload() { this.fileInput.nativeElement.click(); }

  onFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file || !this.selectedFolder()) return;
    const folderId = this.selectedFolder()!.id;
    this.uploading.set(true);
    this.uploadFileName.set(file.name);
    this.svc.presignUpload({ folderId, fileName: file.name, contentType: file.type }).subscribe({
      next: ({ uploadUrl, s3Key }) => {
        this.svc.uploadToS3(uploadUrl, file).subscribe({
          next: () => this.svc.registerDocument({
            folderId, fileName: file.name, contentType: file.type,
            s3Key, fileSize: file.size, description: null,
            visibility: this.uploadVisibility
          }).subscribe({
            next: () => {
              this.uploading.set(false);
              this.snack.open('Documento subido correctamente', 'OK', { duration: 3000 });
              this.loadDocuments(folderId);
            },
            error: () => { this.uploading.set(false); this.snack.open('Error al registrar el documento', 'Cerrar', { duration: 4000 }); }
          }),
          error: () => { this.uploading.set(false); this.snack.open('Error al subir el archivo a S3', 'Cerrar', { duration: 4000 }); }
        });
      },
      error: () => { this.uploading.set(false); this.snack.open('Error al obtener URL de subida', 'Cerrar', { duration: 4000 }); }
    });
    (event.target as HTMLInputElement).value = '';
  }

  downloadDoc(doc: ClinicDocument) {
    this.svc.downloadUrl(doc.id).subscribe({
      next: ({ downloadUrl }) => window.open(downloadUrl, '_blank'),
      error: () => this.snack.open('Error al obtener enlace de descarga', 'Cerrar', { duration: 4000 })
    });
  }

  confirmDeleteDoc(doc: ClinicDocument) {
    if (!confirm(`¿Eliminar "${doc.fileName}"?`)) return;
    this.svc.deleteDocument(doc.id).subscribe({
      next: () => { this.snack.open('Documento eliminado', 'OK', { duration: 3000 }); this.loadDocuments(this.selectedFolder()!.id); },
      error: e => this.snack.open('Error: ' + e.error?.message, 'Cerrar', { duration: 4000 })
    });
  }

  onSearch(q: string) {
    this.searchQuery.set(q);
    if (q.length > 1) this.searchSubject.next(q);
    else this.searchResults.set([]);
  }

  clearSearch() { this.searchQuery.set(''); this.searchResults.set([]); }

  fileIcon(contentType: string): string {
    if (contentType?.includes('pdf')) return 'picture_as_pdf';
    if (contentType?.includes('image')) return 'image';
    if (contentType?.includes('spreadsheet') || contentType?.includes('excel')) return 'table_chart';
    if (contentType?.includes('word') || contentType?.includes('document')) return 'article';
    return 'insert_drive_file';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }
}
