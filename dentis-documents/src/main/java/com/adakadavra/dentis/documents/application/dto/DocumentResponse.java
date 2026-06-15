package com.adakadavra.dentis.documents.application.dto;

import com.adakadavra.dentis.documents.domain.model.DocumentVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID id;
    private UUID folderId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String description;
    private boolean indexedForIa;
    private DocumentVisibility visibility;
    private LocalDateTime uploadedAt;
}
