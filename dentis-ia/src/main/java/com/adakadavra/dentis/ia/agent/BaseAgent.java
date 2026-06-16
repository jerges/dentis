package com.adakadavra.dentis.ia.agent;

import com.adakadavra.dentis.ia.agent.tool.AgentTool;
import com.adakadavra.dentis.ia.domain.model.ChatRole;
import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class BaseAgent implements AgentPort {

    private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_TOOL_TURNS = 8;

    private final BedrockRuntimeAsyncClient asyncClient;
    private final VectorStore vectorStore;
    private final IaProperties props;
    private final List<AgentTool> tools;

    protected BaseAgent(BedrockRuntimeAsyncClient asyncClient,
                        VectorStore vectorStore,
                        IaProperties props,
                        List<AgentTool> tools) {
        this.asyncClient = asyncClient;
        this.vectorStore = vectorStore;
        this.props       = props;
        this.tools       = List.copyOf(tools);
    }

    protected abstract String systemPrompt();

    public abstract AgentType agentType();

    @Override
    public AgentResponse streamAsk(AgentRequest req, Consumer<AgentEvent> onEvent) {
        List<Document> contextDocs = retrieveContext(req.userText(), req.clinicId());
        String enrichedPrompt = contextDocs.isEmpty()
                ? req.userText()
                : buildEnrichedPrompt(req.userText(), contextDocs);

        var messages = new ArrayList<>(buildBedrockMessages(req.history(), enrichedPrompt));
        List<Tool> toolSpecs = buildToolSpecs();

        var totalInputTok  = new int[]{0};
        var totalOutputTok = new int[]{0};
        var fullText       = new StringBuilder();

        for (int turn = 0; turn < MAX_TOOL_TURNS; turn++) {
            var textBuf     = new StringBuilder();
            var stopReason  = new String[]{"end_turn"};
            var inputTok    = new int[]{0};
            var outputTok   = new int[]{0};
            var currentTool = new ToolUseAcc[]{null};
            var toolUseList = new ArrayList<ToolUseAcc>();

            var reqBuilder = ConverseStreamRequest.builder()
                    .modelId(props.getGenerationModelId())
                    .system(List.of(SystemContentBlock.builder().text(systemPrompt()).build()))
                    .messages(messages)
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(props.getMaxTokens())
                            .build());
            if (!toolSpecs.isEmpty()) {
                reqBuilder.toolConfig(ToolConfiguration.builder().tools(toolSpecs).build());
            }

            var visitor = ConverseStreamResponseHandler.Visitor.builder()
                    .onContentBlockStart(e -> {
                        var toolUse = e.start().toolUse();
                        if (toolUse != null) {
                            currentTool[0] = new ToolUseAcc(toolUse.toolUseId(), toolUse.name(), new StringBuilder());
                        }
                    })
                    .onContentBlockDelta(e -> {
                        var delta = e.delta();
                        if (delta.text() != null && !delta.text().isEmpty()) {
                            textBuf.append(delta.text());
                            onEvent.accept(new AgentEvent.Token(delta.text()));
                        } else if (delta.toolUse() != null && delta.toolUse().input() != null && currentTool[0] != null) {
                            currentTool[0].inputJson().append(delta.toolUse().input());
                        }
                    })
                    .onContentBlockStop(e -> {
                        if (currentTool[0] != null) {
                            toolUseList.add(currentTool[0]);
                            currentTool[0] = null;
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
                asyncClient.converseStream(reqBuilder.build(), handler).join();
            } catch (Exception e) {
                log.error("[bedrock-stream] failed model={}: {}", props.getGenerationModelId(), e.getMessage());
                throw new RuntimeException("Bedrock streaming failed: " + e.getMessage(), e);
            }

            totalInputTok[0]  += inputTok[0];
            totalOutputTok[0] += outputTok[0];
            fullText.append(textBuf);

            if (!"tool_use".equals(stopReason[0]) || toolUseList.isEmpty()) {
                break;
            }

            messages.add(buildAssistantToolUseMessage(textBuf.toString(), toolUseList));
            messages.add(buildToolResultMessage(toolUseList, onEvent));
        }

        return new AgentResponse(fullText.toString(), totalInputTok[0], totalOutputTok[0], buildCitations(contextDocs));
    }

    // ── Tool turn helpers ─────────────────────────────────────────────────────

    private Message buildAssistantToolUseMessage(String text, List<ToolUseAcc> toolUses) {
        List<ContentBlock> content = new ArrayList<>();
        if (!text.isBlank()) {
            content.add(ContentBlock.fromText(text));
        }
        for (ToolUseAcc acc : toolUses) {
            content.add(ContentBlock.fromToolUse(
                    ToolUseBlock.builder()
                            .toolUseId(acc.id())
                            .name(acc.name())
                            .input(parseJsonToDocument(acc.inputJson().toString()))
                            .build()));
        }
        return Message.builder().role(ConversationRole.ASSISTANT).content(content).build();
    }

    private Message buildToolResultMessage(List<ToolUseAcc> toolUses, Consumer<AgentEvent> onEvent) {
        List<ContentBlock> results = new ArrayList<>();
        for (ToolUseAcc acc : toolUses) {
            AgentTool tool = findTool(acc.name());
            onEvent.accept(new AgentEvent.ToolEvent(acc.name(), "searching", toolLabel(acc.name())));
            String result;
            if (tool != null) {
                Map<String, Object> inputMap = documentToMap(parseJsonToDocument(acc.inputJson().toString()));
                result = tool.execute(inputMap);
            } else {
                log.warn("[bedrock-tool] unknown tool requested: {}", acc.name());
                result = "Tool not available: " + acc.name();
            }
            onEvent.accept(new AgentEvent.ToolEvent(acc.name(), "done", toolLabel(acc.name())));
            results.add(ContentBlock.fromToolResult(
                    ToolResultBlock.builder()
                            .toolUseId(acc.id())
                            .content(List.of(ToolResultContentBlock.fromText(result)))
                            .build()));
        }
        return Message.builder().role(ConversationRole.USER).content(results).build();
    }

    private AgentTool findTool(String name) {
        return tools.stream().filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }

    private String toolLabel(String name) {
        AgentTool tool = findTool(name);
        return tool != null ? tool.label() : "Ejecutando herramienta...";
    }

    private List<Tool> buildToolSpecs() {
        return tools.stream()
                .map(t -> Tool.builder()
                        .toolSpec(ToolSpecification.builder()
                                .name(t.name())
                                .description(t.description())
                                .inputSchema(ToolInputSchema.builder().json(t.inputSchema()).build())
                                .build())
                        .build())
                .toList();
    }

    // ── JSON ↔ AWS Document ───────────────────────────────────────────────────

    private software.amazon.awssdk.core.document.Document parseJsonToDocument(String json) {
        if (json == null || json.isBlank()) {
            return software.amazon.awssdk.core.document.Document.mapBuilder().build();
        }
        try {
            Object parsed = MAPPER.readValue(json, Object.class);
            return objectToDocument(parsed);
        } catch (Exception e) {
            log.warn("[bedrock-tool] failed to parse tool input JSON: {}", json);
            return software.amazon.awssdk.core.document.Document.mapBuilder().build();
        }
    }

    @SuppressWarnings("unchecked")
    private software.amazon.awssdk.core.document.Document objectToDocument(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            var builder = software.amazon.awssdk.core.document.Document.mapBuilder();
            ((Map<String, Object>) map).forEach((k, v) -> builder.putDocument(k, objectToDocument(v)));
            return builder.build();
        } else if (obj instanceof List<?> list) {
            var builder = software.amazon.awssdk.core.document.Document.listBuilder();
            list.forEach(item -> builder.addDocument(objectToDocument(item)));
            return builder.build();
        } else if (obj instanceof Boolean b) {
            return software.amazon.awssdk.core.document.Document.fromBoolean(b);
        } else if (obj instanceof Number n) {
            return software.amazon.awssdk.core.document.Document.fromNumber(n.toString());
        } else if (obj == null) {
            return software.amazon.awssdk.core.document.Document.fromNull();
        } else {
            return software.amazon.awssdk.core.document.Document.fromString(obj.toString());
        }
    }

    private Map<String, Object> documentToMap(software.amazon.awssdk.core.document.Document doc) {
        if (!doc.isMap()) return Map.of();
        Map<String, Object> result = new HashMap<>();
        doc.asMap().forEach((k, v) -> result.put(k, documentToObject(v)));
        return result;
    }

    private Object documentToObject(software.amazon.awssdk.core.document.Document doc) {
        if (doc.isMap())     return documentToMap(doc);
        if (doc.isList())    return doc.asList().stream().map(this::documentToObject).toList();
        if (doc.isString())  return doc.asString();
        if (doc.isNumber())  return doc.asNumber().doubleValue();
        if (doc.isBoolean()) return doc.asBoolean();
        return null;
    }

    // ── Message building ──────────────────────────────────────────────────────

    private List<Message> buildBedrockMessages(List<ConversationTurn> history, String lastUserText) {
        var messages = new ArrayList<Message>();
        ConversationRole lastRole = null;
        for (ConversationTurn turn : history) {
            var role = turn.role() == ChatRole.USER ? ConversationRole.USER : ConversationRole.ASSISTANT;
            if (role.equals(lastRole)) continue;
            messages.add(Message.builder()
                    .role(role)
                    .content(ContentBlock.fromText(turn.content()))
                    .build());
            lastRole = role;
        }
        if (lastRole == ConversationRole.USER) {
            messages.set(messages.size() - 1,
                    Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(lastUserText))
                            .build());
        } else {
            messages.add(Message.builder()
                    .role(ConversationRole.USER)
                    .content(ContentBlock.fromText(lastUserText))
                    .build());
        }
        return messages;
    }

    // ── RAG helpers ───────────────────────────────────────────────────────────

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

    // ── Internal record ───────────────────────────────────────────────────────

    private record ToolUseAcc(String id, String name, StringBuilder inputJson) {}
}
