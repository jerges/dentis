package com.adakadavra.dentis.ia.agent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FDADrugInfoToolTest {

    private FDADrugInfoTool tool;

    @BeforeEach
    void setUp() {
        tool = new FDADrugInfoTool();
    }

    @Test
    @DisplayName("shouldBeNamedFdaDrugInfo")
    void shouldBeNamedFdaDrugInfo() {
        assertThat(tool.name()).isEqualTo("fda_drug_info");
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
    @DisplayName("shouldBeReturningGracefulMessageWhenDrugIsMissing")
    void shouldBeReturningGracefulMessageWhenDrugIsMissing() {
        String result = tool.execute(Map.of()).toLowerCase();
        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("especifica"),
                r -> assertThat(r).contains("nombre")
        );
    }
}
