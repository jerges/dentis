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

    // ── sendMessage ───────────────────────────────────────────────────────────

    @Test
    void sendMessageSavesUserMessageThenCallsAgent() {
        ChatSession session = buildSession(sessionId, dentistId, clinicId, "consulta");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));
        given(messageRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(messageRepo.findBySessionIdOrderByCreatedAt(sessionId))
                .willReturn(List.of(buildUserMessage(sessionId, "caries en molar")));
        given(agentPort.ask(any())).willReturn(new AgentResponse("Tratamiento recomendado.", 10, 5, null));

        ChatMessage reply = service.sendMessage(sessionId, dentistId, "caries en molar");

        assertThat(reply.getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(reply.getContent()).isEqualTo("Tratamiento recomendado.");
        verify(agentPort).ask(any());
        verify(sessionRepo).touchUpdatedAt(sessionId);
    }

    @Test
    void sendMessagePassesClinicIdAndHistoryToAgent() {
        ChatSession session = buildSession(sessionId, dentistId, clinicId, "consulta");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));
        given(messageRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(messageRepo.findBySessionIdOrderByCreatedAt(sessionId))
                .willReturn(List.of(buildUserMessage(sessionId, "endodoncia")));
        given(agentPort.ask(any())).willReturn(new AgentResponse("respuesta", 5, 3, null));

        service.sendMessage(sessionId, dentistId, "endodoncia");

        ArgumentCaptor<AgentRequest> captor = ArgumentCaptor.forClass(AgentRequest.class);
        verify(agentPort).ask(captor.capture());
        AgentRequest captured = captor.getValue();
        assertThat(captured.clinicId()).isEqualTo(clinicId);
        assertThat(captured.userText()).isEqualTo("endodoncia");
    }

    @Test
    void sendMessageFiltersOffTopicMessagesFromHistory() {
        ChatSession session = buildSession(sessionId, dentistId, clinicId, "consulta");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));
        given(messageRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

        ChatMessage offTopic = buildAssistantMessage(sessionId, RelevanceGuard.OFF_TOPIC_RESPONSE);
        ChatMessage valid    = buildUserMessage(sessionId, "caries");
        given(messageRepo.findBySessionIdOrderByCreatedAt(sessionId))
                .willReturn(List.of(offTopic, valid));
        given(agentPort.ask(any())).willReturn(new AgentResponse("ok", 0, 0, null));

        service.sendMessage(sessionId, dentistId, "caries");

        ArgumentCaptor<AgentRequest> captor = ArgumentCaptor.forClass(AgentRequest.class);
        verify(agentPort).ask(captor.capture());
        List<AgentPort.ConversationTurn> turns = captor.getValue().history();
        // off-topic message must be excluded
        assertThat(turns).noneMatch(t -> t.content().equals(RelevanceGuard.OFF_TOPIC_RESPONSE));
        assertThat(turns).anyMatch(t -> t.content().equals("caries"));
    }

    @Test
    void sendMessageSavesCitationsFromAgentResponse() {
        ChatSession session = buildSession(sessionId, dentistId, clinicId, "consulta");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));
        given(messageRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(messageRepo.findBySessionIdOrderByCreatedAt(sessionId)).willReturn(List.of());
        given(agentPort.ask(any())).willReturn(new AgentResponse("respuesta", 10, 5, "clinic/doc.pdf"));

        ChatMessage reply = service.sendMessage(sessionId, dentistId, "tratamiento");

        assertThat(reply.getCitations()).isEqualTo("clinic/doc.pdf");
    }

    @Test
    void sendMessageSavesTokenUsage() {
        ChatSession session = buildSession(sessionId, dentistId, clinicId, "consulta");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));
        given(messageRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(messageRepo.findBySessionIdOrderByCreatedAt(sessionId)).willReturn(List.of());
        given(agentPort.ask(any())).willReturn(new AgentResponse("texto", 150, 75, null));

        ChatMessage reply = service.sendMessage(sessionId, dentistId, "pregunta");

        assertThat(reply.getInputTokens()).isEqualTo(150);
        assertThat(reply.getOutputTokens()).isEqualTo(75);
    }

    @Test
    void sendMessageThrowsWhenSessionNotFound() {
        given(sessionRepo.findById(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(sessionId, dentistId, "texto"))
                .isInstanceOf(NoSuchElementException.class);
        verify(agentPort, never()).ask(any());
    }

    @Test
    void sendMessageThrowsWhenNotOwner() {
        UUID anotherDentist = UUID.randomUUID();
        ChatSession session = buildSession(sessionId, anotherDentist, clinicId, "test");
        given(sessionRepo.findById(sessionId)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.sendMessage(sessionId, dentistId, "texto"))
                .isInstanceOf(IllegalStateException.class);
        verify(agentPort, never()).ask(any());
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
