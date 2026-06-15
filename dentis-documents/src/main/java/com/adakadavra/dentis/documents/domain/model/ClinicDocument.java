package com.adakadavra.dentis.documents.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class ClinicDocument {

    private final UUID id;
    private final UUID clinicId;
    private final UUID folderId;
    private final String fileName;
    private final String contentType;
    private final String s3Key;
    private final Long fileSize;
    private final String description;
    private final UUID uploadedBy;
    private final LocalDateTime uploadedAt;
    /** True when IA has successfully vectorised this document. */
    private final boolean indexedForIa;
    /** PUBLIC = visible to all clinic staff. PRIVATE = visible only to the uploader. */
    private final DocumentVisibility visibility;
}
