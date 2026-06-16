package com.adakadavra.dentis.ia.agent.dental;

import com.adakadavra.dentis.ia.agent.AgentPort;
import com.adakadavra.dentis.ia.agent.AgentPort.AgentRequest;
import com.adakadavra.dentis.ia.domain.service.RelevanceGuard;
import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DentalAgentTest {

    @Mock BedrockRuntimeAsyncClient asyncClient;
    @Mock VectorStore vectorStore;

    private IaProperties props;
    private DentalAgent agent;

    @BeforeEach
    void setUp() {
        props = new IaProperties();
        agent = new DentalAgent(asyncClient, vectorStore, props, new RelevanceGuard());
    }

    // ── Relevance guard ───────────────────────────────────────────────────────

    @Test
    void offTopicQueryReturnsGuardResponseWithoutCallingBedrock() {
        var req = new AgentRequest(UUID.randomUUID(), null, "¿Cuál es la capital de Francia?", List.of());
        List<String> received = new ArrayList<>();

        AgentPort.AgentResponse response = agent.streamAsk(req, received::add);

        assertThat(response.text()).isEqualTo(RelevanceGuard.OFF_TOPIC_RESPONSE);
        assertThat(received).containsExactly(RelevanceGuard.OFF_TOPIC_RESPONSE);
        assertThat(response.inputTokens()).isZero();
        verifyNoInteractions(asyncClient);
    }

    @Test
    void dentalQueryInvokesBedrockConverseStream() {
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), null, "¿Cómo tratar una caries?", List.of());

        agent.streamAsk(req, tok -> {});

        verify(asyncClient).converseStream(any(ConverseStreamRequest.class), any());
    }

    // ── RAG context retrieval ─────────────────────────────────────────────────

    @Test
    void nullClinicIdSkipsVectorStoreSearch() {
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), null, "caries molar", List.of());
        agent.streamAsk(req, tok -> {});

        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void clinicIdTriggersVectorStoreSearchWithFilter() {
        UUID clinicId = UUID.randomUUID();
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), clinicId, "caries molar", List.of());
        agent.streamAsk(req, tok -> {});

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("caries molar");
        assertThat(captor.getValue().getTopK()).isEqualTo(props.getTopK());
        assertThat(captor.getValue().getFilterExpression().toString()).contains(clinicId.toString());
    }

    // ── Stream-specific behaviour ─────────────────────────────────────────────

    @Test
    void streamAskUsesModelIdFromProperties() {
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "periodontitis", List.of());
        agent.streamAsk(req, tok -> {});

        ArgumentCaptor<ConverseStreamRequest> captor = ArgumentCaptor.forClass(ConverseStreamRequest.class);
        verify(asyncClient).converseStream(captor.capture(), any());
        assertThat(captor.getValue().modelId()).isEqualTo(props.getGenerationModelId());
    }

    @Test
    void streamAskIncludesSystemPromptInRequest() {
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "endodoncia", List.of());
        agent.streamAsk(req, tok -> {});

        ArgumentCaptor<ConverseStreamRequest> captor = ArgumentCaptor.forClass(ConverseStreamRequest.class);
        verify(asyncClient).converseStream(captor.capture(), any());
        assertThat(captor.getValue().system()).isNotEmpty();
        assertThat(captor.getValue().system().get(0).text()).contains("odontológico");
    }

    @Test
    void streamAskWithEmptyStreamReturnsZeroTokens() {
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "molar infectado", List.of());
        AgentPort.AgentResponse response = agent.streamAsk(req, tok -> {});

        assertThat(response.inputTokens()).isZero();
        assertThat(response.outputTokens()).isZero();
    }

    @Test
    void agentTypeIsDental() {
        assertThat(agent.agentType().name()).isEqualTo("DENTAL");
    }
}
