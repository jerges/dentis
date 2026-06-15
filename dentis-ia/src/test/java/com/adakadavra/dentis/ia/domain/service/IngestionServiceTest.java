package com.adakadavra.dentis.ia.domain.service;

import com.adakadavra.dentis.ia.infrastructure.extraction.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock VectorStore vectorStore;
    @Mock TokenTextSplitter textSplitter;
    @Mock PdfTextExtractor pdfExtractor;
    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks
    IngestionService service;

    private final UUID attachmentId = UUID.randomUUID();
    private final UUID clinicId     = UUID.randomUUID();
    private final String s3Key      = "clinic/" + clinicId + "/doc.pdf";
    private final Function<String, byte[]> loader = key -> "contenido".getBytes();

    // ── Supported content type ────────────────────────────────────────────────

    @Test
    void unsupportedContentTypeIsSkipped() {
        given(pdfExtractor.isSupported("image/png")).willReturn(false);

        service.ingestAsync(attachmentId, clinicId, s3Key, "image/png", loader);

        verify(vectorStore, never()).add(any());
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void happyPathDeletesThenIngestsChunks() {
        given(pdfExtractor.isSupported("application/pdf")).willReturn(true);
        given(pdfExtractor.extractText(any(), eq("application/pdf"))).willReturn("texto extraído del pdf");

        Document chunk1 = new Document("chunk uno");
        Document chunk2 = new Document("chunk dos");
        given(textSplitter.apply(any())).willReturn(List.of(chunk1, chunk2));

        service.ingestAsync(attachmentId, clinicId, s3Key, "application/pdf", loader);

        verify(jdbcTemplate).update(
                "DELETE FROM ia_documents WHERE metadata->>'attachment_id' = ?",
                attachmentId.toString());

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.captor();
        verify(vectorStore).add(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void metadataIsSetOnDocumentPassedToSplitter() {
        given(pdfExtractor.isSupported("application/pdf")).willReturn(true);
        given(pdfExtractor.extractText(any(), any())).willReturn("texto");
        given(textSplitter.apply(any())).willReturn(List.of(new Document("chunk")));

        service.ingestAsync(attachmentId, clinicId, s3Key, "application/pdf", loader);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.captor();
        verify(textSplitter).apply(captor.capture());
        Document input = captor.getValue().get(0);
        assertThat(input.getMetadata())
                .containsEntry("clinic_id", clinicId.toString())
                .containsEntry("attachment_id", attachmentId.toString())
                .containsEntry("s3_key", s3Key);
    }

    // ── Empty extraction ──────────────────────────────────────────────────────

    @Test
    void blankExtractionSkipsIngestion() {
        given(pdfExtractor.isSupported("application/pdf")).willReturn(true);
        given(pdfExtractor.extractText(any(), any())).willReturn("   ");

        service.ingestAsync(attachmentId, clinicId, s3Key, "application/pdf", loader);

        verify(vectorStore, never()).add(any());
    }

    // ── Error resilience ──────────────────────────────────────────────────────

    @Test
    void s3LoaderFailureDoesNotPropagateException() {
        given(pdfExtractor.isSupported("application/pdf")).willReturn(true);
        Function<String, byte[]> failingLoader = key -> { throw new RuntimeException("S3 error"); };

        // should not throw
        service.ingestAsync(attachmentId, clinicId, s3Key, "application/pdf", failingLoader);

        verify(vectorStore, never()).add(any());
    }

    // ── Reindex ───────────────────────────────────────────────────────────────

    @Test
    void reindexClinicProcessesEachAttachment() {
        given(pdfExtractor.isSupported(anyString())).willReturn(true);
        given(pdfExtractor.extractText(any(), any())).willReturn("texto");
        given(textSplitter.apply(any())).willReturn(List.of(new Document("chunk")));

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<IngestionService.AttachmentRef> refs = List.of(
                new IngestionService.AttachmentRef(id1, "k1.pdf", "application/pdf"),
                new IngestionService.AttachmentRef(id2, "k2.pdf", "application/pdf")
        );

        service.reindexClinic(clinicId, refs, loader);

        verify(vectorStore, org.mockito.Mockito.times(2)).add(any());
    }
}
