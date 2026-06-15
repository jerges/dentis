package com.adakadavra.dentis.documents.domain.repository;

import com.adakadavra.dentis.documents.domain.model.ClinicDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicDocumentRepository {

    ClinicDocument save(ClinicDocument document);

    Optional<ClinicDocument> findById(UUID id);

    List<ClinicDocument> findByFolderId(UUID folderId);

    /** Returns only PUBLIC docs plus PRIVATE ones uploaded by {@code userId}. */
    List<ClinicDocument> findVisibleByFolderId(UUID folderId, UUID userId);

    List<ClinicDocument> findByClinicId(UUID clinicId);

    /** Full-text search scoped to a clinic (all visibility). */
    List<ClinicDocument> search(UUID clinicId, String query);

    /** Full-text search restricted to PUBLIC docs and user's own PRIVATE docs. */
    List<ClinicDocument> searchVisible(UUID clinicId, String query, UUID userId);

    void deleteById(UUID id);

    void markIndexed(UUID id);
}
