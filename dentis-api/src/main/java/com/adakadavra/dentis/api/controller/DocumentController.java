package com.adakadavra.dentis.api.controller;

import com.adakadavra.dentis.api.config.S3StorageService;
import com.adakadavra.dentis.api.security.entity.User;
import com.adakadavra.dentis.api.security.entity.UserRole;
import com.adakadavra.dentis.api.security.repository.UserRepository;
import com.adakadavra.dentis.api.security.service.TenantSecurityService;
import com.adakadavra.dentis.common.response.ApiError;
import com.adakadavra.dentis.common.response.ApiResponse;
import com.adakadavra.dentis.documents.domain.model.*;
import com.adakadavra.dentis.documents.domain.service.*;
import com.adakadavra.dentis.ia.domain.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "Gestión documental y base de conocimiento de la clínica")
public class DocumentController {

    private final DocumentFolderService folderService;
    private final DocumentFileService fileService;
    private final DocumentShareService shareService;
    private final DocumentSearchService searchService;
    private final DocumentAccessService accessService;
    private final S3StorageService s3;
    private final IngestionService ingestionService;
    private final TenantSecurityService tenantSecurity;
    private final UserRepository userRepository;

    private static final String ANY_AUTHENTICATED = "isAuthenticated()";
    private static final String ADMIN_ONLY = "hasAnyRole('SUPER_ADMIN','ADMIN')";

    // ── Resolvers ─────────────────────────────────────────────────────────────

    private UUID resolveClinicId(User user) {
        if (user.getRole() == UserRole.SUPER_ADMIN) return null;
        return tenantSecurity.currentClinicId()
                .orElseThrow(() -> new IllegalStateException("No clinic context"));
    }

