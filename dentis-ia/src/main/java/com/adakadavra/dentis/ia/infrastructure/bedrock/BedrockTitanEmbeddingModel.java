package com.adakadavra.dentis.ia.infrastructure.bedrock;

import com.adakadavra.dentis.ia.infrastructure.config.IaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;

import java.util.ArrayList;
import java.util.List;

public class BedrockTitanEmbeddingModel implements EmbeddingModel {

    private final BedrockRuntimeClient client;
    private final IaProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BedrockTitanEmbeddingModel(BedrockRuntimeClient client, IaProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> texts = request.getInstructions();
        for (int i = 0; i < texts.size(); i++) {
            embeddings.add(new Embedding(embed(texts.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        try {
            String body = objectMapper.writeValueAsString(
                    new TitanEmbedRequest(text, props.getEmbeddingDimensions(), true));
            var response = client.invokeModel(InvokeModelRequest.builder()
                    .modelId(props.getEmbeddingModelId())
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(body))
                    .build());
            JsonNode root = objectMapper.readTree(response.body().asUtf8String());
            JsonNode embNode = root.get("embedding");
            float[] result = new float[embNode.size()];
            for (int i = 0; i < embNode.size(); i++) {
                result[i] = embNode.get(i).floatValue();
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Bedrock Titan embedding failed", e);
        }
    }

    @Override
    public int dimensions() {
        return props.getEmbeddingDimensions();
    }

    private record TitanEmbedRequest(String inputText, int dimensions, boolean normalize) {}
}