package com.adakadavra.dentis.ia.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@With
public class ChatSession {
    private final UUID id;
    private final UUID dentistId;
    private final UUID clinicId;
    private final String title;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
