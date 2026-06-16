package com.adakadavra.dentis.ia.agent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LabValuesInterpreterToolTest {

    private LabValuesInterpreterTool tool;

    @BeforeEach
    void setUp() {
        tool = new LabValuesInterpreterTool();
    }

    @Test
    @DisplayName("shouldBeNamedLabValuesInterpreter")
    void shouldBeNamedLabValuesInterpreter() {
        assertThat(tool.name()).isEqualTo("lab_values_interpreter");
    }

    @Test
    @DisplayName("shouldBeHavingValidSchema")
    void shouldBeHavingValidSchema() {
        assertThat(tool.inputSchema().asMap()).containsKey("properties");
    }

    @Test
    @DisplayName("shouldBeReturningGracefulMessageWhenNoValues")
    void shouldBeReturningGracefulMessageWhenNoValues() {
        String result = tool.execute(Map.of()).toLowerCase();
        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("proporciona"),
                r -> assertThat(r).contains("valores")
        );
    }

    // ── INR / Coagulation ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("INR assessment")
    class InrTests {

        @Test
        @DisplayName("shouldBeNormalWhenInrIsBelowTwo")
        void shouldBeNormalWhenInrIsBelowTwo() {
            String result = tool.execute(Map.of("values", Map.of("inr", 1.8))).toLowerCase();
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("normal"),
                    r -> assertThat(r).contains("segura"),
                    r -> assertThat(r).contains("rango")
            );
        }

        @Test
        @DisplayName("shouldBeCautionWhenInrIsBetweenTwoAndThreePointFive")
        void shouldBeCautionWhenInrIsBetweenTwoAndThreePointFive() {
            String result = tool.execute(Map.of("values", Map.of("inr", 2.8)));
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r.toLowerCase()).contains("precaución"),
                    r -> assertThat(r.toLowerCase()).contains("caución"),
                    r -> assertThat(r.toLowerCase()).contains("hemostasia")
            );
        }

        @Test
        @DisplayName("shouldBeAlertWhenInrExceedsThreePointFive")
        void shouldBeAlertWhenInrExceedsThreePointFive() {
            String result = tool.execute(Map.of("values", Map.of("inr", 4.2)));
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r.toLowerCase()).contains("alto riesgo"),
                    r -> assertThat(r).contains("⚠"),
                    r -> assertThat(r.toLowerCase()).contains("posponer")
            );
        }
    }

    // ── Platelets ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Platelet assessment")
    class PlateletTests {

        @Test
        @DisplayName("shouldBeNormalWhenPlateletsAbove100k")
        void shouldBeNormalWhenPlateletsAbove100k() {
            String result = tool.execute(Map.of("values", Map.of("platelets", 180_000))).toLowerCase();
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("adecuadas"),
                    r -> assertThat(r).contains("normal")
            );
        }

        @Test
        @DisplayName("shouldBeCautionWhenPlateletsBetween50kAnd100k")
        void shouldBeCautionWhenPlateletsBetween50kAnd100k() {
            String result = tool.execute(Map.of("values", Map.of("platelets", 70_000))).toLowerCase();
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("hemostasia"),
                    r -> assertThat(r).contains("precauci")
            );
        }

        @Test
        @DisplayName("shouldBeAlertWhenPlateletsBelow50k")
        void shouldBeAlertWhenPlateletsBelow50k() {
            String result = tool.execute(Map.of("values", Map.of("platelets", 30_000)));
            assertThat(result).contains("⚠");
        }
    }

    // ── Glycemic ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Glycemic assessment")
    class GlycemicTests {

        @Test
        @DisplayName("shouldBeNormalWhenFastingGlucoseBelow200")
        void shouldBeNormalWhenFastingGlucoseBelow200() {
            String result = tool.execute(Map.of("values", Map.of("glucose_fasting", 120))).toLowerCase();
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("aceptable"),
                    r -> assertThat(r).contains("normal")
            );
        }

        @Test
        @DisplayName("shouldBeAlertWhenFastingGlucoseAbove300")
        void shouldBeAlertWhenFastingGlucoseAbove300() {
            String result = tool.execute(Map.of("values", Map.of("glucose_fasting", 350)));
            assertThat(result).contains("⚠");
        }

        @Test
        @DisplayName("shouldBeAlertWhenHba1cAboveNine")
        void shouldBeAlertWhenHba1cAboveNine() {
            String result = tool.execute(Map.of("values", Map.of("hba1c", 10.5))).toLowerCase();
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("mal control"),
                    r -> assertThat(r).contains("⚠")
            );
        }
    }

    // ── Hepatic ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Hepatic assessment")
    class HepaticTests {

        @Test
        @DisplayName("shouldBeAlertWhenAltIsMoreThanThreeTimesNormal")
        void shouldBeAlertWhenAltIsMoreThanThreeTimesNormal() {
            // ULN = 40, 3× = 120; using 200 → ratio 5
            String result = tool.execute(Map.of("values", Map.of("alt", 200, "ast", 180)));
            assertThat(result).contains("⚠");
        }

        @Test
        @DisplayName("shouldMentionMetronidazoleContraindicationWhenLiverSeverelyAbnormal")
        void shouldMentionMetronidazoleContraindicationWhenLiverSeverelyAbnormal() {
            String result = tool.execute(Map.of("values", Map.of("alt", 200))).toLowerCase();
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("metronidazol"),
                    r -> assertThat(r).contains("hepatopatía"),
                    r -> assertThat(r).contains("hepátic")
            );
        }
    }

    // ── Blood pressure ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Blood pressure assessment")
    class BloodPressureTests {

        @Test
        @DisplayName("shouldBeNormalWhenBloodPressureIsControlled")
        void shouldBeNormalWhenBloodPressureIsControlled() {
            String result = tool.execute(Map.of("values", Map.of("systolic_bp", 130, "diastolic_bp", 80))).toLowerCase();
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("aceptable"),
                    r -> assertThat(r).contains("normal")
            );
        }

        @Test
        @DisplayName("shouldBeAlertWhenBloodPressureIsSeverelyElevated")
        void shouldBeAlertWhenBloodPressureIsSeverelyElevated() {
            String result = tool.execute(Map.of("values", Map.of("systolic_bp", 195, "diastolic_bp", 115)));
            assertThat(result).contains("⚠");
        }

        @Test
        @DisplayName("shouldWarnAboutEpinephrineWhenBloodPressureIsModerate")
        void shouldWarnAboutEpinephrineWhenBloodPressureIsModerate() {
            String result = tool.execute(Map.of("values", Map.of("systolic_bp", 165, "diastolic_bp", 100))).toLowerCase();
            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("epinefrina"),
                    r -> assertThat(r).contains("vasoconstrict"),
                    r -> assertThat(r).contains("precauci")
            );
        }
    }

    // ── Multiple values ───────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldBeHandlingMultipleValuesInSingleRequest")
    void shouldBeHandlingMultipleValuesInSingleRequest() {
        String result = tool.execute(Map.of(
                "values", Map.of("inr", 1.5, "glucose_fasting", 180, "platelets", 220_000),
                "procedure", "simple extraction"
        ));

        assertThat(result).isNotBlank();
        assertThat(result.toLowerCase()).satisfiesAnyOf(
                r -> assertThat(r).contains("extracción"),
                r -> assertThat(r).contains("extraction"),
                r -> assertThat(r).contains("procedure")
        );
    }
}
