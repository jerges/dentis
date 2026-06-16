package com.adakadavra.dentis.ia.agent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DosageCalculatorToolTest {

    private DosageCalculatorTool tool;

    @BeforeEach
    void setUp() {
        tool = new DosageCalculatorTool();
    }

    @Test
    @DisplayName("shouldBeNamedDosageCalculator")
    void shouldBeNamedDosageCalculator() {
        assertThat(tool.name()).isEqualTo("dosage_calculator");
    }

    @Test
    @DisplayName("shouldBeHavingNonEmptyDescription")
    void shouldBeHavingNonEmptyDescription() {
        assertThat(tool.description()).isNotBlank();
    }

    @Test
    @DisplayName("shouldBeHavingNonEmptyLabel")
    void shouldBeHavingNonEmptyLabel() {
        assertThat(tool.label()).isNotBlank();
    }

    @Test
    @DisplayName("shouldBeHavingValidInputSchema")
    void shouldBeHavingValidInputSchema() {
        var schema = tool.inputSchema();
        assertThat(schema).isNotNull();
        assertThat(schema.asMap()).containsKey("properties");
    }

    // ── Anesthetics ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Anesthetic calculations")
    class AnestheticTests {

        @Test
        @DisplayName("shouldBeCalculatingLidocaineMaxDoseByWeight")
        void shouldBeCalculatingLidocaineMaxDoseByWeight() {
            String result = tool.execute(Map.of("drug", "lidocaina 2% epinefrina", "weight_kg", 70.0));

            // 7 mg/kg × 70 kg = 490 mg (below absolute max 500 mg)
            assertThat(result).contains("490").contains("mg");
        }

        @Test
        @DisplayName("shouldBeCapByAbsoluteMaxWhenWeightIsHigh")
        void shouldBeCapByAbsoluteMaxWhenWeightIsHigh() {
            // 100 kg × 7 mg/kg = 700 mg > absolute max 500 mg → capped at 500
            String result = tool.execute(Map.of("drug", "lidocaina 2% epinefrina", "weight_kg", 100.0));

            assertThat(result).contains("500");
        }

        @Test
        @DisplayName("shouldBeIncludingCartridgeCount")
        void shouldBeIncludingCartridgeCount() {
            String result = tool.execute(Map.of("drug", "lidocaina 2% epinefrina", "weight_kg", 70.0));

            assertThat(result.toLowerCase()).contains("cartuchos");
        }

        @Test
        @DisplayName("shouldBeReducingDoseForPediatricPatient")
        void shouldBeReducingDoseForPediatricPatient() {
            // Pediatric max = 4.5 mg/kg × 40 kg = 180 mg  vs  adult 7 mg/kg × 40 kg = 280 mg
            String pediatric = tool.execute(Map.of("drug", "lidocaina 2% epinefrina", "weight_kg", 40.0, "age_years", 8));

            assertThat(pediatric).contains("180");
            assertThat(pediatric.toLowerCase()).contains("pedi");
        }

        @Test
        @DisplayName("shouldBeWarnAboutElderlyAgeReduction")
        void shouldBeWarnAboutElderlyAgeReduction() {
            String result = tool.execute(Map.of("drug", "articaina 4%", "weight_kg", 60.0, "age_years", 70));

            assertThat(result).contains("65");
        }

        @Test
        @DisplayName("shouldBeHalvingDoseForHepaticFailure")
        void shouldBeHalvingDoseForHepaticFailure() {
            // Normal: 7 mg/kg × 70 kg = 490 mg → hepatic: 245 mg
            String hepatic = tool.execute(Map.of("drug", "lidocaina 2% epinefrina", "weight_kg", 70.0,
                    "comorbidities", List.of("hepatic_failure")));

            assertThat(hepatic).contains("245");
            assertThat(hepatic.toLowerCase()).satisfiesAnyOf(
                    r -> assertThat(r).contains("hep"),
                    r -> assertThat(r).contains("50%")
            );
        }

        @Test
        @DisplayName("shouldBeWarnAboutEpinephrineInHypertension")
        void shouldBeWarnAboutEpinephrineInHypertension() {
            String result = tool.execute(Map.of("drug", "lidocaina 2% epinefrina", "weight_kg", 70.0,
                    "comorbidities", List.of("hypertension"))).toLowerCase();

            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("hipertens"),
                    r -> assertThat(r).contains("epinefrina"),
                    r -> assertThat(r).contains("vasoconstrict")
            );
        }

        @Test
        @DisplayName("shouldBeCalculatingArticaineWith72MgPerCartridge")
        void shouldBeCalculatingArticaineWith72MgPerCartridge() {
            // 4% × 10 mg/mL per % × 1.8 mL = 72 mg per cartridge
            String result = tool.execute(Map.of("drug", "articaine 4%", "weight_kg", 70.0));

            assertThat(result).contains("72");
        }
    }

    // ── Antibiotics ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Antibiotic calculations")
    class AntibioticTests {

        @Test
        @DisplayName("shouldBeReturningAmoxicillinAdultDose")
        void shouldBeReturningAmoxicillinAdultDose() {
            String result = tool.execute(Map.of("drug", "amoxicilina", "weight_kg", 70.0, "age_years", 35));

            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("500"),
                    r -> assertThat(r).contains("1 g")
            );
        }

        @Test
        @DisplayName("shouldBeCalculatingPediatricAntibioticDoseByWeight")
        void shouldBeCalculatingPediatricAntibioticDoseByWeight() {
            // 40 mg/kg/day × 20 kg = 800 mg/day ÷ 3 doses = ~267 mg per dose
            String result = tool.execute(Map.of("drug", "amoxicilina", "weight_kg", 20.0, "age_years", 6));

            assertThat(result.toLowerCase()).contains("pedi");
            assertThat(result).contains("267");
        }

        @Test
        @DisplayName("shouldBeWarnAboutMetronidazoleInPregnancy")
        void shouldBeWarnAboutMetronidazoleInPregnancy() {
            String result = tool.execute(Map.of("drug", "metronidazol", "weight_kg", 60.0,
                    "comorbidities", List.of("pregnancy"))).toLowerCase();

            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("trimestre"),
                    r -> assertThat(r).contains("embaraz")
            );
        }

        @Test
        @DisplayName("shouldBeWarnContraindicatedMetronidazoleInHepaticFailure")
        void shouldBeWarnContraindicatedMetronidazoleInHepaticFailure() {
            String result = tool.execute(Map.of("drug", "metronidazole", "weight_kg", 70.0,
                    "comorbidities", List.of("hepatic_failure"))).toLowerCase();

            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("contraindicad"),
                    r -> assertThat(r).contains("hepatopatía")
            );
        }

        @Test
        @DisplayName("shouldBeWarnRenalAdjustmentForAmoxicillin")
        void shouldBeWarnRenalAdjustmentForAmoxicillin() {
            String result = tool.execute(Map.of("drug", "amoxicillin", "weight_kg", 70.0,
                    "comorbidities", List.of("renal_failure"))).toLowerCase();

            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r).contains("renal"),
                    r -> assertThat(r).contains("tfg")
            );
        }

        @Test
        @DisplayName("shouldBeReturningAmoxicillinClavulanateInfo")
        void shouldBeReturningAmoxicillinClavulanateInfo() {
            String result = tool.execute(Map.of("drug", "amoxicilina clavulanato", "weight_kg", 70.0));

            assertThat(result).satisfiesAnyOf(
                    r -> assertThat(r.toLowerCase()).contains("clavul"),
                    r -> assertThat(r).contains("875")
            );
        }
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldBeReturningHelpfulMessageForUnknownDrug")
    void shouldBeReturningHelpfulMessageForUnknownDrug() {
        String result = tool.execute(Map.of("drug", "ibuprofeno", "weight_kg", 70.0)).toLowerCase();

        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("no reconocido"),
                r -> assertThat(r).contains("disponibles")
        );
    }

    @Test
    @DisplayName("shouldBeHandlingMissingDrugGracefully")
    void shouldBeHandlingMissingDrugGracefully() {
        String result = tool.execute(Map.of("weight_kg", 70.0)).toLowerCase();

        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("especifica"),
                r -> assertThat(r).contains("medicamento")
        );
    }
}
