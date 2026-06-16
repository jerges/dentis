package com.adakadavra.dentis.ia.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Queries the official FDA OpenFDA drug label API.
 * No API key required (free tier: 240 req/min).
 * Source: https://api.fda.gov/drug/label.json
 */
@Component
public class FDADrugInfoTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(FDADrugInfoTool.class);
    private static final String API_URL = "https://api.fda.gov/drug/label.json?limit=1&search=openfda.generic_name:";
    private static final int MAX_FIELD_LENGTH = 600;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().build();

    @Override
    public String name() { return "fda_drug_info"; }

    @Override
    public String description() {
        return "Retrieves official FDA drug label information (contraindications, warnings, "
             + "drug interactions, pregnancy category, special populations) from the OpenFDA database. "
             + "Use when precise, official safety information is needed for a dental medication.";
    }

    @Override
    public String label() { return "Consultando ficha técnica FDA..."; }

    @Override
    public Document inputSchema() {
        return Document.mapBuilder()
                .putString("type", "object")
                .putDocument("properties", Document.mapBuilder()
                        .putDocument("drug", Document.mapBuilder()
                                .putString("type", "string")
                                .putString("description",
                                        "Generic drug name in English "
                                        + "(e.g. 'amoxicillin', 'metronidazole', 'clindamycin', "
                                        + "'ibuprofen', 'lidocaine', 'azithromycin')")
                                .build())
                        .build())
                .putDocument("required", Document.listBuilder().addString("drug").build())
                .build();
    }

    @Override
    public String execute(Map<String, Object> input) {
        String drug = (String) input.get("drug");
        if (drug == null || drug.isBlank()) return "Por favor especifica el nombre genérico del medicamento.";
        try {
            String encoded = URLEncoder.encode("\"" + drug.trim().toLowerCase() + "\"", StandardCharsets.UTF_8);
            String body = HTTP.send(
                    HttpRequest.newBuilder(URI.create(API_URL + encoded)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body();

            JsonNode root = MAPPER.readTree(body);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                return "No se encontró información FDA para: " + drug + ". Verifique que sea el nombre genérico en inglés.";
            }

            JsonNode label = root.path("results").get(0);
            if (label == null || label.isMissingNode()) {
                return "No se encontraron resultados FDA para: " + drug;
            }

            String genericName  = firstText(label, "openfda", "generic_name");
            String brandName    = firstText(label, "openfda", "brand_name");

            StringBuilder sb = new StringBuilder();
            sb.append("Ficha técnica FDA — ").append(genericName.isBlank() ? drug : genericName.toUpperCase());
            if (!brandName.isBlank()) sb.append(" (").append(brandName).append(")");
            sb.append("\nFuente: OpenFDA (api.fda.gov) — Etiqueta oficial FDA\n\n");

            appendSection(sb, label, "CONTRAINDICACIONES", "contraindications");
            appendSection(sb, label, "ADVERTENCIAS Y PRECAUCIONES", "warnings_and_cautions", "warnings");
            appendSection(sb, label, "INTERACCIONES FARMACOLÓGICAS", "drug_interactions");
            appendSection(sb, label, "USO EN EMBARAZO", "pregnancy");
            appendSection(sb, label, "USO EN INSUFICIENCIA RENAL/HEPÁTICA", "use_in_specific_populations");
            appendSection(sb, label, "REACCIONES ADVERSAS", "adverse_reactions");

            return sb.toString().trim();
        } catch (Exception e) {
            log.error("[fda] drug info failed for {}: {}", drug, e.getMessage());
            return "Consulta FDA fallida para '" + drug + "': " + e.getMessage();
        }
    }

    private void appendSection(StringBuilder sb, JsonNode label, String title, String... fields) {
        for (String field : fields) {
            JsonNode node = label.path(field);
            if (!node.isMissingNode() && node.isArray() && node.size() > 0) {
                String text = node.get(0).asText().replaceAll("\\s+", " ").strip();
                if (!text.isBlank()) {
                    sb.append(title).append(":\n");
                    sb.append(text, 0, Math.min(text.length(), MAX_FIELD_LENGTH));
                    if (text.length() > MAX_FIELD_LENGTH) sb.append("...");
                    sb.append("\n\n");
                    return;
                }
            }
        }
    }

    private String firstText(JsonNode root, String... path) {
        JsonNode node = root;
        for (String key : path) {
            node = node.path(key);
        }
        if (node.isArray() && node.size() > 0) return node.get(0).asText();
        return node.isTextual() ? node.asText() : "";
    }
}
