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

@Component
public class ICD10LookupTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(ICD10LookupTool.class);
    private static final String API_URL =
            "https://clinicaltables.nlm.nih.gov/api/icd10cm/v3/search?sf=code,name&maxList=7&terms=";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().build();

    @Override
    public String name() { return "icd10_lookup"; }

    @Override
    public String description() {
        return "Looks up ICD-10-CM diagnosis codes using the NIH Clinical Tables API. "
             + "Use when the dentist needs the official code for a dental/medical diagnosis "
             + "for clinical records, referrals, or insurance billing.";
    }

    @Override
    public String label() { return "Buscando código ICD-10..."; }

    @Override
    public Document inputSchema() {
        return Document.mapBuilder()
                .putString("type", "object")
                .putDocument("properties", Document.mapBuilder()
                        .putDocument("query", Document.mapBuilder()
                                .putString("type", "string")
                                .putString("description",
                                        "Dental or medical diagnosis in English "
                                        + "(e.g. 'dental caries', 'chronic periodontitis', 'pulpitis', "
                                        + "'tooth abscess', 'oral candidiasis', 'temporomandibular joint disorder')")
                                .build())
                        .build())
                .putDocument("required", Document.listBuilder().addString("query").build())
                .build();
    }

    @Override
    public String execute(Map<String, Object> input) {
        String query = (String) input.get("query");
        if (query == null || query.isBlank()) return "Por favor especifica el diagnóstico a buscar.";
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String body = HTTP.send(
                    HttpRequest.newBuilder(URI.create(API_URL + encoded)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body();

            // Response: [total, [code,...], null, [[code, desc], ...]]
            JsonNode root = MAPPER.readTree(body);
            int total = root.get(0).asInt();
            if (total == 0) return "No se encontraron códigos ICD-10-CM para: " + query;

            JsonNode items = root.get(3);
            StringBuilder sb = new StringBuilder("Códigos ICD-10-CM para \"").append(query).append("\":\n\n");
            items.forEach(item -> {
                String code = item.get(0).asText();
                String desc = item.get(1).asText();
                sb.append("• ").append(code).append("  —  ").append(desc).append("\n");
            });
            sb.append("\nFuente: NIH Clinical Tables (ICD-10-CM)");
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("[icd10] lookup failed: {}", e.getMessage());
            return "Búsqueda ICD-10 fallida: " + e.getMessage();
        }
    }
}
