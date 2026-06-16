package com.adakadavra.dentis.ia.domain.service;

import com.adakadavra.dentis.ia.agent.AgentPort;
import com.adakadavra.dentis.ia.agent.AgentPort.AgentRequest;
import com.adakadavra.dentis.ia.agent.AgentPort.AgentResponse;
import com.adakadavra.dentis.ia.domain.model.ChatMessage;
import com.adakadavra.dentis.ia.domain.model.ChatRole;
import com.adakadavra.dentis.ia.domain.model.ChatSession;
import com.adakadavra.dentis.ia.domain.repository.ChatMessageRepository;
import com.adakadavra.dentis.ia.domain.repository.ChatSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IaChatServiceTest {

    @Mock ChatSessionRepository sessionRepo;
    @Mock ChatMessageRepository messageRepo;
    @Mock AgentPort agentPort;

    @InjectMocks
    IaChatService service;

    private final UUID dentistId = UUID.randomUUID();
    private final UUID clinicId  = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    // ── createSession ─────────────────────────────────────────────────────────

    @Test
    void createSessionPersistsWithGivenTitle() {
        ChatSession saved = buildSession(sessionId, dentistId, clinicId, "Mi consulta");
        given(sessionRepo.save(any())).willReturn(saved);

        ChatSession result = service.createSession(dentistId, clinicId, "Mi consulta");

        assertThat(result.getTitle()).isEqualTo("Mi consulta");
        verify(sessionRepo).save(any());
    }

    @Test
    void createSessionUsesDefaultTitleWhenBlank() {
        ChatSession saved = buildSession(sessionId, dentistId, clinicId, "Nueva consulta");
        given(sessionRepo.save(any())).willReturn(saved);

        service.createSession(dentistId, clinicId, "  ");

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(sessionRepo).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Nueva consulta");
    }

    // ── getSessions ───────────────────────────────────────────────────────────

    @Test
    void getSessionsDelegatesToRepository() {
        List<ChatSession> sessions = List.of(buildSession(sessionId, dentistId, clinicId, "test"));
        given(sessionRepo.findByDentistIdOrderByUpdatedAtDesc(dentistId)).willReturn(sessions);

        List<ChatSession> result = service.getSessions(dentistId);

        assertThat(result).hasSize(1);
    }

    // ── getMessages ───────────────────────────────────────────────────────────

    @Test
    void getMessagesReturnsHistoryForOwner() {
        ChatSession session = buildSession(sessionId, dentistId, clinicId, "test");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));
        List<ChatMessage> messages = List.of(buildUserMessage(sessionId, "hola"));
        given(messageRepo.findBySessionIdOrderByCreatedAt(sessionId)).willReturn(messages);

        List<ChatMessage> result = service.getMessages(sessionId, dentistId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getMessagesThrowsWhenSessionNotFound() {
        given(sessionRepo.findById(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMessages(sessionId, dentistId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getMessagesThrowsWhenNotOwner() {
        UUID anotherDentist = UUID.randomUUID();
        ChatSession session = buildSession(sessionId, anotherDentist, clinicId, "test");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.getMessages(sessionId, dentistId))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── deleteSession ─────────────────────────────────────────────────────────

    @Test
    void deleteSessionCallsRepositoryForOwner() {
        ChatSession session = buildSession(sessionId, dentistId, clinicId, "test");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));

        service.deleteSession(sessionId, dentistId);

        verify(sessionRepo).deleteById(sessionId);
    }

    @Test
    void deleteSessionThrowsWhenNotOwner() {
        UUID anotherDentist = UUID.randomUUID();
        ChatSession session = buildSession(sessionId, anotherDentist, clinicId, "test");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.deleteSession(sessionId, dentistId))
                .isInstanceOf(IllegalStateException.class);
        verify(sessionRepo, never()).deleteById(any());
    }

    @Test
    void deleteSessionThrowsWhenSessionNotFound() {
        given(sessionRepo.findById(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSession(sessionId, dentistId))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ChatSession buildSession(UUID id, UUID dentistId, UUID clinicId, String title) {
        return ChatSession.builder()
                .id(id).dentistId(dentistId).clinicId(clinicId)
                .title(title).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private ChatMessage buildUserMessage(UUID sessionId, String content) {
        return ChatMessage.builder()
                .id(UUID.randomUUID()).sessionId(sessionId)
                .role(ChatRole.USER).content(content).createdAt(LocalDateTime.now()).build();
    }

    private ChatMessage buildAssistantMessage(UUID sessionId, String content) {
        return ChatMessage.builder()
                .id(UUID.randomUUID()).sessionId(sessionId)
                .role(ChatRole.ASSISTANT).content(content).createdAt(LocalDateTime.now()).build();
    }
}
