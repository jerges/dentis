package com.adakadavra.dentis.documents.application.dto;

import com.adakadavra.dentis.documents.domain.model.DocumentZone;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FolderResponse {
    private UUID id;
    private UUID parentId;
    private String name;
    private String s3Prefix;
    private DocumentZone zone;
    private boolean system;
    private LocalDateTime createdAt;
}
