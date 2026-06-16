package com.adakadavra.dentis.ia.agent.tool;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java dosage calculator using embedded clinical data (no external API).
 * Covers local anesthetics and dental antibiotics with weight/age/comorbidity adjustments.
 */
@Component
public class DosageCalculatorTool implements AgentTool {

    private static final double CARTRIDGE_ML = 1.8;

    private record AnestheticProfile(
            String displayName,
            double concentrationPct,
            double maxMgPerKg,
            double absoluteMaxMg,
            boolean hasEpinephrine,
            String clinicalNote
    ) {
        double mgPerCartridge() { return concentrationPct * 10 * CARTRIDGE_ML; }
    }

    private record AntibioticProfile(
            String displayName,
            String adultDose,
            String duration,
            double pedMgPerKgDay,
            int pedDosesPerDay,
            double pedMaxDailyMg,
            String clinicalNote
    ) {}

    private static final Map<String, AnestheticProfile> ANESTHETICS = new LinkedHashMap<>();
    private static final Map<String, AntibioticProfile> ANTIBIOTICS = new LinkedHashMap<>();

    static {
        ANESTHETICS.put("lidocaina 2% epinefrina",
                new AnestheticProfile("Lidocaína 2% c/epinefrina 1:100.000", 2.0, 7.0, 500, true,
                        "Precaución en hipertensión no controlada y cardiopatías graves. Duración pulpar ~60 min."));
        ANESTHETICS.put("lidocaine 2% epinephrine",
                new AnestheticProfile("Lidocaína 2% c/epinefrina 1:100.000", 2.0, 7.0, 500, true,
                        "Precaución en hipertensión no controlada y cardiopatías graves. Duración pulpar ~60 min."));
        ANESTHETICS.put("lidocaina 2%",
                new AnestheticProfile("Lidocaína 2% sin vasoconstrictor", 2.0, 4.5, 300, false,
                        "Indicada en contraindicación a vasoconstrictores. Duración pulpar ~15 min, menor efectividad."));
        ANESTHETICS.put("lidocaine 2%",
                new AnestheticProfile("Lidocaína 2% sin vasoconstrictor", 2.0, 4.5, 300, false,
                        "Indicada en contraindicación a vasoconstrictores. Duración pulpar ~15 min, menor efectividad."));
        ANESTHETICS.put("articaina 4%",
                new AnestheticProfile("Articaína 4% c/epinefrina 1:100.000", 4.0, 7.0, 500, true,
                        "Excelente difusión ósea. Preferida en exodoncias y cirugía. Precaución en déficit de G6PD."));
        ANESTHETICS.put("articaine 4%",
                new AnestheticProfile("Articaína 4% c/epinefrina 1:100.000", 4.0, 7.0, 500, true,
                        "Excelente difusión ósea. Preferida en exodoncias y cirugía. Precaución en déficit de G6PD."));
        ANESTHETICS.put("mepivacaina 3%",
                new AnestheticProfile("Mepivacaína 3% sin vasoconstrictor", 3.0, 6.6, 400, false,
                        "Primera elección en contraindicación a vasoconstrictores. Duración pulpar ~20-40 min."));
        ANESTHETICS.put("mepivacaine 3%",
                new AnestheticProfile("Mepivacaína 3% sin vasoconstrictor", 3.0, 6.6, 400, false,
                        "Primera elección en contraindicación a vasoconstrictores. Duración pulpar ~20-40 min."));
        ANESTHETICS.put("bupivacaina 0.5%",
                new AnestheticProfile("Bupivacaína 0.5% c/epinefrina 1:200.000", 0.5, 1.3, 90, true,
                        "Larga duración (6-12 h). Útil para control postoperatorio. Cardiotóxica en sobredosis."));
        ANESTHETICS.put("bupivacaine 0.5%",
                new AnestheticProfile("Bupivacaína 0.5% c/epinefrina 1:200.000", 0.5, 1.3, 90, true,
                        "Larga duración (6-12 h). Útil para control postoperatorio. Cardiotóxica en sobredosis."));

        ANTIBIOTICS.put("amoxicilina",
                new AntibioticProfile("Amoxicilina", "500 mg c/8 h ó 1 g c/12 h (vía oral)", "5-7 días",
                        40, 3, 3000, "Verificar alergia a penicilinas. Reducir intervalo en insuficiencia renal."));
        ANTIBIOTICS.put("amoxicillin",
                new AntibioticProfile("Amoxicilina", "500 mg c/8 h ó 1 g c/12 h (vía oral)", "5-7 días",
                        40, 3, 3000, "Verificar alergia a penicilinas. Reducir intervalo en insuficiencia renal."));
        ANTIBIOTICS.put("amoxicilina clavulanato",
                new AntibioticProfile("Amoxicilina/Ácido clavúnico 875/125 mg", "875/125 mg c/12 h (vía oral)", "7 días",
                        40, 2, 3000, "Para infecciones resistentes o periodontitis agresiva. Mayor riesgo de diarrea."));
        ANTIBIOTICS.put("amoxicillin clavulanate",
                new AntibioticProfile("Amoxicilina/Ácido clavúnico 875/125 mg", "875/125 mg c/12 h (vía oral)", "7 días",
                        40, 2, 3000, "Para infecciones resistentes o periodontitis agresiva. Mayor riesgo de diarrea."));
        ANTIBIOTICS.put("metronidazol",
                new AntibioticProfile("Metronidazol", "500 mg c/8 h (vía oral)", "7 días",
                        20, 3, 1500, "Contraindicado en hepatopatía grave y embarazo 1er trimestre. Efecto antábus con alcohol."));
        ANTIBIOTICS.put("metronidazole",
                new AntibioticProfile("Metronidazol", "500 mg c/8 h (vía oral)", "7 días",
                        20, 3, 1500, "Contraindicado en hepatopatía grave y embarazo 1er trimestre. Efecto antábus con alcohol."));
        ANTIBIOTICS.put("clindamicina",
                new AntibioticProfile("Clindamicina", "300 mg c/8 h (vía oral)", "7 días",
                        8, 3, 1800, "Alternativa en alérgicos a penicilinas. Riesgo de colitis por C. difficile."));
        ANTIBIOTICS.put("clindamycin",
                new AntibioticProfile("Clindamicina", "300 mg c/8 h (vía oral)", "7 días",
                        8, 3, 1800, "Alternativa en alérgicos a penicilinas. Riesgo de colitis por C. difficile."));
        ANTIBIOTICS.put("azitromicina",
                new AntibioticProfile("Azitromicina", "500 mg/día (día 1), 250 mg/día (días 2-5)", "5 días",
                        10, 1, 500, "Alternativa en alérgicos a penicilinas. Interacción con anticoagulantes orales."));
        ANTIBIOTICS.put("azithromycin",
                new AntibioticProfile("Azitromicina", "500 mg/día (día 1), 250 mg/día (días 2-5)", "5 días",
                        10, 1, 500, "Alternativa en alérgicos a penicilinas. Interacción con anticoagulantes orales."));
    }

