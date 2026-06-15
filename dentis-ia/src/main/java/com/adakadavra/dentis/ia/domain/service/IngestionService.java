package com.adakadavra.dentis.ia.domain.service;

import com.adakadavra.dentis.ia.infrastructure.extraction.PdfTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final PdfTextExtractor pdfExtractor;
    private final JdbcTemplate jdbcTemplate;

    @Async
    public void ingestAsync(UUID attachmentId, UUID clinicId, String s3Key,
                            String contentType, Function<String, byte[]> s3Loader) {
        if (!pdfExtractor.isSupported(contentType)) {
            log.debug("Skipping ingestion for unsupported content type: {}", contentType);
            return;
        }
        try {
            jdbcTemplate.update(
                    "DELETE FROM ia_documents WHERE metadata->>'attachment_id' = ?",
                    attachmentId.toString());

            byte[] bytes = s3Loader.apply(s3Key);
            String text = pdfExtractor.extractText(bytes, contentType);
            if (text.isBlank()) {
                log.warn("Empty text extracted from attachment {}", attachmentId);
                return;
            }

            Map<String, Object> metadata = Map.of(
                    "clinic_id", clinicId.toString(),
                    "attachment_id", attachmentId.toString(),
                    "s3_key", s3Key
            );
            List<Document> docs = textSplitter.apply(List.of(new Document(text, metadata)));
            vectorStore.add(docs);
            log.info("Ingested {} chunks for attachment {} (clinic {})", docs.size(), attachmentId, clinicId);
        } catch (Exception e) {
            log.error("Ingestion failed for attachment {}: {}", attachmentId, e.getMessage(), e);
        }
    }

    public void reindexClinic(UUID clinicId, List<AttachmentRef> attachments,
                              Function<String, byte[]> s3Loader) {
        for (AttachmentRef ref : attachments) {
            ingestAsync(ref.attachmentId(), clinicId, ref.s3Key(), ref.contentType(), s3Loader);
        }
    }

    public record AttachmentRef(UUID attachmentId, String s3Key, String contentType) {}
}
