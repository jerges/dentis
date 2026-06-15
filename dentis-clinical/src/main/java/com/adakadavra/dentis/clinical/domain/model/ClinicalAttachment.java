package com.adakadavra.dentis.clinical.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ClinicalAttachment {

    private final UUID id;
    private final UUID clinicalRecordId;
    private final UUID clinicId;
    private final Integer toothNumber;
    private final String fileName;
    private final String contentType;
    private final String s3Key;
    private final Long fileSize;
    private final String description;
    private final UUID uploadedByDentistId;
    private final LocalDateTime uploadedAt;
}
