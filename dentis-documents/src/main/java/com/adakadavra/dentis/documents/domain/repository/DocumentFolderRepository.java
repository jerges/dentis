package com.adakadavra.dentis.documents.domain.repository;

import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.DocumentZone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentFolderRepository {

    DocumentFolder save(DocumentFolder folder);

    Optional<DocumentFolder> findById(UUID id);

    List<DocumentFolder> findByClinicIdAndParentId(UUID clinicId, UUID parentId);

    List<DocumentFolder> findByClinicIdAndZone(UUID clinicId, DocumentZone zone);

    boolean existsByClinicIdAndParentIdAndName(UUID clinicId, UUID parentId, String name);

    void deleteById(UUID id);
}