    private DocumentAccessService.Role toAccessRole(UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> DocumentAccessService.Role.SUPER_ADMIN;
            case ADMIN -> DocumentAccessService.Role.ADMIN;
            case USER -> DocumentAccessService.Role.USER;
        };
    }

    // ── Folders ───────────────────────────────────────────────────────────────

    @GetMapping("/folders")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Listar carpetas hijas (raíz si parentId omitido)")
    public ResponseEntity<ApiResponse<List<FolderView>>> listFolders(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) UUID clinicId) {

        UUID effectiveClinic = resolveEffectiveClinic(user, clinicId);
        if (effectiveClinic == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiError.builder().code("MISSING_CLINIC").message("Se requiere clinicId").build()));
        }

        DocumentAccessService.Role role = toAccessRole(user.getRole());
        List<FolderView> folders = folderService.listChildren(effectiveClinic, parentId)
                .stream()
                .filter(f -> accessService.canViewFolder(user.getId(), role, effectiveClinic, f))
                .map(FolderView::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(folders));
    }

    @PostMapping("/folders")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Crear una carpeta")
    public ResponseEntity<ApiResponse<FolderView>> createFolder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateFolderRequest req) {

        UUID clinicId = resolveEffectiveClinic(user, req.clinicId());
        if (clinicId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiError.builder().code("MISSING_CLINIC").message("Se requiere clinicId").build()));
        }
        DocumentFolder folder = folderService.create(clinicId, user.getId(), req.parentId(),
                req.name(), req.visibility() != null ? req.visibility() : Visibility.PRIVATE);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(FolderView.from(folder)));
    }

    @PatchMapping("/folders/{id}")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Renombrar o cambiar visibilidad de una carpeta")
    public ResponseEntity<ApiResponse<FolderView>> updateFolder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFolderRequest req) {

        UUID clinicId = resolveClinicId(user);
        DocumentFolder folder = folderService.getById(id);
        checkFolderAccess(user, clinicId, folder, true);

        if (req.name() != null) folder = folderService.rename(id, req.name());
        if (req.visibility() != null) folder = folderService.updateVisibility(id, req.visibility());
        return ResponseEntity.ok(ApiResponse.ok(FolderView.from(folder)));
    }

    @DeleteMapping("/folders/{id}")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Eliminar una carpeta y su contenido")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        UUID clinicId = resolveClinicId(user);
        DocumentFolder folder = folderService.getById(id);
        checkFolderAccess(user, clinicId, folder, true);

        // borrar S3 de cada fichero en cascada
        fileService.listByFolder(id).forEach(f -> {
            if (f.getS3Key() != null) s3.delete(f.getS3Key());
        });
        folderService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Carpeta eliminada"));
    }

    // ── Files ─────────────────────────────────────────────────────────────────

    @GetMapping("/folders/{folderId}/files")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Listar ficheros de una carpeta")
    public ResponseEntity<ApiResponse<List<FileView>>> listFiles(
            @AuthenticationPrincipal User user,
            @PathVariable UUID folderId) {

        UUID clinicId = resolveClinicId(user);
        DocumentFolder folder = folderService.getById(folderId);
        checkFolderAccess(user, clinicId, folder, false);

        DocumentAccessService.Role role = toAccessRole(user.getRole());
        List<FileView> files = fileService.listByFolder(folderId)
                .stream()
                .filter(f -> accessService.canViewFile(user.getId(), role, clinicId, f))
                .map(f -> FileView.from(f, null))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    @PostMapping("/files/presign")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Obtener URL presignada para subir un fichero a S3")
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> presignUpload(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PresignFileRequest req) {

        UUID clinicId = resolveEffectiveClinic(user, req.clinicId());
        if (clinicId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiError.builder().code("MISSING_CLINIC").message("Se requiere clinicId").build()));
        }
        DocumentFolder folder = folderService.getById(req.folderId());
        checkFolderAccess(user, clinicId, folder, false);

        String prefix = folder.getType() == FolderType.KNOWLEDGE_BASE ? "knowledge-base" : "documents";
        String s3Key = clinicId + "/" + prefix + folder.getPath() + "/" +
                UUID.randomUUID() + "_" + sanitize(req.fileName());
        String uploadUrl = s3.presignedUploadUrl(s3Key, req.contentType());
        return ResponseEntity.ok(ApiResponse.ok(new PresignedUploadResponse(s3Key, uploadUrl)));
    }

    @PostMapping("/files")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Registrar un fichero tras subirlo a S3")
    public ResponseEntity<ApiResponse<FileView>> registerFile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RegisterFileRequest req) {

        UUID clinicId = resolveEffectiveClinic(user, req.clinicId());
        if (clinicId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiError.builder().code("MISSING_CLINIC").message("Se requiere clinicId").build()));
        }
        DocumentFolder folder = folderService.getById(req.folderId());
        checkFolderAccess(user, clinicId, folder, false);

        Visibility visibility = req.visibility() != null ? req.visibility() : folder.getVisibility();
        DocumentFile file = fileService.register(clinicId, req.folderId(), user.getId(),
                req.name(), req.fileName(), req.contentType(), req.fileSize(), req.s3Key(), visibility);

        // Si la carpeta es KB → iniciar ingesta asíncrona en la IA
        if (folder.getType() == FolderType.KNOWLEDGE_BASE) {
            ingestionService.ingestKnowledgeBaseFile(file.getId(), clinicId, file.getS3Key(),
                    file.getContentType(), s3::getObjectBytes);
            fileService.markIndexed(file.getId());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(FileView.from(file, null)));
    }

    @GetMapping("/files/{id}/download")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Obtener URL presignada para descargar un fichero")
    public ResponseEntity<ApiResponse<String>> downloadUrl(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        UUID clinicId = resolveClinicId(user);
        DocumentFile file = fileService.getById(id);
        checkFileAccess(user, clinicId, file, false);
        return ResponseEntity.ok(ApiResponse.ok(s3.presignedDownloadUrl(file.getS3Key())));
    }

    @PatchMapping("/files/{id}")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Cambiar visibilidad o mover un fichero")
    public ResponseEntity<ApiResponse<FileView>> updateFile(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFileRequest req) {

        UUID clinicId = resolveClinicId(user);
        DocumentFile file = fileService.getById(id);
        checkFileAccess(user, clinicId, file, true);

        if (req.visibility() != null) file = fileService.updateVisibility(id, req.visibility());
        if (req.folderId() != null) file = fileService.move(id, req.folderId());
        return ResponseEntity.ok(ApiResponse.ok(FileView.from(file, null)));
    }

    @DeleteMapping("/files/{id}")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Eliminar un fichero")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        UUID clinicId = resolveClinicId(user);
        DocumentFile file = fileService.getById(id);
        checkFileAccess(user, clinicId, file, true);

        s3.delete(file.getS3Key());
        if (file.isIndexedForIa()) {
            ingestionService.deleteFromVectorStore(file.getId());
        }
        fileService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Fichero eliminado"));
    }

    // ── Shares ────────────────────────────────────────────────────────────────

    @PostMapping("/shares")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Compartir un recurso con un usuario de la clínica")
    public ResponseEntity<ApiResponse<DocumentShare>> share(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ShareRequest req) {

        UUID clinicId = resolveClinicId(user);
        // Validar que el destinatario pertenece a la misma clínica
        User target = userRepository.findById(req.sharedWithUserId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        if (!target.getClinicId().equals(clinicId) && user.getRole() != UserRole.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(ApiError.builder().code("CROSS_CLINIC").message("No puedes compartir con usuarios de otra clínica").build()));
        }
        DocumentShare share = shareService.share(req.resourceType(), req.resourceId(), req.sharedWithUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(share));
    }

    @DeleteMapping("/shares/{id}")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Revocar compartición")
    public ResponseEntity<ApiResponse<Void>> removeShare(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        shareService.removeShare(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Compartición eliminada"));
    }

    // ── Share targets ─────────────────────────────────────────────────────────

    @GetMapping("/share-targets")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Usuarios de la clínica disponibles para compartir")
    public ResponseEntity<ApiResponse<List<ShareTarget>>> shareTargets(
            @AuthenticationPrincipal User user) {
        UUID clinicId = resolveClinicId(user);
        if (clinicId == null) return ResponseEntity.ok(ApiResponse.ok(List.of()));
        List<ShareTarget> targets = userRepository.findAllByClinicId(clinicId)
                .stream()
                .filter(u -> !u.getId().equals(user.getId()))
                .map(u -> new ShareTarget(u.getId(), u.getFullName(), u.getUsername()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(targets));
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @GetMapping("/search")
    @PreAuthorize(ANY_AUTHENTICATED)
    @Operation(summary = "Buscar documentos en la clínica (nombre + contenido)")
    public ResponseEntity<ApiResponse<List<DocumentSearchService.SearchResult>>> search(
            @AuthenticationPrincipal User user,
            @RequestParam @NotBlank String q) {
        UUID clinicId = resolveClinicId(user);
        if (clinicId == null) return ResponseEntity.ok(ApiResponse.ok(List.of()));
        List<DocumentSearchService.SearchResult> results = searchService.search(clinicId, user.getId(), q);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    // ── KB (Base de Conocimiento) ─────────────────────────────────────────────

    @GetMapping("/kb")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "Obtener la carpeta Base de Conocimiento de la clínica")
    public ResponseEntity<ApiResponse<FolderView>> getKnowledgeBase(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID clinicId) {
        UUID effectiveClinic = resolveEffectiveClinic(user, clinicId);
        if (effectiveClinic == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiError.builder().code("MISSING_CLINIC").message("Se requiere clinicId").build()));
        }
        DocumentFolder kb = folderService.getOrCreateKnowledgeBase(effectiveClinic);
        return ResponseEntity.ok(ApiResponse.ok(FolderView.from(kb)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID resolveEffectiveClinic(User user, UUID requested) {
        if (user.getRole() == UserRole.SUPER_ADMIN) return requested;
        return tenantSecurity.currentClinicId().orElse(null);
    }

    private void checkFolderAccess(User user, UUID clinicId, DocumentFolder folder, boolean manage) {
        DocumentAccessService.Role role = toAccessRole(user.getRole());
        boolean allowed = manage
                ? accessService.canManageFolder(user.getId(), role, clinicId, folder)
                : accessService.canViewFolder(user.getId(), role, clinicId, folder);
        if (!allowed) throw new SecurityException("Acceso denegado a la carpeta");
    }

    private void checkFileAccess(User user, UUID clinicId, DocumentFile file, boolean manage) {
        DocumentAccessService.Role role = toAccessRole(user.getRole());
        boolean allowed = manage
                ? accessService.canManageFile(user.getId(), role, clinicId, file)
                : accessService.canViewFile(user.getId(), role, clinicId, file);
        if (!allowed) throw new SecurityException("Acceso denegado al fichero");
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record CreateFolderRequest(
            @NotBlank @Size(max = 255) String name,
            UUID parentId,
            UUID clinicId,
            Visibility visibility) {}

    public record UpdateFolderRequest(
            @Size(max = 255) String name,
            Visibility visibility) {}

    public record PresignFileRequest(
            UUID folderId,
            UUID clinicId,
            @NotBlank String fileName,
            @NotBlank String contentType) {}

    public record RegisterFileRequest(
            UUID folderId,
            UUID clinicId,
            @NotBlank @Size(max = 255) String name,
            @NotBlank String fileName,
            String contentType,
            Long fileSize,
            @NotBlank String s3Key,
            Visibility visibility) {}

    public record UpdateFileRequest(Visibility visibility, UUID folderId) {}

    public record ShareRequest(
            ResourceType resourceType,
            UUID resourceId,
            UUID sharedWithUserId) {}

    public record ShareTarget(UUID id, String fullName, String username) {}

    public record PresignedUploadResponse(String s3Key, String uploadUrl) {}

    public record FolderView(
            UUID id, UUID clinicId, UUID parentId, String name,
            String path, FolderType type, Visibility visibility,
            UUID ownerUserId, boolean system) {

        static FolderView from(DocumentFolder f) {
            return new FolderView(f.getId(), f.getClinicId(), f.getParentId(),
                    f.getName(), f.getPath(), f.getType(), f.getVisibility(),
                    f.getOwnerUserId(), f.isSystem());
        }
    }

    public record FileView(
            UUID id, UUID clinicId, UUID folderId, String name,
            String fileName, String contentType, Long fileSize,
            Visibility visibility, UUID ownerUserId,
            boolean indexedForIa, String downloadUrl) {

        static FileView from(DocumentFile f, String downloadUrl) {
            return new FileView(f.getId(), f.getClinicId(), f.getFolderId(),
                    f.getName(), f.getFileName(), f.getContentType(), f.getFileSize(),
                    f.getVisibility(), f.getOwnerUserId(), f.isIndexedForIa(), downloadUrl);
        }
    }
}