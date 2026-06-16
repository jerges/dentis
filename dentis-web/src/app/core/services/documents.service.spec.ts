import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DocumentsService } from './documents.service';
import { DocumentFolder, DocumentFile, DocumentShare } from '../models/documents.model';

describe('DocumentsService', () => {
  let service: DocumentsService;
  let httpMock: HttpTestingController;

  const mockFolder: DocumentFolder = {
    id: 'folder-1', clinicId: 'clinic-1', parentId: null, name: 'Protocolos', path: '/Protocolos',
    type: 'NORMAL', visibility: 'PUBLIC', ownerUserId: 'user-1', system: false
  };

  const mockFile: DocumentFile = {
    id: 'file-1', clinicId: 'clinic-1', folderId: 'folder-1', name: 'Protocolo',
    fileName: 'protocolo.pdf', contentType: 'application/pdf', fileSize: 1024,
    visibility: 'PRIVATE', ownerUserId: 'user-1', indexedForIa: false
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DocumentsService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(DocumentsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ── Folders ──────────────────────────────────────────────────────────────

  it('listFolders() sends GET to /api/v1/documents/folders and unwraps data', () => {
    let result: DocumentFolder[] | undefined;
    service.listFolders().subscribe(f => result = f);

    const req = httpMock.expectOne(r => r.url.startsWith('/api/v1/documents/folders'));
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockFolder] });

    expect(result).toEqual([mockFolder]);
  });

  it('listFolders() includes parentId in query when provided', () => {
    service.listFolders(undefined, 'parent-abc').subscribe();

    const req = httpMock.expectOne(r => r.url.includes('parentId=parent-abc'));
    req.flush({ success: true, data: [] });
  });

  it('createFolder() sends POST with body and returns folder', () => {
    let result: DocumentFolder | undefined;
    service.createFolder({ name: 'Nueva', visibility: 'PRIVATE' }).subscribe(f => result = f);

    const req = httpMock.expectOne('/api/v1/documents/folders');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Nueva', visibility: 'PRIVATE' });
    req.flush({ success: true, data: mockFolder });

    expect(result).toEqual(mockFolder);
  });

  it('getKnowledgeBase() sends GET to /api/v1/documents/kb', () => {
    let result: DocumentFolder | undefined;
    service.getKnowledgeBase().subscribe(f => result = f);

    const req = httpMock.expectOne('/api/v1/documents/kb');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { ...mockFolder, type: 'KNOWLEDGE_BASE' } });

    expect(result?.type).toBe('KNOWLEDGE_BASE');
  });

  // ── Files ────────────────────────────────────────────────────────────────

  it('listFiles() sends GET to folders/{id}/files', () => {
    let result: DocumentFile[] | undefined;
    service.listFiles('folder-1').subscribe(f => result = f);

    const req = httpMock.expectOne('/api/v1/documents/folders/folder-1/files');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockFile] });

    expect(result).toEqual([mockFile]);
  });

  it('presignUpload() sends POST to /api/v1/documents/files/presign', () => {
    service.presignUpload('folder-1', 'doc.pdf', 'application/pdf').subscribe();

    const req = httpMock.expectOne('/api/v1/documents/files/presign');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ folderId: 'folder-1', fileName: 'doc.pdf', contentType: 'application/pdf', clinicId: undefined });
    req.flush({ success: true, data: { uploadUrl: 'https://s3.amazonaws.com/put', s3Key: 'key', fileId: 'file-1' } });
  });

  it('uploadToS3() sends PUT directly to the external URL without ApiResponse wrapper', () => {
    const file = new File(['content'], 'doc.pdf', { type: 'application/pdf' });
    service.uploadToS3('https://bucket.s3.amazonaws.com/presigned', file).subscribe();

    const req = httpMock.expectOne('https://bucket.s3.amazonaws.com/presigned');
    expect(req.request.method).toBe('PUT');
    expect(req.request.headers.get('Content-Type')).toBe('application/pdf');
    req.flush(null);
  });

  it('registerFile() sends POST to /api/v1/documents/files', () => {
    let result: DocumentFile | undefined;
    const registerReq = { folderId: 'folder-1', name: 'Doc', fileName: 'doc.pdf', contentType: 'application/pdf', fileSize: 1024, s3Key: 'key', visibility: 'PRIVATE' as const };
    service.registerFile(registerReq).subscribe(f => result = f);

    const req = httpMock.expectOne('/api/v1/documents/files');
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, data: mockFile });

    expect(result).toEqual(mockFile);
  });

  it('downloadUrl() sends GET to /api/v1/documents/files/{id}/download', () => {
    let result: string | undefined;
    service.downloadUrl('file-1').subscribe(u => result = u);

    const req = httpMock.expectOne('/api/v1/documents/files/file-1/download');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: 'https://s3.amazonaws.com/download-presigned' });

    expect(result).toBe('https://s3.amazonaws.com/download-presigned');
  });

  it('search() encodes the query string', () => {
    service.search('radiografía').subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/v1/documents/search' && r.params.get('q') === 'radiografía');
    req.flush({ success: true, data: [] });
  });

  // ── Shares ────────────────────────────────────────────────────────────────

  it('share() sends POST to /api/v1/documents/shares with correct body', () => {
    let result: DocumentShare | undefined;
    const mockShare: DocumentShare = { id: 'share-1', resourceType: 'FILE', resourceId: 'file-1', sharedWithUserId: 'user-2' };
    service.share('FILE', 'file-1', 'user-2').subscribe(s => result = s);

    const req = httpMock.expectOne('/api/v1/documents/shares');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ resourceType: 'FILE', resourceId: 'file-1', sharedWithUserId: 'user-2' });
    req.flush({ success: true, data: mockShare });

    expect(result?.id).toBe('share-1');
  });
});
