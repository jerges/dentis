package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.DocumentZone;
import com.adakadavra.dentis.documents.domain.repository.DocumentFolderRepository;
import com.adakadavra.dentis.documents.infrastructure.persistence.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentFolderRepositoryAdapter implements DocumentFolderRepository {

    private final DocumentFolderJpaRepository jpa;
    private final DocumentMapper mapper;

    @Override
    public DocumentFolder save(DocumentFolder folder) {
        return mapper.toDomain(jpa.save(mapper.toEntity(folder)));
    }

    @Override
    public Optional<DocumentFolder> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DocumentFolder> findByClinicIdAndParentId(UUID clinicId, UUID parentId) {
        return jpa.findByClinicIdAndParentId(clinicId, parentId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DocumentFolder> findVisibleByClinicIdAndParentId(UUID clinicId, UUID parentId, UUID userId) {
        return jpa.findVisibleByClinicIdAndParentId(clinicId, parentId, userId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DocumentFolder> findByClinicIdAndZone(UUID clinicId, DocumentZone zone) {
        return jpa.findByClinicIdAndZone(clinicId, zone).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByClinicIdAndParentIdAndName(UUID clinicId, UUID parentId, String name) {
        return jpa.existsByClinicIdAndParentIdAndName(clinicId, parentId, name);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
