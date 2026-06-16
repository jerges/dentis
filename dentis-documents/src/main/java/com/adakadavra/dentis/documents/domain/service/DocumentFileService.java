package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentFile;
import com.adakadavra.dentis.documents.domain.model.ResourceType;
import com.adakadavra.dentis.documents.domain.model.Visibility;
import com.adakadavra.dentis.documents.domain.repository.DocumentFileRepository;
import com.adakadavra.dentis.documents.domain.repository.DocumentShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentFileService {

    private final DocumentFileRepository fileRepository;
    private final DocumentShareRepository shareRepository;

    public DocumentFile register(UUID clinicId, UUID folderId, UUID ownerUserId,
                                 String name, String fileName, String contentType,
                                 Long fileSize, String s3Key, Visibility visibility) {
        DocumentFile file = DocumentFile.builder()
                .id(UUID.randomUUID())
                .clinicId(clinicId)
                .folderId(folderId)
                .name(name)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(fileSize)
                .s3Key(s3Key)
                .visibility(visibility)
                .ownerUserId(ownerUserId)
                .indexedForIa(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return fileRepository.save(file);
    }

    public DocumentFile getById(UUID id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fichero no encontrado: " + id));
    }

    public List<DocumentFile> listByFolder(UUID folderId) {
        return fileRepository.findByFolderId(folderId);
    }

    public List<DocumentFile> listKnowledgeBaseFiles(UUID folderId) {
        return fileRepository.findByFolderIdAndIndexedForIaTrue(folderId);
    }

    public DocumentFile updateVisibility(UUID id, Visibility visibility) {
        DocumentFile file = getById(id);
        file.setVisibility(visibility);
        file.setUpdatedAt(LocalDateTime.now());
        return fileRepository.save(file);
    }

    public DocumentFile markIndexed(UUID id) {
        DocumentFile file = getById(id);
        file.setIndexedForIa(true);
        file.setUpdatedAt(LocalDateTime.now());
        return fileRepository.save(file);
    }

    public DocumentFile move(UUID id, UUID targetFolderId) {
        DocumentFile file = getById(id);
        file.setFolderId(targetFolderId);
        file.setIndexedForIa(false); // requiere re-ingesta si cambia de carpeta
        file.setUpdatedAt(LocalDateTime.now());
        return fileRepository.save(file);
    }

    public void delete(UUID id) {
        fileRepository.findById(id).ifPresent(f -> {
            shareRepository.deleteByResourceTypeAndResourceId(ResourceType.FILE, id);
            fileRepository.deleteById(id);
        });
    }
}
