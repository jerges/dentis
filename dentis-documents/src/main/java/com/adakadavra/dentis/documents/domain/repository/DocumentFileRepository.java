package com.adakadavra.dentis.documents.domain.repository;

import com.adakadavra.dentis.documents.domain.model.DocumentFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentFileRepository {

    DocumentFile save(DocumentFile file);

    Optional<DocumentFile> findById(UUID id);

    List<DocumentFile> findByFolderId(UUID folderId);

    List<DocumentFile> findByClinicId(UUID clinicId);

    List<DocumentFile> findByFolderIdAndIndexedForIaTrue(UUID folderId);

    void deleteById(UUID id);
}
