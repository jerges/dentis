package com.adakadavra.dentis.ia.agent;

import com.adakadavra.dentis.ia.domain.model.ChatRole;
import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class BaseAgent implements AgentPort {

    private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

    private final ChatModel chatModel;
    private final BedrockRuntimeAsyncClient asyncClient;
    private final VectorStore vectorStore;
    private final IaProperties props;

    protected BaseAgent(ChatModel chatModel, BedrockRuntimeAsyncClient asyncClient,
                        VectorStore vectorStore, IaProperties props) {
        this.chatModel   = chatModel;
        this.asyncClient = asyncClient;
        this.vectorStore = vectorStore;
        this.props       = props;
    }

    protected abstract String systemPrompt();

    public abstract AgentType agentType();

    @Override
    public AgentResponse ask(AgentRequest req) {
        List<Document> contextDocs = retrieveContext(req.userText(), req.clinicId());
        String enrichedPrompt = contextDocs.isEmpty()
                ? req.userText()
                : buildEnrichedPrompt(req.userText(), contextDocs);

        List<Message> messages = buildMessages(req.history(), enrichedPrompt);
        var response = chatModel.call(new Prompt(messages));
        String text = response.getResult().getOutput().getText();
        var usage = response.getMetadata().getUsage();
        int inputTokens  = usage != null ? (int) usage.getPromptTokens()     : 0;
        int outputTokens = usage != null ? (int) usage.getCompletionTokens() : 0;

        return new AgentResponse(text, inputTokens, outputTokens, buildCitations(contextDocs));
    }

    @Override
    public AgentResponse streamAsk(AgentRequest req, Consumer<String> onToken) {
        List<Document> contextDocs = retrieveContext(req.userText(), req.clinicId());
        String enrichedPrompt = contextDocs.isEmpty()
                ? req.userText()
                : buildEnrichedPrompt(req.userText(), contextDocs);

        var bedrockMessages = buildBedrockMessages(req.history(), enrichedPrompt);

        var streamReq = ConverseStreamRequest.builder()
                .modelId(props.getGenerationModelId())
                .system(List.of(SystemContentBlock.builder().text(systemPrompt()).build()))
                .messages(bedrockMessages)
                .inferenceConfig(InferenceConfiguration.builder()
                        .maxTokens(props.getMaxTokens())
                        .build())
                .build();

        var textBuf    = new StringBuilder();
        var inputTok   = new int[]{0};
        var outputTok  = new int[]{0};
        var stopReason = new String[]{"end_turn"};

        var visitor = ConverseStreamResponseHandler.Visitor.builder()
                .onContentBlockDelta(e -> {
                    var delta = e.delta();
                    if (delta.text() != null && !delta.text().isEmpty()) {
                        textBuf.append(delta.text());
                        onToken.accept(delta.text());
                    }
                })
                .onMessageStop(e -> stopReason[0] = e.stopReasonAsString())
                .onMetadata(e -> {
                    if (e.usage() != null) {
                        inputTok[0]  = e.usage().inputTokens()  != null ? e.usage().inputTokens()  : 0;
                        outputTok[0] = e.usage().outputTokens() != null ? e.usage().outputTokens() : 0;
                    }
                })
                .build();

        var handler = ConverseStreamResponseHandler.builder()
                .subscriber(visitor)
                .onError(ex -> log.error("[bedrock-stream] error model={}: {}", props.getGenerationModelId(), ex.getMessage()))
                .build();

        try {
            asyncClient.converseStream(streamReq, handler).join();
        } catch (Exception e) {
            log.error("[bedrock-stream] failed model={}: {}", props.getGenerationModelId(), e.getMessage());
            throw new RuntimeException("Bedrock streaming failed: " + e.getMessage(), e);
        }

        return new AgentResponse(textBuf.toString(), inputTok[0], outputTok[0], buildCitations(contextDocs));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<Message> buildMessages(List<ConversationTurn> history, String lastUserText) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt()));
        for (ConversationTurn turn : history) {
            messages.add(turn.role() == ChatRole.USER
                    ? new UserMessage(turn.content())
                    : new AssistantMessage(turn.content()));
        }
        messages.add(new UserMessage(lastUserText));
        return messages;
    }

    private List<software.amazon.awssdk.services.bedrockruntime.model.Message> buildBedrockMessages(
            List<ConversationTurn> history, String lastUserText) {
        var messages = new ArrayList<software.amazon.awssdk.services.bedrockruntime.model.Message>();
        ConversationRole lastRole = null;
        for (ConversationTurn turn : history) {
            var role = turn.role() == ChatRole.USER ? ConversationRole.USER : ConversationRole.ASSISTANT;
            if (role.equals(lastRole)) continue; // avoid consecutive same-role messages
            messages.add(software.amazon.awssdk.services.bedrockruntime.model.Message.builder()
                    .role(role)
                    .content(ContentBlock.fromText(turn.content()))
                    .build());
            lastRole = role;
        }
        // Ensure last message is from user
        if (lastRole == ConversationRole.USER) {
            // replace last user message with enriched prompt
            messages.set(messages.size() - 1,
                    software.amazon.awssdk.services.bedrockruntime.model.Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(lastUserText))
                            .build());
        } else {
            messages.add(software.amazon.awssdk.services.bedrockruntime.model.Message.builder()
                    .role(ConversationRole.USER)
                    .content(ContentBlock.fromText(lastUserText))
                    .build());
        }
        return messages;
    }

    private List<Document> retrieveContext(String query, UUID clinicId) {
        if (clinicId == null) return List.of();
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

    private String buildCitations(List<Document> docs) {
        String citations = docs.stream()
                .map(d -> (String) d.getMetadata().get("s3_key"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(","));
        return citations.isBlank() ? null : citations;
    }
}