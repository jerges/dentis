package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.api.config.S3StorageService;
import com.adakadavra.dentis.api.security.entity.User;
import com.adakadavra.dentis.api.security.entity.UserRole;
import com.adakadavra.dentis.api.security.service.TenantSecurityService;
import com.adakadavra.dentis.documents.application.dto.*;
import com.adakadavra.dentis.documents.domain.model.ClinicDocument;
import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.DocumentZone;
import com.adakadavra.dentis.documents.domain.service.ClinicDocumentService;
import com.adakadavra.dentis.documents.domain.service.DocumentFolderService;
import com.adakadavra.dentis.ia.domain.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Gestión documental y base de conocimiento IA")
public class DocumentController {

    private static final String KB_GUARD =
            "hasRole('SUPER_ADMIN') or hasRole('ADMIN')";
    private static final String STAFF_GUARD =
            "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasRole('USER')";

    private final DocumentFolderService folderService;
    private final ClinicDocumentService documentService;
    private final TenantSecurityService tenantSecurity;
    private final S3StorageService s3;
    private final IngestionService ingestionService;

    // ------------------------------------------------------------------ Folders

    @PostMapping("/folders")
    @PreAuthorize(STAFF_GUARD)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear carpeta (KNOWLEDGE_BASE requiere ADMIN)")
    public FolderResponse createFolder(@Valid @RequestBody CreateFolderRequest req,
                                       @AuthenticationPrincipal User user) {
        UUID clinicId = resolveClinicId(user, null);
        if (req.getZone() == DocumentZone.KNOWLEDGE_BASE
                && user.getRole() != UserRole.SUPER_ADMIN
                && user.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Only ADMIN can create knowledge-base folders");
        }
        DocumentFolder folder = folderService.createFolder(
                clinicId, req.getParentId(), req.getName(), req.getZone(), user.getId());
        return toFolderResponse(folder);
    }

    @GetMapping("/folders")
    @PreAuthorize(STAFF_GUARD)
    @Operation(summary = "Listar carpetas hijas (parentId=null → raíz)")
    public List<FolderResponse> listFolders(@RequestParam(required = false) UUID parentId,
                                             @RequestParam(required = false) UUID clinicId,
                                             @AuthenticationPrincipal User user) {
        UUID resolvedClinic = resolveClinicId(user, clinicId);
        return folderService.listChildren(resolvedClinic, parentId)
                .stream().map(this::toFolderResponse).toList();
    }

