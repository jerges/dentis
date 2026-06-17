package com.adakadavra.dentis.ia.agent.dental;

import com.adakadavra.dentis.ia.agent.AgentPort;
import com.adakadavra.dentis.ia.agent.AgentPort.AgentRequest;
import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;

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
        agent = new DentalAgent(asyncClient, vectorStore, props, List.of());
    }

    @Test
    @DisplayName("shouldBeInvokedWhenDentalQueryIsSent")
    void shouldBeInvokedWhenDentalQueryIsSent() {
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), null, "¿Cómo tratar una caries?", List.of());

        agent.streamAsk(req, event -> {});

        verify(asyncClient).converseStream(any(ConverseStreamRequest.class), any());
    }

    // ── RAG context retrieval ─────────────────────────────────────────────────

    @Test
    @DisplayName("shouldBeSkippedWhenNullClinicIdIsProvided")
    void shouldBeSkippedWhenNullClinicIdIsProvided() {
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), null, "caries molar", List.of());
        agent.streamAsk(req, event -> {});

        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("shouldBeSearchedWithFilterWhenClinicIdIsProvided")
    void shouldBeSearchedWithFilterWhenClinicIdIsProvided() {
        UUID clinicId = UUID.randomUUID();
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), clinicId, "caries molar", List.of());
        agent.streamAsk(req, event -> {});

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("caries molar");
        assertThat(captor.getValue().getTopK()).isEqualTo(props.getTopK());
        assertThat(captor.getValue().getFilterExpression().toString()).contains(clinicId.toString());
    }

    // ── Stream-specific behaviour ─────────────────────────────────────────────

    @Test
    @DisplayName("shouldBeUsingModelIdFromProperties")
    void shouldBeUsingModelIdFromProperties() {
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "periodontitis", List.of());
        agent.streamAsk(req, event -> {});

        ArgumentCaptor<ConverseStreamRequest> captor = ArgumentCaptor.forClass(ConverseStreamRequest.class);
        verify(asyncClient).converseStream(captor.capture(), any());
        assertThat(captor.getValue().modelId()).isEqualTo(props.getGenerationModelId());
    }

    @Test
    @DisplayName("shouldBeIncludingSystemPromptWithOdontologicalKeyword")
    void shouldBeIncludingSystemPromptWithOdontologicalKeyword() {
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "endodoncia", List.of());
        agent.streamAsk(req, event -> {});

        ArgumentCaptor<ConverseStreamRequest> captor = ArgumentCaptor.forClass(ConverseStreamRequest.class);
        verify(asyncClient).converseStream(captor.capture(), any());
        assertThat(captor.getValue().system()).isNotEmpty();
        assertThat(captor.getValue().system().get(0).text()).contains("odontológico");
    }

    @Test
    @DisplayName("shouldBeReturningZeroTokensWhenStreamIsEmpty")
    void shouldBeReturningZeroTokensWhenStreamIsEmpty() {
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());
        given(asyncClient.converseStream(any(ConverseStreamRequest.class), any()))
                .willReturn(CompletableFuture.completedFuture(null));

        var req = new AgentRequest(UUID.randomUUID(), UUID.randomUUID(), "molar infectado", List.of());
        AgentPort.AgentResponse response = agent.streamAsk(req, event -> {});

        assertThat(response.inputTokens()).isZero();
        assertThat(response.outputTokens()).isZero();
    }

    @Test
    @DisplayName("shouldBeOfTypeDental")
    void shouldBeOfTypeDental() {
        assertThat(agent.agentType().name()).isEqualTo("DENTAL");
    }
}
