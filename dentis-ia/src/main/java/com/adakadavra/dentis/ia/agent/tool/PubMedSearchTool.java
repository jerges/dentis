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
public class PubMedSearchTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(PubMedSearchTool.class);
    private static final String ESEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmax=5&retmode=json&term=";
    private static final String EFETCH_URL  = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=abstract&id=";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().build();

    @Override
    public String name() { return "pubmed_search"; }

    @Override
    public String description() {
        return "Searches PubMed for peer-reviewed dental and medical literature. Use when the dentist needs evidence-based information, clinical guidelines, or scientific references about treatments, diagnoses, or medications.";
    }

    @Override
    public String label() { return "Buscando en PubMed..."; }

    @Override
    public Document inputSchema() {
        return Document.mapBuilder()
                .putString("type", "object")
                .putDocument("properties", Document.mapBuilder()
                        .putDocument("query", Document.mapBuilder()
                                .putString("type", "string")
                                .putString("description", "Medical or dental search query in English (e.g. 'caries treatment evidence', 'dental implant osseointegration')")
                                .build())
                        .build())
                .putDocument("required", Document.listBuilder().addString("query").build())
                .build();
    }

    @Override
    public String execute(Map<String, Object> input) {
        String query = (String) input.get("query");
        if (query == null || query.isBlank()) return "No query provided.";
        try {
            String encoded = URLEncoder.encode(query + " dentistry", StandardCharsets.UTF_8);
            String searchBody = HTTP.send(
                    HttpRequest.newBuilder(URI.create(ESEARCH_URL + encoded)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body();

            JsonNode ids = MAPPER.readTree(searchBody).path("esearchresult").path("idlist");
            if (ids.isEmpty()) return "No PubMed results found for: " + query;

            StringBuilder sb = new StringBuilder();
            int max = Math.min(3, ids.size());
            for (int i = 0; i < max; i++) {
                String pmid = ids.get(i).asText();
                String xml = HTTP.send(
                        HttpRequest.newBuilder(URI.create(EFETCH_URL + pmid)).GET().build(),
                        HttpResponse.BodyHandlers.ofString()).body();
                String title = extractXmlTag(xml, "ArticleTitle");
                String abs   = extractXmlTag(xml, "AbstractText");
                if (!title.isBlank()) {
                    sb.append("PMID:").append(pmid).append(" — ").append(title).append("\n");
                    if (!abs.isBlank()) {
                        sb.append(abs, 0, Math.min(500, abs.length())).append("...\n\n");
                    }
                }
            }
            return sb.isEmpty() ? "No abstracts available." : sb.toString().trim();
        } catch (Exception e) {
            log.error("[pubmed] search failed: {}", e.getMessage());
            return "PubMed search failed: " + e.getMessage();
        }
    }

    private String extractXmlTag(String xml, String tag) {
        int start = xml.indexOf("<" + tag);
        if (start == -1) return "";
        int contentStart = xml.indexOf(">", start) + 1;
        int end = xml.indexOf("</" + tag + ">", contentStart);
        if (end == -1) return "";
        return xml.substring(contentStart, end).replaceAll("<[^>]+>", "").trim();
    }
}
