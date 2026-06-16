package com.adakadavra.dentis.ia.agent;

import com.adakadavra.dentis.ia.domain.model.ChatRole;
import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class BaseAgent implements AgentPort {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final IaProperties props;

    protected BaseAgent(ChatModel chatModel, VectorStore vectorStore, IaProperties props) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.props = props;
    }

    protected abstract String systemPrompt();

    public abstract AgentType agentType();

    @Override
    public AgentResponse ask(AgentRequest req) {
        List<Document> contextDocs = retrieveContext(req.userText(), req.clinicId());

        String enrichedPrompt = contextDocs.isEmpty()
                ? req.userText()
                : buildEnrichedPrompt(req.userText(), contextDocs);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt()));
        for (ConversationTurn turn : req.history()) {
            messages.add(turn.role() == ChatRole.USER
                    ? new UserMessage(turn.content())
                    : new AssistantMessage(turn.content()));
        }
        messages.add(new UserMessage(enrichedPrompt));

        var response = chatModel.call(new Prompt(messages));
        String text = response.getResult().getOutput().getText();
        var usage = response.getMetadata().getUsage();
        int inputTokens  = usage != null ? (int) usage.getPromptTokens()     : 0;
        int outputTokens = usage != null ? (int) usage.getCompletionTokens() : 0;

        String citations = contextDocs.stream()
                .map(d -> (String) d.getMetadata().get("s3_key"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(","));

        return new AgentResponse(text, inputTokens, outputTokens, citations.isBlank() ? null : citations);
    }

    private List<Document> retrieveContext(String query, UUID clinicId) {
        if (clinicId == null) return List.of();
        // Solo recupera documentos de la carpeta Base de Conocimiento (kb == 'true')
        var searchRequest = SearchRequest.builder()
                .query(query)
                .topK(props.getTopK())
                .similarityThreshold(props.getMinScore())
                .filterExpression("clinic_id == '" + clinicId + "' && kb == 'true'")
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    private String buildEnrichedPrompt(String userText, List<Document> docs) {
        StringBuilder sb = new StringBuilder("CONTEXTO CLÍNICO RECUPERADO DE LOS DOCUMENTOS DE LA CLÍNICA:\n");
        for (int i = 0; i < docs.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(docs.get(i).getText()).append("\n\n");
        }
        sb.append("CONSULTA DEL DENTISTA:\n").append(userText);
        return sb.toString();
    }
}