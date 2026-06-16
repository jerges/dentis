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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DrugInteractionTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(DrugInteractionTool.class);
    private static final String RXNORM_RXCUI_URL        = "https://rxnav.nlm.nih.gov/REST/rxcui.json?name=";
    private static final String RXNORM_INTERACTION_URL  = "https://rxnav.nlm.nih.gov/REST/interaction/list.json?rxcuis=";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().build();

    @Override
    public String name() { return "drug_interaction_check"; }

    @Override
    public String description() {
        return "Checks drug-drug interactions using the RxNorm database. Use when a dentist needs to verify if a prescribed dental medication is safe alongside a patient's existing medications.";
    }

    @Override
    public String label() { return "Verificando interacciones farmacológicas..."; }

    @Override
    public Document inputSchema() {
        return Document.mapBuilder()
                .putString("type", "object")
                .putDocument("properties", Document.mapBuilder()
                        .putDocument("drugs", Document.mapBuilder()
                                .putString("type", "array")
                                .putDocument("items", Document.mapBuilder().putString("type", "string").build())
                                .putString("description", "List of at least 2 drug names to check for interactions (e.g. ['amoxicillin', 'warfarin'])")
                                .build())
                        .build())
                .putDocument("required", Document.listBuilder().addString("drugs").build())
                .build();
    }

    @Override
    public String execute(Map<String, Object> input) {
        Object drugsObj = input.get("drugs");
        if (!(drugsObj instanceof List<?> drugList) || drugList.size() < 2) {
            return "Please provide at least 2 drug names to check for interactions.";
        }
        try {
            List<String> rxcuis = new ArrayList<>();
            for (Object drug : drugList) {
                String encoded = URLEncoder.encode(drug.toString(), StandardCharsets.UTF_8);
                String json = HTTP.send(
                        HttpRequest.newBuilder(URI.create(RXNORM_RXCUI_URL + encoded)).GET().build(),
                        HttpResponse.BodyHandlers.ofString()).body();
                JsonNode idNode = MAPPER.readTree(json).path("idGroup").path("rxnormId");
                if (idNode.isArray() && !idNode.isEmpty()) {
                    rxcuis.add(idNode.get(0).asText());
                }
            }
            if (rxcuis.size() < 2) {
                return "Could not resolve enough drug names to RxNorm identifiers. Try using generic drug names.";
            }

            String rxcuiParam = String.join("+", rxcuis);
            String interactionJson = HTTP.send(
                    HttpRequest.newBuilder(URI.create(RXNORM_INTERACTION_URL + rxcuiParam)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body();

            JsonNode groups = MAPPER.readTree(interactionJson).path("fullInteractionTypeGroup");
            if (groups.isMissingNode() || groups.isEmpty()) {
                return "No known interactions found between the specified drugs.";
            }

            StringBuilder sb = new StringBuilder("Drug interactions found:\n");
            groups.forEach(group ->
                    group.path("fullInteractionType").forEach(type ->
                            type.path("interactionPair").forEach(pair -> {
                                String desc = pair.path("description").asText();
                                String severity = pair.path("severity").asText();
                                if (!desc.isBlank()) {
                                    sb.append("• [").append(severity.isBlank() ? "unknown" : severity)
                                      .append("] ").append(desc).append("\n");
                                }
                            })
                    )
            );
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("[rxnorm] drug interaction check failed: {}", e.getMessage());
            return "Drug interaction check failed: " + e.getMessage();
        }
    }
}
