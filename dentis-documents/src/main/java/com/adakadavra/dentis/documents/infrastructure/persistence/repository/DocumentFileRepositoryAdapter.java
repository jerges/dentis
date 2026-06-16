package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.domain.model.DocumentFile;
import com.adakadavra.dentis.documents.domain.repository.DocumentFileRepository;
import com.adakadavra.dentis.documents.infrastructure.persistence.mapper.DocumentEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentFileRepositoryAdapter implements DocumentFileRepository {

    private final DocumentFileJpaRepository jpa;
    private final DocumentEntityMapper mapper;

    @Override
    public DocumentFile save(DocumentFile file) {
        return mapper.toDomain(jpa.save(mapper.toEntity(file)));
    }

    @Override
    public Optional<DocumentFile> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DocumentFile> findByFolderId(UUID folderId) {
        return jpa.findByFolderId(folderId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DocumentFile> findByClinicId(UUID clinicId) {
        return jpa.findByClinicId(clinicId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DocumentFile> findByFolderIdAndIndexedForIaTrue(UUID folderId) {
        return jpa.findByFolderIdAndIndexedForIaTrue(folderId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}