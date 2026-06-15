package com.adakadavra.dentis.ia.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
}
