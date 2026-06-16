package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.domain.model.DocumentFolder;
import com.adakadavra.dentis.documents.domain.model.FolderType;
import com.adakadavra.dentis.documents.domain.repository.DocumentFolderRepository;
import com.adakadavra.dentis.documents.infrastructure.persistence.mapper.DocumentEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentFolderRepositoryAdapter implements DocumentFolderRepository {

    private final DocumentFolderJpaRepository jpa;
    private final DocumentEntityMapper mapper;

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
        return jpa.findByClinicIdAndParentId(clinicId, parentId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<DocumentFolder> findByClinicIdAndType(UUID clinicId, FolderType type) {
        return jpa.findByClinicIdAndType(clinicId, type).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}