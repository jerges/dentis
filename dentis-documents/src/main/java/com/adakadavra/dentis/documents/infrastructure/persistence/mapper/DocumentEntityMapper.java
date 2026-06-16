package com.adakadavra.dentis.documents.infrastructure.persistence.mapper;

import com.adakadavra.dentis.documents.domain.model.DocumentFile;
import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.DocumentShare;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.DocumentFileEntity;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.DocumentFolderEntity;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.DocumentShareEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentEntityMapper {

    // ── Folder ────────────────────────────────────────────────────────────────

    public DocumentFolderEntity toEntity(DocumentFolder folder) {
        if (folder == null) return null;
        return DocumentFolderEntity.builder()
                .id(folder.getId())
                .clinicId(folder.getClinicId())
                .parentId(folder.getParentId())
                .name(folder.getName())
                .path(folder.getPath())
                .type(folder.getType())
                .visibility(folder.getVisibility())
                .ownerUserId(folder.getOwnerUserId())
                .system(folder.isSystem())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    public DocumentFolder toDomain(DocumentFolderEntity e) {
        if (e == null) return null;
        return DocumentFolder.builder()
                .id(e.getId())
                .clinicId(e.getClinicId())
                .parentId(e.getParentId())
                .name(e.getName())
                .path(e.getPath())
                .type(e.getType())
                .visibility(e.getVisibility())
                .ownerUserId(e.getOwnerUserId())
                .system(e.isSystem())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    // ── File ─────────────────────────────────────────────────────────────────

    public DocumentFileEntity toEntity(DocumentFile file) {
        if (file == null) return null;
        return DocumentFileEntity.builder()
                .id(file.getId())
                .clinicId(file.getClinicId())
                .folderId(file.getFolderId())
                .name(file.getName())
                .fileName(file.getFileName())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .s3Key(file.getS3Key())
                .visibility(file.getVisibility())
                .ownerUserId(file.getOwnerUserId())
                .indexedForIa(file.isIndexedForIa())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    public DocumentFile toDomain(DocumentFileEntity e) {
        if (e == null) return null;
        return DocumentFile.builder()
                .id(e.getId())
                .clinicId(e.getClinicId())
                .folderId(e.getFolderId())
                .name(e.getName())
                .fileName(e.getFileName())
                .contentType(e.getContentType())
                .fileSize(e.getFileSize())
                .s3Key(e.getS3Key())
                .visibility(e.getVisibility())
                .ownerUserId(e.getOwnerUserId())
                .indexedForIa(e.isIndexedForIa())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    // ── Share ────────────────────────────────────────────────────────────────

    public DocumentShareEntity toEntity(DocumentShare share) {
        if (share == null) return null;
        return DocumentShareEntity.builder()
                .id(share.getId())
                .resourceType(share.getResourceType())
                .resourceId(share.getResourceId())
                .sharedWithUserId(share.getSharedWithUserId())
                .createdAt(share.getCreatedAt())
                .build();
    }

    public DocumentShare toDomain(DocumentShareEntity e) {
        if (e == null) return null;
        return DocumentShare.builder()
                .id(e.getId())
                .resourceType(e.getResourceType())
                .resourceId(e.getResourceId())
                .sharedWithUserId(e.getSharedWithUserId())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
