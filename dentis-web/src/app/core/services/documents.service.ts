import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';
import {
  ClinicDocument,
  CreateFolderRequest,
  DocumentFolder,
  PresignUploadRequest,
  RegisterDocumentRequest,
} from '../models/documents.model';

@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly base = `${environment.apiUrl}/api/v1/documents`;

  constructor(private readonly http: HttpClient) {}

  // -- Folders --

  listFolders(parentId?: string | null): Observable<DocumentFolder[]> {
    const params: Record<string, string> = {};
    if (parentId !== undefined && parentId !== null) params['parentId'] = parentId;
    return this.http.get<DocumentFolder[]>(`${this.base}/folders`, { params });
  }

  createFolder(req: CreateFolderRequest): Observable<DocumentFolder> {
    return this.http.post<DocumentFolder>(`${this.base}/folders`, req);
  }

  deleteFolder(folderId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/folders/${folderId}`);
  }

  initKnowledgeBase(): Observable<DocumentFolder> {
    return this.http.post<DocumentFolder>(`${this.base}/folders/kb/init`, {});
  }

  // -- Documents --

  presignUpload(req: PresignUploadRequest): Observable<{ uploadUrl: string; s3Key: string }> {
    return this.http.post<{ uploadUrl: string; s3Key: string }>(`${this.base}/presign`, req);
  }

  uploadToS3(uploadUrl: string, file: File): Observable<void> {
    return this.http.put<void>(uploadUrl, file, {
      headers: { 'Content-Type': file.type },
    });
  }

  registerDocument(req: RegisterDocumentRequest): Observable<ClinicDocument> {
    return this.http.post<ClinicDocument>(this.base, req);
  }

  listDocuments(folderId: string): Observable<ClinicDocument[]> {
    return this.http.get<ClinicDocument[]>(this.base, { params: { folderId } });
  }

  search(q: string): Observable<ClinicDocument[]> {
    return this.http.get<ClinicDocument[]>(`${this.base}/search`, { params: { q } });
  }

  downloadUrl(documentId: string): Observable<{ downloadUrl: string }> {
    return this.http.get<{ downloadUrl: string }>(`${this.base}/${documentId}/download`);
  }

  deleteDocument(documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${documentId}`);
  }
}
