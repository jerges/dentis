package com.adakadavra.dentis.ia.infrastructure.bedrock;

import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BedrockTitanEmbeddingModelTest {

    @Mock BedrockRuntimeClient bedrockRuntimeClient;

    private IaProperties props;
    private BedrockTitanEmbeddingModel model;

    @BeforeEach
    void setUp() {
        props = new IaProperties();
        model = new BedrockTitanEmbeddingModel(bedrockRuntimeClient, props);
    }

    // ── embed(String) ─────────────────────────────────────────────────────────

    @Test
    void embedStringReturnsFloatsFromJsonResponse() {
        stubBedrockResponse("{\"embedding\":[0.1,0.2,0.3]}");

        float[] result = model.embed("texto odontológico");

        assertThat(result).hasSize(3);
        assertThat(result[0]).isCloseTo(0.1f, within(0.001f));
        assertThat(result[1]).isCloseTo(0.2f, within(0.001f));
        assertThat(result[2]).isCloseTo(0.3f, within(0.001f));
    }

    @Test
    void embedInvokesBedrockWithConfiguredModelId() {
        stubBedrockResponse("{\"embedding\":[0.5]}");

        model.embed("caries dental");

        verify(bedrockRuntimeClient).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void embedSdkExceptionIsWrappedInRuntimeException() {
        given(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .willThrow(new RuntimeException("Bedrock connection error"));

        assertThatThrownBy(() -> model.embed("caries"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bedrock Titan embedding failed");
    }

    // ── embed(Document) ───────────────────────────────────────────────────────

    @Test
    void embedDocumentDelegatesToEmbedString() {
        stubBedrockResponse("{\"embedding\":[0.9,0.8]}");
        Document doc = new Document("texto del documento clínico");

        float[] result = model.embed(doc);

        assertThat(result).hasSize(2);
    }

    // ── dimensions() ─────────────────────────────────────────────────────────

    @Test
    void dimensionsReturnsConfiguredValue() {
        assertThat(model.dimensions()).isEqualTo(props.getEmbeddingDimensions());
    }

    @Test
    void dimensionsDefaultsTo1024() {
        assertThat(model.dimensions()).isEqualTo(1024);
    }

    // ── call(EmbeddingRequest) ────────────────────────────────────────────────

    @Test
    void callReturnsEmbeddingForEachInstruction() {
        stubBedrockResponse("{\"embedding\":[0.1,0.2]}");

        EmbeddingRequest request = new EmbeddingRequest(
                List.of("texto uno", "texto dos"), null);
        EmbeddingResponse response = model.call(request);

        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().get(0).getIndex()).isEqualTo(0);
        assertThat(response.getResults().get(1).getIndex()).isEqualTo(1);
    }

    @Test
    void callWithSingleInstructionReturnsSingleEmbedding() {
        stubBedrockResponse("{\"embedding\":[0.5,0.6,0.7]}");

        EmbeddingRequest request = new EmbeddingRequest(List.of("endodoncia"), null);
        EmbeddingResponse response = model.call(request);

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getOutput()).hasSize(3);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stubBedrockResponse(String json) {
        InvokeModelResponse response = mock(InvokeModelResponse.class);
        given(response.body()).willReturn(SdkBytes.fromUtf8String(json));
        given(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).willReturn(response);
    }
}
