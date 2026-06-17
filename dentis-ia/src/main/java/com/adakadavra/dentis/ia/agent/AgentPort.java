package com.adakadavra.dentis.ia.agent;

import com.adakadavra.dentis.ia.domain.model.ChatRole;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface AgentPort {

    AgentResponse streamAsk(AgentRequest request, Consumer<AgentEvent> onEvent);

    sealed interface AgentEvent {
        record Token(String text) implements AgentEvent {}
        record ToolEvent(String toolName, String status, String label) implements AgentEvent {}
        record ThinkingChunk(String text) implements AgentEvent {}
    }

    record AgentRequest(
            UUID sessionId,
            UUID clinicId,
            String userText,
            List<ConversationTurn> history
    ) {}

    record AgentResponse(String text, int inputTokens, int outputTokens, String citations) {}

    record ConversationTurn(ChatRole role, String content) {}
}
