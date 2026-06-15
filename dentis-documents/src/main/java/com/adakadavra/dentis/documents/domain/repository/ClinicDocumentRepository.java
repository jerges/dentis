package com.adakadavra.dentis.documents.domain.repository;

import com.adakadavra.dentis.documents.domain.model.ClinicDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicDocumentRepository {

    ClinicDocument save(ClinicDocument document);

    Optional<ClinicDocument> findById(UUID id);

    List<ClinicDocument> findByFolderId(UUID folderId);

    List<ClinicDocument> findByClinicId(UUID clinicId);

    /** Full-text search scoped to a clinic. */
    List<ClinicDocument> search(UUID clinicId, String query);

    void deleteById(UUID id);

    void markIndexed(UUID id);
}
