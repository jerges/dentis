package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.FolderType;
import com.adakadavra.dentis.documents.domain.model.ResourceType;
import com.adakadavra.dentis.documents.domain.model.Visibility;
import com.adakadavra.dentis.documents.domain.repository.DocumentFolderRepository;
import com.adakadavra.dentis.documents.domain.repository.DocumentShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentFolderService {

    private final DocumentFolderRepository folderRepository;
    private final DocumentShareRepository shareRepository;

    public DocumentFolder create(UUID clinicId, UUID ownerUserId, UUID parentId,
                                 String name, Visibility visibility) {
        String path = buildPath(clinicId, parentId, name);
        DocumentFolder folder = DocumentFolder.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .parentId(parentId)
                .name(name)
                .path(path)
                .type(FolderType.NORMAL)
                .visibility(visibility)
                .ownerUserId(ownerUserId)
                .system(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return folderRepository.save(folder);
    }

    public List<DocumentFolder> listChildren(UUID clinicId, UUID parentId) {
        return folderRepository.findByClinicIdAndParentId(clinicId, parentId);
    }

    public DocumentFolder getById(UUID id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Carpeta no encontrada: " + id));
    }

    public DocumentFolder rename(UUID id, String newName) {
        DocumentFolder folder = getById(id);
        if (folder.isSystem()) {
            throw new IllegalStateException("La carpeta de sistema no puede renombrarse");
        }
        folder.setName(newName);
        folder.setUpdatedAt(LocalDateTime.now());
        return folderRepository.save(folder);
    }

    public DocumentFolder updateVisibility(UUID id, Visibility visibility) {
        DocumentFolder folder = getById(id);
        if (folder.isSystem()) {
            throw new IllegalStateException("La visibilidad de la carpeta de sistema no puede modificarse");
        }
        folder.setVisibility(visibility);
        folder.setUpdatedAt(LocalDateTime.now());
        return folderRepository.save(folder);
    }

    public void delete(UUID id) {
        DocumentFolder folder = getById(id);
        if (folder.isSystem()) {
            throw new IllegalStateException("La carpeta de sistema no puede eliminarse");
        }
        shareRepository.deleteByResourceTypeAndResourceId(ResourceType.FOLDER, id);
        folderRepository.deleteById(id);
    }

    /**
     * Obtiene (o crea) la carpeta Base de Conocimiento para la clínica.
     * Es la carpeta de sistema que alimenta la IA.
     */
    public DocumentFolder getOrCreateKnowledgeBase(UUID clinicId) {
        Optional<DocumentFolder> existing = folderRepository.findByClinicIdAndType(clinicId, FolderType.KNOWLEDGE_BASE);
        if (existing.isPresent()) return existing.get();

        DocumentFolder kb = DocumentFolder.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .parentId(null)
                .name("Base de Conocimiento")
                .path("/Base de Conocimiento")
                .type(FolderType.KNOWLEDGE_BASE)
                .visibility(Visibility.PRIVATE)
                .ownerUserId(null)  // de sistema, no tiene propietario
                .system(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return folderRepository.save(kb);
    }

    private String buildPath(UUID clinicId, UUID parentId, String name) {
        if (parentId == null) return "/" + name;
        DocumentFolder parent = folderRepository.findById(parentId)
                .orElseThrow(() -> new NoSuchElementException("Carpeta padre no encontrada: " + parentId));
        return parent.getPath() + "/" + name;
    }
}
