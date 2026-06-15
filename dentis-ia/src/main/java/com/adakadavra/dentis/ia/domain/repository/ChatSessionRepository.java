package com.adakadavra.dentis.ia.domain.repository;

import com.adakadavra.dentis.ia.domain.model.ChatSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository {
    ChatSession save(ChatSession session);
    Optional<ChatSession> findById(UUID id);
    List<ChatSession> findByDentistIdOrderByUpdatedAtDesc(UUID dentistId);
    void deleteById(UUID id);
    void touchUpdatedAt(UUID sessionId);
}
