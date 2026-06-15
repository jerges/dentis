package com.adakadavra.dentis.documents.infrastructure.persistence.mapper;

import com.adakadavra.dentis.documents.domain.model.ClinicDocument;
import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.ClinicDocumentEntity;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.DocumentFolderEntity;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentFolder toDomain(DocumentFolderEntity e) {
        return DocumentFolder.builder()
                .id(e.getId())
                .clinicId(e.getClinicId())
                .parentId(e.getParentId())
                .name(e.getName())
                .s3Prefix(e.getS3Prefix())
                .zone(e.getZone())
                .system(e.isSystemFolder())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public DocumentFolderEntity toEntity(DocumentFolder d) {
        return DocumentFolderEntity.builder()
                .id(d.getId())
                .clinicId(d.getClinicId())
                .parentId(d.getParentId())
                .name(d.getName())
                .s3Prefix(d.getS3Prefix())
                .zone(d.getZone())
                .systemFolder(d.isSystem())
                .createdBy(d.getCreatedBy())
                .createdAt(d.getCreatedAt())
                .build();
    }

    public ClinicDocument toDomain(ClinicDocumentEntity e) {
        return ClinicDocument.builder()
                .id(e.getId())
                .clinicId(e.getClinicId())
                .folderId(e.getFolderId())
                .fileName(e.getFileName())
                .contentType(e.getContentType())
                .s3Key(e.getS3Key())
                .fileSize(e.getFileSize())
                .description(e.getDescription())
                .uploadedBy(e.getUploadedBy())
                .uploadedAt(e.getUploadedAt())
                .indexedForIa(e.isIndexedForIa())
                .build();
    }

    public ClinicDocumentEntity toEntity(ClinicDocument d) {
        return ClinicDocumentEntity.builder()
                .id(d.getId())
                .clinicId(d.getClinicId())
                .folderId(d.getFolderId())
                .fileName(d.getFileName())
                .contentType(d.getContentType())
                .s3Key(d.getS3Key())
                .fileSize(d.getFileSize())
                .description(d.getDescription())
                .uploadedBy(d.getUploadedBy())
                .uploadedAt(d.getUploadedAt())
                .indexedForIa(d.isIndexedForIa())
                .build();
    }
}
