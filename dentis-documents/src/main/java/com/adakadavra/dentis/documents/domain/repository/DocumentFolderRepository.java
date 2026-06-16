package com.adakadavra.dentis.documents.domain.repository;

import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.FolderType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentFolderRepository {

    DocumentFolder save(DocumentFolder folder);

    Optional<DocumentFolder> findById(UUID id);

    List<DocumentFolder> findByClinicIdAndParentId(UUID clinicId, UUID parentId);

    Optional<DocumentFolder> findByClinicIdAndType(UUID clinicId, FolderType type);

    void deleteById(UUID id);
}
