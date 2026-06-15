package com.adakadavra.dentis.ia.infrastructure.persistence.repository;

import com.adakadavra.dentis.ia.domain.model.ChatMessage;
import com.adakadavra.dentis.ia.domain.repository.ChatMessageRepository;
import com.adakadavra.dentis.ia.infrastructure.persistence.mapper.IaEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryAdapter implements ChatMessageRepository {

    private final ChatMessageJpaRepository jpa;
    private final IaEntityMapper mapper;

    @Override
    public ChatMessage save(ChatMessage message) {
        boolean isNew = message.getId() == null;
        return mapper.toMessageDomain(jpa.save(mapper.toMessageEntity(message, isNew)));
    }

    @Override
    public List<ChatMessage> findBySessionIdOrderByCreatedAt(UUID sessionId) {
        return jpa.findBySessionIdOrderByCreatedAt(sessionId)
                .stream().map(mapper::toMessageDomain).toList();
    }
}