    @Override
    public String name() { return "dosage_calculator"; }

    @Override
    public String description() {
        return "Calculates safe dental drug doses based on patient weight, age, and comorbidities. "
             + "Covers local anesthetics (lidocaine, articaine, mepivacaine, bupivacaine) and dental antibiotics "
             + "(amoxicillin, amoxicillin/clavulanate, metronidazole, clindamycin, azithromycin). "
             + "Returns max dose in mg, number of cartridges for anesthetics, and clinical warnings.";
    }

    @Override
    public String label() { return "Calculando dosis segura..."; }

    @Override
    public boolean isExternal() { return false; }

    @Override
    public Document inputSchema() {
        return Document.mapBuilder()
                .putString("type", "object")
                .putDocument("properties", Document.mapBuilder()
                        .putDocument("drug", Document.mapBuilder()
                                .putString("type", "string")
                                .putString("description",
                                        "Drug name. For anesthetics use: 'lidocaina 2% epinefrina', 'lidocaina 2%', "
                                        + "'articaina 4%', 'mepivacaina 3%', 'bupivacaina 0.5%'. "
                                        + "For antibiotics use: 'amoxicilina', 'amoxicilina clavulanato', "
                                        + "'metronidazol', 'clindamicina', 'azitromicina'.")
                                .build())
                        .putDocument("weight_kg", Document.mapBuilder()
                                .putString("type", "number")
                                .putString("description", "Patient weight in kilograms")
                                .build())
                        .putDocument("age_years", Document.mapBuilder()
                                .putString("type", "number")
                                .putString("description", "Patient age in years")
                                .build())
                        .putDocument("comorbidities", Document.mapBuilder()
                                .putString("type", "array")
                                .putDocument("items", Document.mapBuilder().putString("type", "string").build())
                                .putString("description",
                                        "Relevant conditions from: 'renal_failure', 'hepatic_failure', "
                                        + "'pregnancy', 'hypertension', 'diabetes'")
                                .build())
                        .build())
                .putDocument("required", Document.listBuilder()
                        .addString("drug")
                        .addString("weight_kg")
                        .build())
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(Map<String, Object> input) {
        String drug = normalize((String) input.get("drug"));
        if (drug == null || drug.isBlank()) {
            return "Por favor especifica el medicamento.";
        }
        double weightKg;
        try {
            weightKg = ((Number) input.get("weight_kg")).doubleValue();
        } catch (Exception e) {
            return "Por favor especifica el peso del paciente en kg.";
        }

        int ageYears = input.get("age_years") instanceof Number n ? n.intValue() : -1;
        boolean isPediatric = ageYears >= 0 && ageYears < 12;

        List<String> comorbidities = input.get("comorbidities") instanceof List<?> l
                ? (List<String>) l : List.of();
        boolean renalFailure   = comorbidities.stream().anyMatch(c -> c.contains("renal"));
        boolean hepaticFailure = comorbidities.stream().anyMatch(c -> c.contains("hepatic") || c.contains("hepat"));
        boolean pregnancy      = comorbidities.stream().anyMatch(c -> c.contains("preg") || c.contains("embaraz"));
        boolean hypertension   = comorbidities.stream().anyMatch(c -> c.contains("hypert") || c.contains("hiperten"));

        AnestheticProfile anesthetic = findAnesthetic(drug);
        if (anesthetic != null) {
            return anestheticResult(anesthetic, weightKg, ageYears, isPediatric, renalFailure, hepaticFailure, pregnancy, hypertension);
        }

        AntibioticProfile antibiotic = ANTIBIOTICS.get(drug);
        if (antibiotic != null) {
            return antibioticResult(antibiotic, weightKg, ageYears, isPediatric, renalFailure, hepaticFailure, pregnancy);
        }

        return "Medicamento no reconocido: '" + input.get("drug") + "'.\n"
             + "Anestésicos disponibles: lidocaina 2% epinefrina, lidocaina 2%, articaina 4%, mepivacaina 3%, bupivacaina 0.5%\n"
             + "Antibióticos disponibles: amoxicilina, amoxicilina clavulanato, metronidazol, clindamicina, azitromicina";
    }

    private String anestheticResult(AnestheticProfile p, double weightKg, int ageYears,
                                    boolean isPediatric, boolean renal, boolean hepatic,
                                    boolean pregnancy, boolean hypertension) {
        double maxMgPerKg  = p.maxMgPerKg();
        double absoluteMax = p.absoluteMaxMg();
        List<String> adjustments = new ArrayList<>();

        if (isPediatric) {
            maxMgPerKg = Math.min(maxMgPerKg, 4.5);
            adjustments.add("Paciente pediátrico: límite ajustado a 4.5 mg/kg");
        }
        if (ageYears >= 65) {
            maxMgPerKg  *= 0.75;
            absoluteMax *= 0.75;
            adjustments.add("Adulto mayor (≥65 años): reducción del 25%");
        }
        if (hepatic) {
            maxMgPerKg  *= 0.5;
            absoluteMax *= 0.5;
            adjustments.add("Insuficiencia hepática: dosis máxima reducida al 50% (metabolismo hepático)");
        }
        if (renal) {
            adjustments.add("Insuficiencia renal: anestésicos locales se metabolizan principalmente en hígado; monitorizar signos de toxicidad");
        }
        if (hypertension && p.hasEpinephrine()) {
            adjustments.add("⚠ Hipertensión: limitar a 2 cartuchos con epinefrina; valorar mepivacaína 3% sin vasoconstrictor");
        }
        if (pregnancy && p.hasEpinephrine()) {
            adjustments.add("Embarazo: epinefrina en dosis bajas es aceptable; aspirar siempre antes de inyectar; evitar bloqueos bilaterales");
        }

        double maxByWeight = maxMgPerKg * weightKg;
        double finalMax    = Math.min(maxByWeight, absoluteMax);
        double cartridges  = finalMax / p.mgPerCartridge();

        StringBuilder sb = new StringBuilder();
        sb.append("Anestésico: ").append(p.displayName()).append("\n");
        sb.append("Paciente: ").append(weightKg).append(" kg");
        if (ageYears >= 0) sb.append(", ").append(ageYears).append(" años");
        sb.append("\n\n");
        sb.append("DOSIS MÁXIMA SEGURA: ").append(String.format("%.0f", finalMax)).append(" mg\n");
        sb.append("  • Por peso: ").append(String.format("%.0f", maxByWeight))
          .append(" mg (").append(String.format("%.1f", maxMgPerKg)).append(" mg/kg × ").append(weightKg).append(" kg)\n");
        sb.append("  • Límite absoluto del fármaco: ").append(String.format("%.0f", absoluteMax)).append(" mg\n");
        sb.append("  • Máximo cartuchos: ≤ ").append(String.format("%.1f", cartridges))
          .append(" (1 cartucho = ").append(String.format("%.0f", p.mgPerCartridge())).append(" mg)\n");

        if (!adjustments.isEmpty()) {
            sb.append("\nAjustes aplicados:\n");
            adjustments.forEach(a -> sb.append("  • ").append(a).append("\n"));
        }
        sb.append("\nNota clínica: ").append(p.clinicalNote());
        return sb.toString();
    }

    private String antibioticResult(AntibioticProfile p, double weightKg, int ageYears,
                                    boolean isPediatric, boolean renal, boolean hepatic, boolean pregnancy) {
        List<String> warnings = new ArrayList<>();
        String dose;

        if (isPediatric) {
            double dailyMg      = Math.min(p.pedMgPerKgDay() * weightKg, p.pedMaxDailyMg());
            double dosePerAdmin = dailyMg / p.pedDosesPerDay();
            int intervalH       = 24 / p.pedDosesPerDay();
            dose = String.format("%.0f mg c/%d h (%.0f mg/día) — dosis pediátrica",
                    dosePerAdmin, intervalH, dailyMg);
            warnings.add(String.format("Calculada a %.0f mg/kg/día; máximo %.0f mg/día",
                    p.pedMgPerKgDay(), p.pedMaxDailyMg()));
        } else {
            dose = p.adultDose();
        }

        if (renal) {
            if (p.displayName().contains("Amoxicilina")) {
                warnings.add("⚠ Insuficiencia renal: TFG 10-30 ml/min → c/12 h; TFG <10 ml/min → c/24 h");
            } else if (p.displayName().contains("Metronidazol")) {
                warnings.add("IR grave: reducir dosis al 50%; IR leve-moderada sin ajuste");
            } else {
                warnings.add("Insuficiencia renal: consultar ficha técnica para ajuste de dosis");
            }
        }
        if (hepatic) {
            if (p.displayName().contains("Metronidazol")) {
                warnings.add("⚠ CONTRAINDICADO en hepatopatía grave — usar clindamicina como alternativa");
            } else {
                warnings.add("Hepatopatía: reducir dosis según severidad; monitorizar niveles si está disponible");
            }
        }
        if (pregnancy) {
            if (p.displayName().contains("Metronidazol")) {
                warnings.add("⚠ Evitar en 1er trimestre; aceptable en 2º y 3er trimestre si es imprescindible");
            } else if (p.displayName().contains("Amoxicilina")) {
                warnings.add("Categoría B en embarazo — es la primera elección");
            } else if (p.displayName().contains("Clindamicina")) {
                warnings.add("Categoría B en embarazo — aceptable cuando penicilinas están contraindicadas");
            } else if (p.displayName().contains("Azitromicina")) {
                warnings.add("Categoría B en embarazo — usar solo si beneficio supera riesgo");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Antibiótico: ").append(p.displayName()).append("\n");
        sb.append("Paciente: ").append(weightKg).append(" kg");
        if (ageYears >= 0) sb.append(", ").append(ageYears).append(" años");
        sb.append("\n\n");
        sb.append("PAUTA RECOMENDADA:\n");
        sb.append("  • Dosis: ").append(dose).append("\n");
        sb.append("  • Duración: ").append(p.duration()).append("\n");

        if (!warnings.isEmpty()) {
            sb.append("\nConsideraciones clínicas:\n");
            warnings.forEach(w -> sb.append("  • ").append(w).append("\n"));
        }
        sb.append("\nNota: ").append(p.clinicalNote());
        return sb.toString();
    }

    private AnestheticProfile findAnesthetic(String normalized) {
        if (ANESTHETICS.containsKey(normalized)) return ANESTHETICS.get(normalized);
        for (Map.Entry<String, AnestheticProfile> e : ANESTHETICS.entrySet()) {
            if (normalized.contains(e.getKey()) || e.getKey().contains(normalized)) return e.getValue();
        }
        return null;
    }

    private String normalize(String drug) {
        if (drug == null) return null;
        return drug.toLowerCase().trim()
                .replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("á", "a")
                .replace("ñ", "n").replaceAll("\\s+", " ");
    }
}
