package com.adakadavra.dentis.documents.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFile {

    private UUID id;
    private UUID clinicId;
    private UUID folderId;
    private String name;          // nombre descriptivo
    private String fileName;      // nombre original del fichero
    private String contentType;
    private Long fileSize;
    private String s3Key;
    private Visibility visibility;
    private UUID ownerUserId;
    private boolean indexedForIa; // true si ya está en pgvector
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
