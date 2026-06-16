package com.adakadavra.dentis.ia.domain.service;

import com.adakadavra.dentis.ia.agent.AgentPort;
import com.adakadavra.dentis.ia.domain.model.ChatMessage;
import com.adakadavra.dentis.ia.domain.model.ChatRole;
import com.adakadavra.dentis.ia.domain.model.ChatSession;
import com.adakadavra.dentis.ia.domain.repository.ChatMessageRepository;
import com.adakadavra.dentis.ia.domain.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class IaChatService {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final AgentPort agentPort;

    @Transactional
    public ChatSession createSession(UUID dentistId, UUID clinicId, String title) {
        ChatSession session = ChatSession.builder()
                .dentistId(dentistId)
                .clinicId(clinicId)
                .title(title != null && !title.isBlank() ? title : "Nueva consulta")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return sessionRepo.save(session);
    }

    public List<ChatSession> getSessions(UUID dentistId) {
        return sessionRepo.findByDentistIdOrderByUpdatedAtDesc(dentistId);
    }

    public List<ChatMessage> getMessages(UUID sessionId, UUID dentistId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
        if (!session.getDentistId().equals(dentistId)) {
            throw new IllegalStateException("Session does not belong to you");
        }
        return messageRepo.findBySessionIdOrderByCreatedAt(sessionId);
    }

    @Transactional
    public ChatMessage sendMessage(UUID sessionId, UUID dentistId, String userText) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
        if (!session.getDentistId().equals(dentistId)) {
            throw new IllegalStateException("Session does not belong to you");
        }

        messageRepo.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(ChatRole.USER)
                .content(userText)
                .createdAt(LocalDateTime.now())
                .build());

        List<ChatMessage> history = messageRepo.findBySessionIdOrderByCreatedAt(sessionId);
        List<AgentPort.ConversationTurn> turns = history.stream()
                .filter(m -> !m.getContent().equals(RelevanceGuard.OFF_TOPIC_RESPONSE))
                .map(m -> new AgentPort.ConversationTurn(m.getRole(), m.getContent()))
                .toList();

        AgentPort.AgentResponse result = agentPort.ask(new AgentPort.AgentRequest(
                sessionId, session.getClinicId(), userText, turns));

        ChatMessage reply = messageRepo.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(ChatRole.ASSISTANT)
                .content(result.text())
                .citations(result.citations())
                .inputTokens(result.inputTokens())
                .outputTokens(result.outputTokens())
                .createdAt(LocalDateTime.now())
                .build());

        sessionRepo.touchUpdatedAt(sessionId);
        return reply;
    }

    @Transactional
    public ChatMessage streamMessage(UUID sessionId, UUID dentistId, String userText, Consumer<String> onToken) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
        if (!session.getDentistId().equals(dentistId)) {
            throw new IllegalStateException("Session does not belong to you");
        }

        messageRepo.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(ChatRole.USER)
                .content(userText)
                .createdAt(LocalDateTime.now())
                .build());

        List<ChatMessage> history = messageRepo.findBySessionIdOrderByCreatedAt(sessionId);
        List<AgentPort.ConversationTurn> turns = history.stream()
                .filter(m -> !m.getContent().equals(RelevanceGuard.OFF_TOPIC_RESPONSE))
                .map(m -> new AgentPort.ConversationTurn(m.getRole(), m.getContent()))
                .toList();

        AgentPort.AgentResponse result = agentPort.streamAsk(
                new AgentPort.AgentRequest(sessionId, session.getClinicId(), userText, turns),
                onToken);

        ChatMessage reply = messageRepo.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(ChatRole.ASSISTANT)
                .content(result.text())
                .citations(result.citations())
                .inputTokens(result.inputTokens())
                .outputTokens(result.outputTokens())
                .createdAt(LocalDateTime.now())
                .build());

        sessionRepo.touchUpdatedAt(sessionId);
        return reply;
    }

    @Transactional
    public void deleteSession(UUID sessionId, UUID dentistId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
        if (!session.getDentistId().equals(dentistId)) {
            throw new IllegalStateException("Session does not belong to you");
        }
        sessionRepo.deleteById(sessionId);
    }
}
