package com.adakadavra.dentis.ia.agent.dental;

import com.adakadavra.dentis.ia.agent.AgentPort;
import com.adakadavra.dentis.ia.agent.AgentPort.AgentRequest;
import com.adakadavra.dentis.ia.agent.AgentPort.ConversationTurn;
import com.adakadavra.dentis.ia.domain.model.ChatRole;
import com.adakadavra.dentis.ia.domain.service.RelevanceGuard;
import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DentalAgentTest {

    @Mock ChatModel chatModel;
    @Mock VectorStore vectorStore;
    @Mock ChatResponse chatResponse;
    @Mock Generation generation;
    @Mock AssistantMessage assistantMessage;
    @Mock ChatResponseMetadata responseMetadata;
    @Mock Usage usage;

    private IaProperties props;
    private DentalAgent agent;

    @BeforeEach
    void setUp() {
        props = new IaProperties();
        agent = new DentalAgent(chatModel, vectorStore, props, new RelevanceGuard());
    }

    // ── Relevance guard ───────────────────────────────────────────────────────

    @Test
    void offTopicQueryReturnsGuardResponseWithoutCallingModel() {
        var req = new AgentRequest(UUID.randomUUID(), null, "¿Cuál es la capital de Francia?", List.of());

        AgentPort.AgentResponse response = agent.ask(req);

        assertThat(response.text()).isEqualTo(RelevanceGuard.OFF_TOPIC_RESPONSE);
        assertThat(response.inputTokens()).isZero();
        assertThat(response.outputTokens()).isZero();
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void dentalQueryPassesRelevanceGuard() {
        // clinicId null → no VectorStore call
        stubChatModel("Tratamiento recomendado.");

        var req = new AgentRequest(UUID.randomUUID(), null, "¿Cómo tratar una caries?", List.of());
        AgentPort.AgentResponse response = agent.ask(req);

        assertThat(response.text()).isEqualTo("Tratamiento recomendado.");
        verify(chatModel).call(any(Prompt.class));
    }

    // ── RAG context retrieval ─────────────────────────────────────────────────

    @Test
    void nullClinicIdSkipsVectorStoreSearch() {
        stubChatModel("respuesta");
        var req = new AgentRequest(UUID.randomUUID(), null, "caries molar", List.of());

        agent.ask(req);

        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void clinicIdTriggersVectorStoreSearchWithFilter() {
        UUID clinicId = UUID.randomUUID();
        stubChatModel("respuesta");
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        var req = new AgentRequest(UUID.randomUUID(), clinicId, "caries molar", List.of());
        agent.ask(req);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest captured = captor.getValue();
        assertThat(captured.getQuery()).isEqualTo("caries molar");
        assertThat(captured.getTopK()).isEqualTo(props.getTopK());
        assertThat(captured.getFilterExpression().toString()).contains(clinicId.toString());
    }

    @Test
    void emptyVectorStoreResultSendsRawUserPrompt() {
        UUID clinicId = UUID.randomUUID();
        stubChatModel("respuesta");
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        var req = new AgentRequest(UUID.randomUUID(), clinicId, "endodoncia protocolo", List.of());
        agent.ask(req);

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        assertThat(lastUserMessage(captor.getValue())).isEqualTo("endodoncia protocolo");
    }

    @Test
    void contextFromDocsIsIncludedInEnrichedPrompt() {
        UUID clinicId = UUID.randomUUID();
        stubChatModel("respuesta");
        Document doc = new Document("fragmento clínico relevante",
                Map.of("s3_key", "clinic/protocolo.pdf", "clinic_id", clinicId.toString()));
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of(doc));

        var req = new AgentRequest(UUID.randomUUID(), clinicId, "caries molar", List.of());
        agent.ask(req);

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        assertThat(lastUserMessage(captor.getValue()))
                .contains("CONTEXTO CLÍNICO")
                .contains("fragmento clínico relevante")
                .contains("CONSULTA DEL DENTISTA")
                .contains("caries molar");
    }

    @Test
    void citationsAreJoinedFromDocMetadata() {
        UUID clinicId = UUID.randomUUID();
        stubChatModel("respuesta");
        Document doc1 = new Document("texto1", Map.of("s3_key", "clinic/a.pdf"));
        Document doc2 = new Document("texto2", Map.of("s3_key", "clinic/b.pdf"));
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of(doc1, doc2));

        var req = new AgentRequest(UUID.randomUUID(), clinicId, "caries", List.of());
        AgentPort.AgentResponse response = agent.ask(req);

        assertThat(response.citations()).contains("clinic/a.pdf").contains("clinic/b.pdf");
    }

    @Test
    void noCitationsWhenDocsHaveNoS3Key() {
        UUID clinicId = UUID.randomUUID();
        stubChatModel("respuesta");
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(new Document("texto sin metadata", Map.of())));

        var req = new AgentRequest(UUID.randomUUID(), clinicId, "caries", List.of());
        AgentPort.AgentResponse response = agent.ask(req);

        assertThat(response.citations()).isNull();
    }

    // ── Token usage ───────────────────────────────────────────────────────────

    @Test
    void tokenUsageIsPropagatedFromModelResponse() {
        given(usage.getPromptTokens()).willReturn(120);
        given(usage.getCompletionTokens()).willReturn(60);
        given(responseMetadata.getUsage()).willReturn(usage);
        given(chatResponse.getMetadata()).willReturn(responseMetadata);
        given(assistantMessage.getText()).willReturn("ok");
        given(generation.getOutput()).willReturn(assistantMessage);
        given(chatResponse.getResult()).willReturn(generation);
        given(chatModel.call(any(Prompt.class))).willReturn(chatResponse);
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "endodoncia", List.of());
        AgentPort.AgentResponse response = agent.ask(req);

        assertThat(response.inputTokens()).isEqualTo(120);
        assertThat(response.outputTokens()).isEqualTo(60);
    }

    @Test
    void nullUsageDefaultsToZeroTokens() {
        given(responseMetadata.getUsage()).willReturn(null);
        given(chatResponse.getMetadata()).willReturn(responseMetadata);
        given(assistantMessage.getText()).willReturn("ok");
        given(generation.getOutput()).willReturn(assistantMessage);
        given(chatResponse.getResult()).willReturn(generation);
        given(chatModel.call(any(Prompt.class))).willReturn(chatResponse);
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "molar", List.of());
        AgentPort.AgentResponse response = agent.ask(req);

        assertThat(response.inputTokens()).isZero();
        assertThat(response.outputTokens()).isZero();
    }

    // ── Conversation history ──────────────────────────────────────────────────

    @Test
    void systemMessageIsFirstInPrompt() {
        stubChatModel("respuesta");
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "caries", List.of());
        agent.ask(req);

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        assertThat(captor.getValue().getInstructions().get(0)).isInstanceOf(SystemMessage.class);
    }

    @Test
    void conversationHistoryIsIncludedInPrompt() {
        stubChatModel("respuesta");
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        List<ConversationTurn> history = List.of(
                new ConversationTurn(ChatRole.USER, "mensaje anterior"),
                new ConversationTurn(ChatRole.ASSISTANT, "respuesta anterior")
        );
        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "endodoncia en molar", history);
        agent.ask(req);

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        var messages = captor.getValue().getInstructions();
        // system + 2 history + 1 user = 4
        assertThat(messages).hasSize(4);
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(2)).isInstanceOf(AssistantMessage.class);
    }

    @Test
    void agentTypeIsDental() {
        assertThat(agent.agentType().name()).isEqualTo("DENTAL");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stubChatModel(String text) {
        given(assistantMessage.getText()).willReturn(text);
        given(generation.getOutput()).willReturn(assistantMessage);
        given(chatResponse.getResult()).willReturn(generation);
        given(responseMetadata.getUsage()).willReturn(usage);
        given(chatResponse.getMetadata()).willReturn(responseMetadata);
        given(chatModel.call(any(Prompt.class))).willReturn(chatResponse);
    }

    private String lastUserMessage(Prompt prompt) {
        var messages = prompt.getInstructions();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage um) return um.getText();
        }
        return "";
    }
}
