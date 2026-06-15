package com.adakadavra.dentis.ia.infrastructure.extraction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    // ── isSupported ───────────────────────────────────────────────────────────

    @Test
    void pdfContentTypeIsSupported() {
        assertThat(extractor.isSupported("application/pdf")).isTrue();
    }

    @Test
    void textPlainIsSupported() {
        assertThat(extractor.isSupported("text/plain")).isTrue();
    }

    @Test
    void textHtmlIsSupported() {
        assertThat(extractor.isSupported("text/html")).isTrue();
    }

    @Test
    void imageContentTypeIsNotSupported() {
        assertThat(extractor.isSupported("image/png")).isFalse();
    }

    @Test
    void nullContentTypeIsNotSupported() {
        assertThat(extractor.isSupported(null)).isFalse();
    }

    // ── extractText: text/* ───────────────────────────────────────────────────

    @Test
    void textContentIsReturnedAsString() {
        byte[] bytes = "Caries interproximal en molar superior.".getBytes();

        String result = extractor.extractText(bytes, "text/plain");

        assertThat(result).isEqualTo("Caries interproximal en molar superior.");
    }

    @Test
    void textContentPreservesLineBreaks() {
        byte[] bytes = "línea 1\nlínea 2".getBytes();

        String result = extractor.extractText(bytes, "text/csv");

        assertThat(result).contains("línea 1").contains("línea 2");
    }

    // ── extractText: invalid PDF ──────────────────────────────────────────────

    @Test
    void invalidPdfBytesReturnsEmptyString() {
        byte[] notAPdf = "esto no es un pdf".getBytes();

        String result = extractor.extractText(notAPdf, "application/pdf");

        assertThat(result).isEmpty();
    }

    @Test
    void emptyBytesForPdfReturnsEmptyString() {
        String result = extractor.extractText(new byte[0], "application/pdf");

        assertThat(result).isEmpty();
    }
}
