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
public class DocumentFolder {

    private final UUID id;
    private final UUID clinicId;
    /** Null means root folder. */
    private final UUID parentId;
    private final String name;
    /** Computed S3 key prefix: clinics/{clinicId}/{zone}/{path}/ */
    private final String s3Prefix;
    private final DocumentZone zone;
    /** System folders cannot be renamed or deleted. */
    private final boolean system;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
}
