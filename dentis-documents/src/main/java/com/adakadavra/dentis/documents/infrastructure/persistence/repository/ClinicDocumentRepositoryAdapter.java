package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.domain.model.ClinicDocument;
import com.adakadavra.dentis.documents.domain.repository.ClinicDocumentRepository;
import com.adakadavra.dentis.documents.infrastructure.persistence.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ClinicDocumentRepositoryAdapter implements ClinicDocumentRepository {

    private final ClinicDocumentJpaRepository jpa;
    private final DocumentMapper mapper;

    @Override
    public ClinicDocument save(ClinicDocument document) {
        return mapper.toDomain(jpa.save(mapper.toEntity(document)));
    }

    @Override
    public Optional<ClinicDocument> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ClinicDocument> findByFolderId(UUID folderId) {
        return jpa.findByFolderId(folderId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ClinicDocument> findVisibleByFolderId(UUID folderId, UUID userId) {
        return jpa.findVisibleByFolderId(folderId, userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ClinicDocument> findByClinicId(UUID clinicId) {
        return jpa.findByClinicId(clinicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ClinicDocument> search(UUID clinicId, String query) {
        return jpa.fullTextSearch(clinicId, query).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ClinicDocument> searchVisible(UUID clinicId, String query, UUID userId) {
        return jpa.fullTextSearchVisible(clinicId, query, userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public void markIndexed(UUID id) {
        jpa.markIndexed(id);
    }
}
