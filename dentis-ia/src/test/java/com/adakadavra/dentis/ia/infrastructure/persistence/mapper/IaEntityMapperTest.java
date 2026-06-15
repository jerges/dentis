package com.adakadavra.dentis.ia.infrastructure.persistence.mapper;

import com.adakadavra.dentis.ia.domain.model.ChatMessage;
import com.adakadavra.dentis.ia.domain.model.ChatRole;
import com.adakadavra.dentis.ia.domain.model.ChatSession;
import com.adakadavra.dentis.ia.infrastructure.persistence.entity.ChatMessageEntity;
import com.adakadavra.dentis.ia.infrastructure.persistence.entity.ChatSessionEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IaEntityMapperTest {

    private final IaEntityMapper mapper = new IaEntityMapper();

    private final UUID sessionId  = UUID.randomUUID();
    private final UUID dentistId  = UUID.randomUUID();
    private final UUID clinicId   = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    // ── ChatSession ───────────────────────────────────────────────────────────

    @Test
    void toSessionDomainMapsAllFields() {
        ChatSessionEntity entity = ChatSessionEntity.builder()
                .id(sessionId).dentistId(dentistId).clinicId(clinicId)
                .title("Consulta protésica").createdAt(now).updatedAt(now)
                .build();

        ChatSession domain = mapper.toSessionDomain(entity);

        assertThat(domain.getId()).isEqualTo(sessionId);
        assertThat(domain.getDentistId()).isEqualTo(dentistId);
        assertThat(domain.getClinicId()).isEqualTo(clinicId);
        assertThat(domain.getTitle()).isEqualTo("Consulta protésica");
        assertThat(domain.getCreatedAt()).isEqualTo(now);
        assertThat(domain.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void toSessionEntityPreservesExistingId() {
        ChatSession session = ChatSession.builder()
                .id(sessionId).dentistId(dentistId).clinicId(clinicId)
                .title("Ortodoncia").createdAt(now).updatedAt(now).build();

        ChatSessionEntity entity = mapper.toSessionEntity(session, false);

        assertThat(entity.getId()).isEqualTo(sessionId);
        assertThat(entity.isNew()).isFalse();
    }

    @Test
    void toSessionEntityWithNullIdGeneratesUuid() {
        ChatSession session = ChatSession.builder()
                .id(null).dentistId(dentistId).clinicId(clinicId)
                .title("Nueva").createdAt(now).updatedAt(now).build();

        ChatSessionEntity entity = mapper.toSessionEntity(session, true);

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.isNew()).isTrue();
    }

    // ── ChatMessage ───────────────────────────────────────────────────────────

    @Test
    void toMessageDomainMapsAllFields() {
        UUID msgId = UUID.randomUUID();
        ChatMessageEntity entity = ChatMessageEntity.builder()
                .id(msgId).sessionId(sessionId)
                .role("USER").content("¿Cuándo debo aplicar flúor?")
                .citations("clinic/doc.pdf")
                .inputTokens(80).outputTokens(40)
                .createdAt(now).build();

        ChatMessage domain = mapper.toMessageDomain(entity);

        assertThat(domain.getId()).isEqualTo(msgId);
        assertThat(domain.getSessionId()).isEqualTo(sessionId);
        assertThat(domain.getRole()).isEqualTo(ChatRole.USER);
        assertThat(domain.getContent()).isEqualTo("¿Cuándo debo aplicar flúor?");
        assertThat(domain.getCitations()).isEqualTo("clinic/doc.pdf");
        assertThat(domain.getInputTokens()).isEqualTo(80);
        assertThat(domain.getOutputTokens()).isEqualTo(40);
        assertThat(domain.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void toMessageDomainHandlesAssistantRole() {
        ChatMessageEntity entity = ChatMessageEntity.builder()
                .id(UUID.randomUUID()).sessionId(sessionId)
                .role("ASSISTANT").content("El flúor se aplica tras la profilaxis.")
                .createdAt(now).build();

        ChatMessage domain = mapper.toMessageDomain(entity);

        assertThat(domain.getRole()).isEqualTo(ChatRole.ASSISTANT);
    }

    @Test
    void toMessageEntityPreservesExistingId() {
        UUID msgId = UUID.randomUUID();
        ChatMessage message = ChatMessage.builder()
                .id(msgId).sessionId(sessionId).role(ChatRole.USER)
                .content("pregunta").createdAt(now).build();

        ChatMessageEntity entity = mapper.toMessageEntity(message, false);

        assertThat(entity.getId()).isEqualTo(msgId);
        assertThat(entity.getRole()).isEqualTo("USER");
        assertThat(entity.isNew()).isFalse();
    }

    @Test
    void toMessageEntityWithNullIdGeneratesUuid() {
        ChatMessage message = ChatMessage.builder()
                .id(null).sessionId(sessionId).role(ChatRole.ASSISTANT)
                .content("respuesta").createdAt(now).build();

        ChatMessageEntity entity = mapper.toMessageEntity(message, true);

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void toMessageEntityMapsNullableFieldsGracefully() {
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID()).sessionId(sessionId).role(ChatRole.USER)
                .content("texto").citations(null).createdAt(now).build();

        ChatMessageEntity entity = mapper.toMessageEntity(message, true);

        assertThat(entity.getCitations()).isNull();
        assertThat(entity.getInputTokens()).isZero();
        assertThat(entity.getOutputTokens()).isZero();
    }
}
