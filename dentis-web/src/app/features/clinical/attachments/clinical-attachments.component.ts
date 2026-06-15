import { Component, OnInit, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ClinicalService } from '../../../core/services/clinical.service';
import { ClinicalAttachment } from '../../../core/models/clinical.model';
import { switchMap } from 'rxjs';

@Component({
  selector: 'app-clinical-attachments',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatProgressBarModule,
    MatSnackBarModule, MatChipsModule, MatTooltipModule
  ],
  template: `
    <div class="attachments-container">
      <div class="attach-header">
        <span class="attach-title">Imágenes y documentos clínicos</span>
        @if (toothNumber()) {
          <mat-chip class="tooth-chip">Diente {{ toothNumber() }}</mat-chip>
        }
        <span class="spacer"></span>
        <button mat-stroked-button (click)="fileInput.click()" [disabled]="uploading()">
          <mat-icon>upload_file</mat-icon> Subir archivo
        </button>
        <input #fileInput type="file" accept="image/*,application/pdf"
               style="display:none" (change)="onFileSelected($event)">
      </div>

      @if (uploading()) {
        <mat-progress-bar mode="indeterminate" class="upload-bar"></mat-progress-bar>
      }

      @if (loading()) {
        <p class="hint">Cargando...</p>
      } @else if (attachments().length === 0) {
        <div class="empty-state">
          <mat-icon class="empty-icon">folder_open</mat-icon>
          <p>Sin adjuntos{{ toothNumber() ? ' para este diente' : '' }}.</p>
          <p class="hint-sub">Sube radiografías, fotografías o documentos clínicos.</p>
        </div>
      } @else {
        <div class="attachments-grid">
          @for (a of attachments(); track a.id) {
            <div class="attach-card">
              @if (isImage(a.contentType)) {
                <div class="attach-thumb">
                  @if (a.downloadUrl) {
                    <img [src]="a.downloadUrl" [alt]="a.fileName" class="thumb-img"
                         (error)="onImgError($event)">
                  } @else {
                    <mat-icon class="thumb-icon">image</mat-icon>
                  }
                </div>
              } @else {
                <div class="attach-thumb attach-doc">
                  <mat-icon class="thumb-icon">picture_as_pdf</mat-icon>
                </div>
              }
              <div class="attach-info">
                <div class="attach-name" [matTooltip]="a.fileName">{{ a.fileName }}</div>
                @if (a.toothNumber) {
                  <div class="attach-tooth">Diente {{ a.toothNumber }}</div>
                }
                @if (a.description) {
                  <div class="attach-desc">{{ a.description }}</div>
                }
                <div class="attach-meta">
                  {{ formatSize(a.fileSize) }} · {{ a.uploadedAt | date:'dd/MM/yy' }}
                </div>
              </div>
              <div class="attach-actions">
                @if (a.downloadUrl) {
                  <a [href]="a.downloadUrl" target="_blank" mat-icon-button matTooltip="Ver">
                    <mat-icon>open_in_new</mat-icon>
                  </a>
                }
                <button mat-icon-button matTooltip="Eliminar"
                        (click)="deleteAttachment(a)" [disabled]="uploading()">
                  <mat-icon>delete_outline</mat-icon>
                </button>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .attachments-container { padding: 8px 0; }
    .attach-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
    .attach-title { font-weight: 600; font-size: 14px; }
    .tooth-chip { background: var(--dentis-primary-light, #e8e8fd); color: var(--dentis-primary); font-size: 11px; }
    .spacer { flex: 1; }
    .upload-bar { margin-bottom: 16px; border-radius: 2px; }
    .hint { color: var(--dentis-text-muted); font-size: 13px; text-align: center; padding: 24px; }
    .empty-state { text-align: center; padding: 40px 16px; color: var(--dentis-text-muted); }
    .empty-icon { font-size: 48px; width: 48px; height: 48px; opacity: 0.3; display: block; margin: 0 auto 12px; }
    .hint-sub { font-size: 12px; margin-top: 4px; }
    .attachments-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
    .attach-card { border: 1px solid var(--dentis-border, #e2e8f0); border-radius: 8px;
                   overflow: hidden; background: var(--dentis-surface-alt, #f8f9fa);
                   display: flex; flex-direction: column; }
    .attach-thumb { height: 110px; display: flex; align-items: center; justify-content: center;
                    background: var(--dentis-surface, #fff); overflow: hidden; }
    .attach-doc { background: #fff8f0; }
    .thumb-img { width: 100%; height: 100%; object-fit: cover; }
    .thumb-icon { font-size: 40px; width: 40px; height: 40px; color: #ccc; }
    .attach-info { padding: 8px; flex: 1; }
    .attach-name { font-size: 12px; font-weight: 600; white-space: nowrap; overflow: hidden;
                   text-overflow: ellipsis; }
    .attach-tooth { font-size: 10px; color: var(--dentis-primary); font-weight: 600; }
    .attach-desc { font-size: 11px; color: var(--dentis-text-muted); margin: 2px 0; }
    .attach-meta { font-size: 10px; color: var(--dentis-text-muted); }
    .attach-actions { display: flex; justify-content: flex-end; padding: 4px;
                      border-top: 1px solid var(--dentis-border, #e2e8f0); }
  `]
})
export class ClinicalAttachmentsComponent implements OnInit {
  patientId = input.required<string>();
  toothNumber = input<number | null>(null);

  attachments = signal<ClinicalAttachment[]>([]);
  loading = signal(true);
  uploading = signal(false);

  private svc = inject(ClinicalService);
  private snack = inject(MatSnackBar);

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.svc.listAttachments(this.patientId(), this.toothNumber() ?? undefined)
      .subscribe({
        next: (list) => { this.attachments.set(list); this.loading.set(false); },
        error: () => { this.loading.set(false); }
      });
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.upload(file);
    (event.target as HTMLInputElement).value = '';
  }

  private upload(file: File): void {
    this.uploading.set(true);
    this.svc.presignAttachmentUpload(this.patientId(), file.name, file.type)
      .pipe(
        switchMap(({ s3Key, uploadUrl }) =>
          this.svc.uploadToS3(uploadUrl, file).pipe(
            switchMap(() => this.svc.registerAttachment(this.patientId(), {
              s3Key,
              fileName: file.name,
              contentType: file.type,
              fileSize: file.size,
              toothNumber: this.toothNumber() ?? undefined,
            }))
          )
        )
      )
      .subscribe({
        next: (a) => {
          this.attachments.update(list => [a, ...list]);
          this.uploading.set(false);
          this.snack.open('Archivo subido correctamente', 'OK', { duration: 3000 });
        },
        error: () => {
          this.uploading.set(false);
          this.snack.open('Error al subir el archivo', 'Cerrar', { duration: 4000 });
        }
      });
  }

  deleteAttachment(a: ClinicalAttachment): void {
    this.svc.deleteAttachment(this.patientId(), a.id).subscribe({
      next: () => this.attachments.update(list => list.filter(x => x.id !== a.id)),
      error: () => this.snack.open('Error al eliminar', 'Cerrar', { duration: 3000 })
    });
  }

  isImage(contentType: string): boolean {
    return contentType.startsWith('image/');
  }

  formatSize(bytes?: number): string {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  onImgError(e: Event): void {
    (e.target as HTMLImageElement).style.display = 'none';
  }
}
