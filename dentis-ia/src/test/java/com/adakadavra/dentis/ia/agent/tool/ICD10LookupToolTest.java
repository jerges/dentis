package com.adakadavra.dentis.ia.agent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ICD10LookupToolTest {

    private ICD10LookupTool tool;

    @BeforeEach
    void setUp() {
        tool = new ICD10LookupTool();
    }

    @Test
    @DisplayName("shouldBeNamedIcd10Lookup")
    void shouldBeNamedIcd10Lookup() {
        assertThat(tool.name()).isEqualTo("icd10_lookup");
    }

    @Test
    @DisplayName("shouldBeHavingNonEmptyDescriptionAndLabel")
    void shouldBeHavingNonEmptyDescriptionAndLabel() {
        assertThat(tool.description()).isNotBlank();
        assertThat(tool.label()).isNotBlank();
    }

    @Test
    @DisplayName("shouldBeHavingValidInputSchema")
    void shouldBeHavingValidInputSchema() {
        assertThat(tool.inputSchema().asMap()).containsKey("properties");
    }

    @Test
    @DisplayName("shouldBeReturningGracefulMessageWhenQueryIsMissing")
    void shouldBeReturningGracefulMessageWhenQueryIsMissing() {
        String result = tool.execute(Map.of()).toLowerCase();
        assertThat(result).contains("especifica");
    }
}
