package com.adakadavra.dentis.ia.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ChatMessage {
    private final UUID id;
    private final UUID sessionId;
    private final ChatRole role;
    private final String content;
    private final String citations;
    private final int inputTokens;
    private final int outputTokens;
    private final LocalDateTime createdAt;
}
