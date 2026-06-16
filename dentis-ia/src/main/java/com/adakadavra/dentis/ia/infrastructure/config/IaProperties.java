package com.adakadavra.dentis.ia.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "dentis.ia")
@Getter
@Setter
public class IaProperties {

    private boolean enabled = true;
    private String generationModelId = "us.amazon.nova-pro-v1:0";
    private String embeddingModelId = "amazon.titan-embed-text-v2:0";
    private int embeddingDimensions = 1024;
    private int topK = 5;
    private int maxTokens = 1024;
    private double minScore = 0.72;
    private int chunkSize = 800;

    /** Per-model pricing configuration. Key is a substring matched against the model ID. */
    private Map<String, ModelPricing> modelPricing = new LinkedHashMap<>();

    /**
     * Resolves pricing for the given model ID by matching the first entry whose key
     * is a substring of the model ID. Falls back to defaults if no match is found.
     */
    public ModelPricing resolvePricing(String modelId) {
        if (modelId == null) return new ModelPricing();
        return modelPricing.entrySet().stream()
                .filter(e -> modelId.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(ModelPricing::new);
    }

    @Getter
    @Setter
    public static class ModelPricing {
        /** Raw cost charged by the provider per 1M input tokens (USD). */
        private double inputPricePer1M = 0.80;
        /** Raw cost charged by the provider per 1M output tokens (USD). */
        private double outputPricePer1M = 3.20;
        /** Multiplier applied on top of raw cost to compute the billed price. */
        private double marginMultiplier = 3.0;

        /** Cost charged to the provider (raw AWS / Anthropic cost). */
        public double rawCost(long inputTokens, long outputTokens) {
            return (inputTokens / 1_000_000.0 * inputPricePer1M)
                 + (outputTokens / 1_000_000.0 * outputPricePer1M);
        }

        /** Price billed to the client (raw cost × margin multiplier). */
        public double billedCost(long inputTokens, long outputTokens) {
            return rawCost(inputTokens, outputTokens) * marginMultiplier;
        }
    }
}
