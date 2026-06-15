package com.adakadavra.dentis.ia.infrastructure.persistence.mapper;

import com.adakadavra.dentis.ia.domain.model.ChatMessage;
import com.adakadavra.dentis.ia.domain.model.ChatRole;
import com.adakadavra.dentis.ia.domain.model.ChatSession;
import com.adakadavra.dentis.ia.infrastructure.persistence.entity.ChatMessageEntity;
import com.adakadavra.dentis.ia.infrastructure.persistence.entity.ChatSessionEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IaEntityMapper {

    public ChatSession toSessionDomain(ChatSessionEntity e) {
        return ChatSession.builder()
                .id(e.getId()).dentistId(e.getDentistId()).clinicId(e.getClinicId())
                .title(e.getTitle()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public ChatSessionEntity toSessionEntity(ChatSession s, boolean isNew) {
        return ChatSessionEntity.builder()
                .id(s.getId() != null ? s.getId() : UUID.randomUUID())
                .dentistId(s.getDentistId()).clinicId(s.getClinicId())
                .title(s.getTitle()).createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt())
                .isNew(isNew).build();
    }

    public ChatMessage toMessageDomain(ChatMessageEntity e) {
        return ChatMessage.builder()
                .id(e.getId()).sessionId(e.getSessionId())
                .role(ChatRole.valueOf(e.getRole()))
                .content(e.getContent()).citations(e.getCitations())
                .inputTokens(e.getInputTokens()).outputTokens(e.getOutputTokens())
                .createdAt(e.getCreatedAt()).build();
    }

    public ChatMessageEntity toMessageEntity(ChatMessage m, boolean isNew) {
        return ChatMessageEntity.builder()
                .id(m.getId() != null ? m.getId() : UUID.randomUUID())
                .sessionId(m.getSessionId()).role(m.getRole().name())
                .content(m.getContent()).citations(m.getCitations())
                .inputTokens(m.getInputTokens()).outputTokens(m.getOutputTokens())
                .createdAt(m.getCreatedAt()).isNew(isNew).build();
    }
}