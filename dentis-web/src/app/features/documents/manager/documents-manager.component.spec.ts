import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { DocumentsManagerComponent } from './documents-manager.component';
import { DocumentsService } from '../../../core/services/documents.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClinicDocument, DocumentFolder } from '../../../core/models/documents.model';

const generalFolder: DocumentFolder = {
  id: 'folder-1',
  parentId: null,
  name: 'Facturas',
  s3Prefix: 'clinics/c1/general/',
  zone: 'GENERAL',
  system: false,
  visibility: 'PUBLIC',
  createdAt: '2026-01-01T10:00:00'
};

const privateFolder: DocumentFolder = {
  id: 'folder-priv',
  parentId: null,
  name: 'Mis Notas',
  s3Prefix: 'clinics/c1/general/mis-notas/',
  zone: 'GENERAL',
  system: false,
  visibility: 'PRIVATE',
  createdAt: '2026-01-01T10:00:00'
};

const kbFolder: DocumentFolder = {
  id: 'folder-kb',
  parentId: null,
  name: 'Base de Conocimiento IA',
  s3Prefix: 'clinics/c1/knowledge-base/',
  zone: 'KNOWLEDGE_BASE',
  system: true,
  visibility: 'PUBLIC',
  createdAt: '2026-01-01T10:00:00'
};

const sampleDoc: ClinicDocument = {
  id: 'doc-1',
  folderId: 'folder-1',
  fileName: 'factura.pdf',
  contentType: 'application/pdf',
  fileSize: 51200,
  description: null,
  indexedForIa: false,
  visibility: 'PUBLIC',
  uploadedAt: '2026-01-01T10:00:00'
};

const privateDoc: ClinicDocument = {
  id: 'doc-priv',
  folderId: 'folder-1',
  fileName: 'privado.pdf',
  contentType: 'application/pdf',
  fileSize: 10240,
  description: null,
  indexedForIa: false,
  visibility: 'PRIVATE',
  uploadedAt: '2026-01-01T10:00:00'
};

