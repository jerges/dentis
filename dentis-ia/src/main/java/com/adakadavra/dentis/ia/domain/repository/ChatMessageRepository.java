package com.adakadavra.dentis.ia.domain.repository;

import com.adakadavra.dentis.ia.domain.model.ChatMessage;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findBySessionIdOrderByCreatedAt(UUID sessionId);
}
