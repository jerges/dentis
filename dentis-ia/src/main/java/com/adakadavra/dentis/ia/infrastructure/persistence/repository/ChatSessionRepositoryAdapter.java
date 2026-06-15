package com.adakadavra.dentis.ia.infrastructure.persistence.repository;

import com.adakadavra.dentis.ia.domain.model.ChatSession;
import com.adakadavra.dentis.ia.domain.repository.ChatSessionRepository;
import com.adakadavra.dentis.ia.infrastructure.persistence.mapper.IaEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryAdapter implements ChatSessionRepository {

    private final ChatSessionJpaRepository jpa;
    private final IaEntityMapper mapper;

    @Override
    public ChatSession save(ChatSession session) {
        boolean isNew = session.getId() == null || jpa.findById(
                session.getId() != null ? session.getId() : UUID.randomUUID()).isEmpty();
        return mapper.toSessionDomain(jpa.save(mapper.toSessionEntity(session, isNew)));
    }

    @Override
    public Optional<ChatSession> findById(UUID id) {
        return jpa.findById(id).map(mapper::toSessionDomain);
    }

    @Override
    public List<ChatSession> findByDentistIdOrderByUpdatedAtDesc(UUID dentistId) {
        return jpa.findByDentistIdOrderByUpdatedAtDesc(dentistId)
                .stream().map(mapper::toSessionDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public void touchUpdatedAt(UUID sessionId) {
        jpa.touchUpdatedAt(sessionId);
    }
}