describe('DocumentsManagerComponent', () => {
  let component: DocumentsManagerComponent;
  let fixture: ComponentFixture<DocumentsManagerComponent>;
  let svcSpy: jasmine.SpyObj<DocumentsService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  function buildAuthService(role: string) {
    return { currentUser: jasmine.createSpy('currentUser').and.returnValue({ role }) };
  }

  beforeEach(async () => {
    svcSpy = jasmine.createSpyObj<DocumentsService>('DocumentsService', [
      'listFolders', 'createFolder', 'deleteFolder', 'initKnowledgeBase',
      'presignUpload', 'uploadToS3', 'registerDocument',
      'listDocuments', 'search', 'downloadUrl', 'deleteDocument'
    ]);

    svcSpy.listFolders.and.returnValue(of([generalFolder]));
    svcSpy.listDocuments.and.returnValue(of([]));
    svcSpy.search.and.returnValue(of([]));
    snackSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [DocumentsManagerComponent, NoopAnimationsModule],
      providers: [
        { provide: DocumentsService, useValue: svcSpy },
        { provide: AuthService, useValue: buildAuthService('ADMIN') },
        { provide: MatSnackBar, useValue: snackSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentsManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load root folders on init', () => {
    expect(svcSpy.listFolders).toHaveBeenCalledWith(null);
    expect(component.rootFolders().length).toBe(1);
    expect(component.rootFolders()[0].name).toBe('Facturas');
  });

  it('canManageKb should be true for ADMIN', () => {
    expect(component.canManageKb()).toBeTrue();
  });

  it('canManageKb should be false for USER role', async () => {
    await TestBed.resetTestingModule();
    svcSpy.listFolders.and.returnValue(of([]));
    svcSpy.search.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [DocumentsManagerComponent, NoopAnimationsModule],
      providers: [
        { provide: DocumentsService, useValue: svcSpy },
        { provide: AuthService, useValue: buildAuthService('USER') },
        { provide: MatSnackBar, useValue: snackSpy }
      ]
    }).compileComponents();
    const f2 = TestBed.createComponent(DocumentsManagerComponent);
    f2.detectChanges();
    expect(f2.componentInstance.canManageKb()).toBeFalse();
  });

  it('selectFolder should load documents for the selected folder', () => {
    svcSpy.listDocuments.and.returnValue(of([sampleDoc]));
    component.selectFolder({ ...generalFolder, children: [] });

    expect(svcSpy.listDocuments).toHaveBeenCalledWith('folder-1');
    expect(component.selectedFolder()?.id).toBe('folder-1');
    expect(component.documents().length).toBe(1);
  });

  it('initKb should reload folders and show snack on success', () => {
    svcSpy.initKnowledgeBase.and.returnValue(of(kbFolder));

    component.initKb();

    expect(svcSpy.initKnowledgeBase).toHaveBeenCalled();
    expect(snackSpy.open).toHaveBeenCalledWith('Base de conocimiento inicializada', 'OK', { duration: 3000 });
    expect(svcSpy.listFolders).toHaveBeenCalledTimes(2);
  });

  it('initKb should show error snack on failure', () => {
    svcSpy.initKnowledgeBase.and.returnValue(throwError(() => ({ error: { message: 'Error S3' } })));

    component.initKb();

    expect(snackSpy.open).toHaveBeenCalledWith(jasmine.stringContaining('Error'), 'Cerrar', { duration: 4000 });
  });

  it('createFolder should call service and reload folders', () => {
    svcSpy.createFolder.and.returnValue(of(generalFolder));
    component.newFolderName = 'Contratos';
    component.newFolderZone = 'GENERAL';
    component.newFolderVisibility = 'PUBLIC';
    component.showNewFolderDialog.set(true);

    component.createFolder();

    expect(svcSpy.createFolder).toHaveBeenCalledWith(
      jasmine.objectContaining({ name: 'Contratos', zone: 'GENERAL', visibility: 'PUBLIC' })
    );
    expect(component.showNewFolderDialog()).toBeFalse();
    expect(svcSpy.listFolders).toHaveBeenCalledTimes(2);
  });

  it('createFolder should pass PRIVATE visibility when set', () => {
    svcSpy.createFolder.and.returnValue(of(privateFolder));
    component.newFolderName = 'Mis Notas';
    component.newFolderZone = 'GENERAL';
    component.newFolderVisibility = 'PRIVATE';
    component.showNewFolderDialog.set(true);

    component.createFolder();

    expect(svcSpy.createFolder).toHaveBeenCalledWith(
      jasmine.objectContaining({ name: 'Mis Notas', visibility: 'PRIVATE' })
    );
  });

  it('newFolderVisibility should default to PUBLIC', () => {
    expect(component.newFolderVisibility).toBe('PUBLIC');
  });

  it('createFolder should do nothing when name is blank', () => {
    component.newFolderName = '   ';
    component.createFolder();
    expect(svcSpy.createFolder).not.toHaveBeenCalled();
  });

  it('createFolder should show error on service failure', () => {
    svcSpy.createFolder.and.returnValue(throwError(() => ({ error: { message: 'Duplicado' } })));
    component.newFolderName = 'Nueva';

    component.createFolder();

    expect(snackSpy.open).toHaveBeenCalledWith(jasmine.stringContaining('Error'), 'Cerrar', { duration: 4000 });
  });

  it('confirmDeleteFolder should call deleteFolder and reload when confirmed', () => {
    svcSpy.deleteFolder.and.returnValue(of(undefined));
    spyOn(window, 'confirm').and.returnValue(true);

    component.confirmDeleteFolder({ ...generalFolder, children: [] });

    expect(svcSpy.deleteFolder).toHaveBeenCalledWith('folder-1');
    expect(svcSpy.listFolders).toHaveBeenCalledTimes(2);
  });

  it('confirmDeleteFolder should not call service when user cancels', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    component.confirmDeleteFolder({ ...generalFolder, children: [] });
    expect(svcSpy.deleteFolder).not.toHaveBeenCalled();
  });

  it('downloadDoc should open window with presigned URL', () => {
    svcSpy.downloadUrl.and.returnValue(of({ downloadUrl: 'https://s3.example.com/dl' }));
    spyOn(window, 'open');

    component.downloadDoc(sampleDoc);

    expect(svcSpy.downloadUrl).toHaveBeenCalledWith('doc-1');
    expect(window.open).toHaveBeenCalledWith('https://s3.example.com/dl', '_blank');
  });

  it('confirmDeleteDoc should delete and reload documents', () => {
    svcSpy.deleteDocument.and.returnValue(of(undefined));
    svcSpy.listDocuments.and.returnValue(of([]));
    spyOn(window, 'confirm').and.returnValue(true);
    component.selectedFolder.set(generalFolder);

    component.confirmDeleteDoc(sampleDoc);

    expect(svcSpy.deleteDocument).toHaveBeenCalledWith('doc-1');
    expect(snackSpy.open).toHaveBeenCalledWith('Documento eliminado', 'OK', { duration: 3000 });
  });

  it('clearSearch should reset query and results', () => {
    component.searchQuery.set('protocolo');
    component.searchResults.set([sampleDoc]);

    component.clearSearch();

    expect(component.searchQuery()).toBe('');
    expect(component.searchResults().length).toBe(0);
  });

  it('uploadVisibility should default to PUBLIC', () => {
    expect(component.uploadVisibility).toBe('PUBLIC');
  });

  it('PRIVATE document should be identifiable by visibility field', () => {
    expect(privateDoc.visibility).toBe('PRIVATE');
    expect(sampleDoc.visibility).toBe('PUBLIC');
  });

  describe('fileIcon', () => {
    it('should return picture_as_pdf for application/pdf', () => {
      expect(component.fileIcon('application/pdf')).toBe('picture_as_pdf');
    });
    it('should return image for image/png', () => {
      expect(component.fileIcon('image/png')).toBe('image');
    });
    it('should return table_chart for spreadsheet', () => {
      expect(component.fileIcon('application/vnd.ms-excel')).toBe('table_chart');
    });
    it('should return article for word document', () => {
      expect(component.fileIcon('application/msword')).toBe('article');
    });
    it('should return insert_drive_file for unknown type', () => {
      expect(component.fileIcon('application/octet-stream')).toBe('insert_drive_file');
    });
  });

  describe('formatSize', () => {
    it('should format bytes as B', () => {
      expect(component.formatSize(512)).toBe('512 B');
    });
    it('should format KB', () => {
      expect(component.formatSize(2048)).toBe('2.0 KB');
    });
    it('should format MB', () => {
      expect(component.formatSize(3 * 1024 * 1024)).toBe('3.0 MB');
    });
  });

  describe('search pipeline', () => {
    it('onSearch should not trigger search when query is 1 char or less', fakeAsync(() => {
      component.onSearch('x');
      tick(400);
      expect(svcSpy.search).not.toHaveBeenCalled();
    }));

    it('onSearch should clear results when query is empty', () => {
      component.searchResults.set([sampleDoc]);
      component.onSearch('');
      expect(component.searchResults().length).toBe(0);
    });
  });
});
