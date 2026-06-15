package com.adakadavra.dentis.ia.infrastructure.extraction;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class PdfTextExtractor {

    public String extractText(byte[] bytes, String contentType) {
        if (contentType != null && contentType.startsWith("text/")) {
            return new String(bytes);
        }
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (IOException e) {
            log.warn("PDF text extraction failed, skipping document: {}", e.getMessage());
            return "";
        }
    }

    public boolean isSupported(String contentType) {
        if (contentType == null) return false;
        return contentType.equals("application/pdf")
                || contentType.startsWith("text/");
    }
}
