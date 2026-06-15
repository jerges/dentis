package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.ClinicDocument;
import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.DocumentZone;
import com.adakadavra.dentis.documents.domain.repository.ClinicDocumentRepository;
import com.adakadavra.dentis.documents.domain.repository.DocumentFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicDocumentService {

    private final ClinicDocumentRepository documentRepo;
    private final DocumentFolderRepository folderRepo;

    public record PresignRequest(UUID folderId, String fileName, String contentType) {}
    public record PresignResult(String uploadUrl, String s3Key) {}

    /** Builds the S3 key for the file and returns it alongside a pre-signed upload URL. */
    public PresignResult buildPresignInfo(UUID clinicId, PresignRequest req) {
        DocumentFolder folder = folderRepo.findById(req.folderId())
                .orElseThrow(() -> new NoSuchElementException("Folder not found"));
        assertBelongsToClinic(folder, clinicId);
        String key = folder.getS3Prefix() + UUID.randomUUID() + "-" + sanitize(req.fileName());
        return new PresignResult(null, key); // uploadUrl is filled by the API layer (S3StorageService)
    }

    @Transactional
    public ClinicDocument register(UUID clinicId, UUID folderId, String fileName,
                                   String contentType, String s3Key, Long fileSize,
                                   String description, UUID uploadedBy) {
        DocumentFolder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new NoSuchElementException("Folder not found"));
        assertBelongsToClinic(folder, clinicId);

        return documentRepo.save(ClinicDocument.builder()
                .clinicId(clinicId)
                .folderId(folderId)
                .fileName(fileName)
                .contentType(contentType)
                .s3Key(s3Key)
                .fileSize(fileSize)
                .description(description)
                .uploadedBy(uploadedBy)
                .uploadedAt(LocalDateTime.now())
                .indexedForIa(false)
                .build());
    }

    public List<ClinicDocument> listByFolder(UUID folderId, UUID clinicId) {
        DocumentFolder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new NoSuchElementException("Folder not found"));
        assertBelongsToClinic(folder, clinicId);
        return documentRepo.findByFolderId(folderId);
    }

    public List<ClinicDocument> search(UUID clinicId, String query) {
        return documentRepo.search(clinicId, query);
    }

    public ClinicDocument getDocument(UUID documentId, UUID clinicId) {
        ClinicDocument doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + documentId));
        if (!doc.getClinicId().equals(clinicId)) {
            throw new IllegalStateException("Document does not belong to this clinic");
        }
        return doc;
    }

    @Transactional
    public String deleteDocument(UUID documentId, UUID clinicId) {
        ClinicDocument doc = getDocument(documentId, clinicId);
        documentRepo.deleteById(documentId);
        return doc.getS3Key();
    }

    @Transactional
    public void markIndexed(UUID documentId) {
        documentRepo.markIndexed(documentId);
    }

    public boolean isKnowledgeBaseFolder(UUID folderId) {
        return folderRepo.findById(folderId)
                .map(f -> f.getZone() == DocumentZone.KNOWLEDGE_BASE)
                .orElse(false);
    }

    private void assertBelongsToClinic(DocumentFolder folder, UUID clinicId) {
        if (!folder.getClinicId().equals(clinicId)) {
            throw new IllegalStateException("Folder does not belong to this clinic");
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
