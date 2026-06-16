package com.adakadavra.dentis.ia.infrastructure.config;

import com.adakadavra.dentis.ia.infrastructure.bedrock.BedrockTitanEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class BedrockConfig {

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(@Value("${AWS_REGION:us-east-1}") String region) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient(@Value("${AWS_REGION:us-east-1}") String region) {
        return BedrockRuntimeAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(BedrockRuntimeClient client, IaProperties props) {
        return new BedrockTitanEmbeddingModel(client, props);
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter(IaProperties props) {
        return new TokenTextSplitter(props.getChunkSize(), 100, 5, 10_000, false);
    }
}