    @DeleteMapping("/folders/{folderId}")
    @PreAuthorize(KB_GUARD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar carpeta (no aplica a carpeta raíz KB)")
    public void deleteFolder(@PathVariable UUID folderId, @AuthenticationPrincipal User user) {
        UUID clinicId = resolveClinicId(user, null);
        folderService.deleteFolder(folderId, clinicId);
    }

    @PostMapping("/folders/kb/init")
    @PreAuthorize(KB_GUARD)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inicializar carpeta raíz de Knowledge Base (idempotente)")
    public FolderResponse initKnowledgeBase(@AuthenticationPrincipal User user) {
        UUID clinicId = resolveClinicId(user, null);
        return toFolderResponse(folderService.ensureKnowledgeBaseRoot(clinicId, user.getId()));
    }

    // ------------------------------------------------------------------ Documents

    @PostMapping("/presign")
    @PreAuthorize(STAFF_GUARD)
    @Operation(summary = "Obtener URL pre-firmada para subir un documento a S3")
    public Map<String, String> presignUpload(@Valid @RequestBody PresignUploadRequest req,
                                              @AuthenticationPrincipal User user) {
        UUID clinicId = resolveClinicId(user, null);
        assertKbAccess(user, req.getFolderId());
        ClinicDocumentService.PresignResult info = documentService.buildPresignInfo(
                clinicId, new ClinicDocumentService.PresignRequest(
                        req.getFolderId(), req.getFileName(), req.getContentType()));
        String uploadUrl = s3.presignedUploadUrl(info.s3Key(), req.getContentType());
        return Map.of("uploadUrl", uploadUrl, "s3Key", info.s3Key());
    }

    @PostMapping
    @PreAuthorize(STAFF_GUARD)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar documento tras subida exitosa a S3")
    public DocumentResponse registerDocument(@Valid @RequestBody RegisterDocumentRequest req,
                                              @AuthenticationPrincipal User user) {
        UUID clinicId = resolveClinicId(user, null);
        assertKbAccess(user, req.getFolderId());
        ClinicDocument doc = documentService.register(
                clinicId, req.getFolderId(), req.getFileName(), req.getContentType(),
                req.getS3Key(), req.getFileSize(), req.getDescription(), user.getId());

        if (documentService.isKnowledgeBaseFolder(req.getFolderId())) {
            ingestionService.ingestAsync(
                    doc.getId(), clinicId, doc.getS3Key(), doc.getContentType(),
                    s3::getObjectBytes);
            documentService.markIndexed(doc.getId());
        }
        return toDocumentResponse(doc);
    }

    @GetMapping
    @PreAuthorize(STAFF_GUARD)
    @Operation(summary = "Listar documentos de una carpeta")
    public List<DocumentResponse> listDocuments(@RequestParam UUID folderId,
                                                 @AuthenticationPrincipal User user) {
        UUID clinicId = resolveClinicId(user, null);
        return documentService.listByFolder(folderId, clinicId)
                .stream().map(this::toDocumentResponse).toList();
    }

    @GetMapping("/search")
    @PreAuthorize(STAFF_GUARD)
    @Operation(summary = "Buscar documentos por texto (full-text, scoped por clínica)")
    public List<DocumentResponse> search(@RequestParam String q,
                                          @RequestParam(required = false) UUID clinicId,
                                          @AuthenticationPrincipal User user) {
        UUID resolvedClinic = resolveClinicId(user, clinicId);
        return documentService.search(resolvedClinic, q)
                .stream().map(this::toDocumentResponse).toList();
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize(STAFF_GUARD)
    @Operation(summary = "Obtener URL pre-firmada para descargar un documento")
    public Map<String, String> downloadUrl(@PathVariable UUID documentId,
                                            @AuthenticationPrincipal User user) {
        UUID clinicId = resolveClinicId(user, null);
        ClinicDocument doc = documentService.getDocument(documentId, clinicId);
        return Map.of("downloadUrl", s3.presignedDownloadUrl(doc.getS3Key()));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize(STAFF_GUARD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar documento (S3 + registro BD)")
    public void deleteDocument(@PathVariable UUID documentId, @AuthenticationPrincipal User user) {
        UUID clinicId = resolveClinicId(user, null);
        String s3Key = documentService.deleteDocument(documentId, clinicId);
        s3.delete(s3Key);
    }

    // ------------------------------------------------------------------ Helpers

    private UUID resolveClinicId(User user, UUID overrideClinicId) {
        if (user.getRole() == UserRole.SUPER_ADMIN && overrideClinicId != null) {
            return overrideClinicId;
        }
        return tenantSecurity.currentClinicId()
                .orElseThrow(() -> new IllegalStateException("No clinic associated with this user"));
    }

    /** Only ADMIN/SUPER_ADMIN may write to knowledge-base folders. */
    private void assertKbAccess(User user, UUID folderId) {
        if (documentService.isKnowledgeBaseFolder(folderId)
                && user.getRole() != UserRole.SUPER_ADMIN
                && user.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Only ADMIN can upload to the knowledge-base");
        }
    }

    private FolderResponse toFolderResponse(DocumentFolder f) {
        return FolderResponse.builder()
                .id(f.getId())
                .parentId(f.getParentId())
                .name(f.getName())
                .s3Prefix(f.getS3Prefix())
                .zone(f.getZone())
                .system(f.isSystem())
                .createdAt(f.getCreatedAt())
                .build();
    }

    private DocumentResponse toDocumentResponse(ClinicDocument d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .folderId(d.getFolderId())
                .fileName(d.getFileName())
                .contentType(d.getContentType())
                .fileSize(d.getFileSize())
                .description(d.getDescription())
                .indexedForIa(d.isIndexedForIa())
                .uploadedAt(d.getUploadedAt())
                .build();
    }
}
