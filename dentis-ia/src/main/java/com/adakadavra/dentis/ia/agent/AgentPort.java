package com.adakadavra.dentis.ia.agent;

import com.adakadavra.dentis.ia.domain.model.ChatRole;

import java.util.List;
import java.util.UUID;

public interface AgentPort {

    AgentResponse ask(AgentRequest request);

    record AgentRequest(
            UUID sessionId,
            UUID clinicId,
            String userText,
            List<ConversationTurn> history
    ) {}

    record AgentResponse(String text, int inputTokens, int outputTokens, String citations) {}

    record ConversationTurn(ChatRole role, String content) {}
}