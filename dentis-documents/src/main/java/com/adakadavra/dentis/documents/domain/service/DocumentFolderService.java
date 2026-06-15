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
public class DocumentFolderService {

    private final DocumentFolderRepository folderRepo;
    private final ClinicDocumentRepository documentRepo;

    /** Creates the immutable root knowledge-base folder when a clinic is first set up. */
    @Transactional
    public DocumentFolder ensureKnowledgeBaseRoot(UUID clinicId, UUID createdBy) {
        return folderRepo.findByClinicIdAndZone(clinicId, DocumentZone.KNOWLEDGE_BASE)
                .stream()
                .filter(f -> f.getParentId() == null)
                .findFirst()
                .orElseGet(() -> folderRepo.save(DocumentFolder.builder()
                        .clinicId(clinicId)
                        .parentId(null)
                        .name("Base de Conocimiento IA")
                        .s3Prefix("clinics/" + clinicId + "/knowledge-base/")
                        .zone(DocumentZone.KNOWLEDGE_BASE)
                        .system(true)
                        .createdBy(createdBy)
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    @Transactional
    public DocumentFolder createFolder(UUID clinicId, UUID parentId, String name,
                                       DocumentZone zone, UUID createdBy) {
        if (folderRepo.existsByClinicIdAndParentIdAndName(clinicId, parentId, name)) {
            throw new IllegalArgumentException("A folder named '" + name + "' already exists here");
        }
        String parentPrefix = parentId == null
                ? buildRootPrefix(clinicId, zone)
                : folderRepo.findById(parentId)
                        .map(DocumentFolder::getS3Prefix)
                        .orElseThrow(() -> new NoSuchElementException("Parent folder not found: " + parentId));

        return folderRepo.save(DocumentFolder.builder()
                .clinicId(clinicId)
                .parentId(parentId)
                .name(name)
                .s3Prefix(parentPrefix + sanitize(name) + "/")
                .zone(zone)
                .system(false)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public List<DocumentFolder> listChildren(UUID clinicId, UUID parentId) {
        return folderRepo.findByClinicIdAndParentId(clinicId, parentId);
    }

    @Transactional
    public void deleteFolder(UUID folderId, UUID clinicId) {
        DocumentFolder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new NoSuchElementException("Folder not found: " + folderId));
        assertBelongsToClinic(folder, clinicId);
        if (folder.isSystem()) {
            throw new IllegalStateException("The knowledge-base root folder cannot be deleted");
        }
        folderRepo.deleteById(folderId);
    }

    public DocumentFolder getFolder(UUID folderId, UUID clinicId) {
        DocumentFolder folder = folderRepo.findById(folderId)
                .orElseThrow(() -> new NoSuchElementException("Folder not found: " + folderId));
        assertBelongsToClinic(folder, clinicId);
        return folder;
    }

    private void assertBelongsToClinic(DocumentFolder folder, UUID clinicId) {
        if (!folder.getClinicId().equals(clinicId)) {
            throw new IllegalStateException("Folder does not belong to this clinic");
        }
    }

    private String buildRootPrefix(UUID clinicId, DocumentZone zone) {
        return "clinics/" + clinicId + "/" + zone.name().toLowerCase().replace('_', '-') + "/";
    }

    private String sanitize(String name) {
        return name.trim().toLowerCase().replaceAll("[^a-z0-9áéíóúüñ\\-]", "-").replaceAll("-+", "-");
    }
}
