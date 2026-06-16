package com.adakadavra.dentis.ia.agent.tool;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interprets pre-procedure lab values in a dental clinical context.
 * Pure Java — reference ranges and dental safety thresholds are embedded
 * from established clinical guidelines (ADA, ACC/AHA, SEPA).
 */
@Component
public class LabValuesInterpreterTool implements AgentTool {

    @Override
    public String name() { return "lab_values_interpreter"; }

    @Override
    public String description() {
        return "Interprets pre-procedure laboratory values in a dental context and assesses procedure safety. "
             + "Evaluates coagulation (INR, PT, platelets), glycemic control (glucose, HbA1c), "
             + "renal function (creatinine), hepatic function (ALT, AST), "
             + "hematologic values (hemoglobin, WBC, neutrophils), and blood pressure. "
             + "Use before invasive dental procedures in medically complex patients.";
    }

    @Override
    public String label() { return "Interpretando valores de laboratorio..."; }

    @Override
    public boolean isExternal() { return false; }

    @Override
    public Document inputSchema() {
        return Document.mapBuilder()
                .putString("type", "object")
                .putDocument("properties", Document.mapBuilder()
                        .putDocument("values", Document.mapBuilder()
                                .putString("type", "object")
                                .putString("description",
                                        "Lab values as key-value pairs with numeric values. "
                                        + "Supported keys: inr, pt_seconds, platelets (cells/µL), "
                                        + "glucose_fasting (mg/dL), hba1c (%), creatinine (mg/dL), "
                                        + "alt (U/L), ast (U/L), hemoglobin (g/dL), "
                                        + "wbc (cells/µL), neutrophils (cells/µL), "
                                        + "systolic_bp (mmHg), diastolic_bp (mmHg)")
                                .build())
                        .putDocument("procedure", Document.mapBuilder()
                                .putString("type", "string")
                                .putString("description",
                                        "Planned dental procedure, e.g. 'simple extraction', "
                                        + "'multiple extractions', 'implant placement', 'scaling and root planing', "
                                        + "'pulpotomy', 'incision and drainage'")
                                .build())
                        .build())
                .putDocument("required", Document.listBuilder().addString("values").build())
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> input) {
        Object valuesObj = input.get("values");
        if (!(valuesObj instanceof Map<?, ?> rawValues)) {
            return "Por favor proporciona los valores de laboratorio como pares clave-valor.";
        }
        Map<String, Object> values = (Map<String, Object>) rawValues;
        String procedure = input.get("procedure") instanceof String s ? s : "procedimiento dental";

        List<String> alerts    = new ArrayList<>();
        List<String> cautions  = new ArrayList<>();
        List<String> normals   = new ArrayList<>();
        List<String> adjustments = new ArrayList<>();

        // ── Coagulation ───────────────────────────────────────────────────────
        if (values.containsKey("inr")) {
            double inr = toDouble(values.get("inr"));
            if (inr <= 2.0) {
                normals.add(String.format("INR %.1f — dentro de rango terapéutico; extracción simple segura", inr));
            } else if (inr <= 3.5) {
                cautions.add(String.format("INR %.1f — extracción simple posible con precaución; hemostasia local reforzada (sutura + gasa + ácido tranexámico). No suspender anticoagulante sin consultar al médico", inr));
            } else {
                alerts.add(String.format("⚠ INR %.1f — ALTO RIESGO hemorrágico. Posponer cirugía electiva. Consultar médico prescriptor antes de cualquier procedimiento invasivo", inr));
            }
        }
        if (values.containsKey("pt_seconds")) {
            double pt = toDouble(values.get("pt_seconds"));
            if (pt <= 15) {
                normals.add(String.format("TP %.0f seg — normal", pt));
            } else if (pt <= 20) {
                cautions.add(String.format("TP %.0f seg — levemente prolongado; valorar con INR", pt));
            } else {
                alerts.add(String.format("⚠ TP %.0f seg — significativamente prolongado. Consultar hematología antes de procedimiento invasivo", pt));
            }
        }
        if (values.containsKey("platelets")) {
            double plt = toDouble(values.get("platelets"));
            if (plt >= 100_000) {
                normals.add(String.format("Plaquetas %.0f/µL — adecuadas para cualquier procedimiento dental", plt));
            } else if (plt >= 50_000) {
                cautions.add(String.format("Plaquetas %.0f/µL — extracción simple posible con hemostasia meticulosa; evitar cirugía extensa", plt));
                adjustments.add("Considerar gasa hemostática reabsorbible y sutura en alvéolo");
            } else {
                alerts.add(String.format("⚠ Plaquetas %.0f/µL — trombocitopenia severa. Posponer procedimientos invasivos. Consultar hematología", plt));
            }
        }

        // ── Glycemic control ──────────────────────────────────────────────────
        if (values.containsKey("glucose_fasting")) {
            double glc = toDouble(values.get("glucose_fasting"));
            if (glc < 200) {
                normals.add(String.format("Glucosa en ayuno %.0f mg/dL — aceptable para procedimiento electivo", glc));
            } else if (glc < 300) {
                cautions.add(String.format("Glucosa en ayuno %.0f mg/dL — hiperglucemia moderada; proceder con precaución, mayor riesgo de infección y cicatrización lenta", glc));
                adjustments.add("Reforzar antibiótico profiláctico en caso de cirugía; citar en horario matutino tras el desayuno del paciente");
            } else {
                alerts.add(String.format("⚠ Glucosa %.0f mg/dL — hiperglucemia severa. Posponer tratamiento electivo hasta optimizar control glucémico. Solo urgencias", glc));
            }
        }
        if (values.containsKey("hba1c")) {
            double hba1c = toDouble(values.get("hba1c"));
            if (hba1c < 7.0) {
                normals.add(String.format("HbA1c %.1f%% — control glucémico óptimo; riesgo perioperatorio normal", hba1c));
            } else if (hba1c < 9.0) {
                cautions.add(String.format("HbA1c %.1f%% — control subóptimo; mayor riesgo de infección periodontal y cicatrización deficiente", hba1c));
            } else {
                alerts.add(String.format("⚠ HbA1c %.1f%% — mal control crónico. Alto riesgo de complicaciones infecciosas postoperatorias. Coordinar con médico antes de cirugía", hba1c));
            }
        }

        // ── Renal function ────────────────────────────────────────────────────
        if (values.containsKey("creatinine")) {
            double cr = toDouble(values.get("creatinine"));
            if (cr <= 1.3) {
                normals.add(String.format("Creatinina %.1f mg/dL — función renal normal", cr));
            } else if (cr <= 2.5) {
                cautions.add(String.format("Creatinina %.1f mg/dL — ERC moderada; ajustar dosis de amoxicilina (c/12 h) y evitar AINEs", cr));
                adjustments.add("Anestésicos locales: sin ajuste de dosis (metabolismo hepático)");
            } else {
                alerts.add(String.format("⚠ Creatinina %.1f mg/dL — ERC severa. Ajuste significativo de dosis de antibióticos. Evitar AINEs. Consultar nefrólogo antes de cirugía mayor", cr));
            }
        }

        // ── Hepatic function ──────────────────────────────────────────────────
        if (values.containsKey("alt") || values.containsKey("ast")) {
            double alt = values.containsKey("alt") ? toDouble(values.get("alt")) : 0;
            double ast = values.containsKey("ast") ? toDouble(values.get("ast")) : 0;
            double max = Math.max(alt, ast);
            double uln = 40; // upper limit of normal
            double ratio = max / uln;

            if (ratio <= 1.0) {
                normals.add(String.format("ALT/AST dentro de límites normales (ALT %.0f, AST %.0f U/L)", alt, ast));
            } else if (ratio <= 3.0) {
                cautions.add(String.format("ALT/AST elevadas (%.1f× límite normal) — precaución con lidocaína (metabolismo hepático); reducir dosis máxima 25%%", ratio));
            } else {
                alerts.add(String.format("⚠ ALT/AST muy elevadas (%.1f× límite normal) — hepatopatía significativa. Reducir anestésico local al 50%%. Metronidazol contraindicado. Consultar hepatología", ratio));
            }
        }

        // ── Hematologic ───────────────────────────────────────────────────────
        if (values.containsKey("hemoglobin")) {
            double hb = toDouble(values.get("hemoglobin"));
            if (hb >= 12.0) {
                normals.add(String.format("Hemoglobina %.1f g/dL — normal", hb));
            } else if (hb >= 10.0) {
                cautions.add(String.format("Hemoglobina %.1f g/dL — anemia leve-moderada; limitar sangrado intraoperatorio, evitar técnicas que causen pérdida hemática significativa", hb));
            } else {
                alerts.add(String.format("⚠ Hemoglobina %.1f g/dL — anemia severa. Posponer cirugía electiva. Investigar causa. Urgencias solo con cobertura", hb));
            }
        }
        if (values.containsKey("wbc")) {
            double wbc = toDouble(values.get("wbc"));
            if (wbc >= 4_000) {
                normals.add(String.format("Leucocitos %.0f/µL — normal", wbc));
            } else if (wbc >= 2_000) {
                cautions.add(String.format("Leucocitos %.0f/µL — leucopenia moderada; considerar profilaxis antibiótica para procedimientos invasivos", wbc));
                adjustments.add("Profilaxis antibiótica: amoxicilina 2 g vo 1 hora antes del procedimiento");
            } else {
                alerts.add(String.format("⚠ Leucocitos %.0f/µL — leucopenia severa. Alto riesgo de infección. Posponer cirugía electiva. Consultar hematología/oncología", wbc));
            }
        }
        if (values.containsKey("neutrophils")) {
            double anc = toDouble(values.get("neutrophils"));
            if (anc >= 1_500) {
                normals.add(String.format("Neutrófilos (ANC) %.0f/µL — normal", anc));
            } else if (anc >= 1_000) {
                cautions.add(String.format("ANC %.0f/µL — neutropenia leve; profilaxis antibiótica recomendada", anc));
            } else if (anc >= 500) {
                alerts.add(String.format("⚠ ANC %.0f/µL — neutropenia moderada. Profilaxis antibiótica obligatoria. Evitar procedimientos electivos", anc));
            } else {
                alerts.add(String.format("⚠ ANC %.0f/µL — neutropenia severa. CONTRAINDICADO procedimiento electivo. Solo urgencias vitales con cobertura antibiótica intensiva y supervisión hospitalaria", anc));
            }
        }

        // ── Blood pressure ────────────────────────────────────────────────────
        if (values.containsKey("systolic_bp") || values.containsKey("diastolic_bp")) {
            double sbp = values.containsKey("systolic_bp")  ? toDouble(values.get("systolic_bp"))  : 0;
            double dbp = values.containsKey("diastolic_bp") ? toDouble(values.get("diastolic_bp")) : 0;
            if (sbp > 0 && dbp > 0) {
                if (sbp < 160 && dbp < 100) {
                    normals.add(String.format("PA %.0f/%.0f mmHg — aceptable para tratamiento dental", sbp, dbp));
                } else if (sbp < 180 && dbp < 110) {
                    cautions.add(String.format("PA %.0f/%.0f mmHg — HTA grado 2; limitar epinefrina a máx 2 cartuchos de lidocaína 1:100.000; monitorizar durante el procedimiento", sbp, dbp));
                } else {
                    alerts.add(String.format("⚠ PA %.0f/%.0f mmHg — HTA severa (crisis hipertensiva). Posponer tratamiento electivo. No usar vasoconstrictores. Remitir a urgencias médicas si sintomático", sbp, dbp));
                }
            }
        }

        if (normals.isEmpty() && cautions.isEmpty() && alerts.isEmpty()) {
            return "No se reconocieron valores de laboratorio en la entrada. Parámetros aceptados: inr, pt_seconds, platelets, glucose_fasting, hba1c, creatinine, alt, ast, hemoglobin, wbc, neutrophils, systolic_bp, diastolic_bp.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Interpretación de laboratorio — ").append(procedure).append("\n\n");

        if (!alerts.isEmpty()) {
            sb.append("ALERTAS (proceder con extrema precaución o posponer):\n");
            alerts.forEach(a -> sb.append("  ").append(a).append("\n"));
            sb.append("\n");
        }
        if (!cautions.isEmpty()) {
            sb.append("PRECAUCIONES:\n");
            cautions.forEach(c -> sb.append("  ").append(c).append("\n"));
            sb.append("\n");
        }
        if (!normals.isEmpty()) {
            sb.append("VALORES NORMALES:\n");
            normals.forEach(n -> sb.append("  ").append(n).append("\n"));
            sb.append("\n");
        }
        if (!adjustments.isEmpty()) {
            sb.append("AJUSTES RECOMENDADOS:\n");
            adjustments.forEach(a -> sb.append("  • ").append(a).append("\n"));
            sb.append("\n");
        }

        sb.append("Fuente: Umbrales basados en guías ADA (2024), ACC/AHA, SEPA y Farmacopea Española.");
        return sb.toString().trim();
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }
}
