package com.adakadavra.dentis.scheduling.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class TimeBlock {

    private final UUID id;
    private final UUID dentistId;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final String reason;
    private final TimeBlockType type;
}
