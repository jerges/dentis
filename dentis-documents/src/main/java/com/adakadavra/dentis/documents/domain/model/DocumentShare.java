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
public class DocumentShare {

    private UUID id;
    private ResourceType resourceType;
    private UUID resourceId;
    private UUID sharedWithUserId;
    private LocalDateTime createdAt;
}
