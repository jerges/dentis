package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.domain.model.DocumentShare;
import com.adakadavra.dentis.documents.domain.model.ResourceType;
import com.adakadavra.dentis.documents.domain.repository.DocumentShareRepository;
import com.adakadavra.dentis.documents.infrastructure.persistence.mapper.DocumentEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentShareRepositoryAdapter implements DocumentShareRepository {

    private final DocumentShareJpaRepository jpa;
    private final DocumentEntityMapper mapper;

    @Override
    public DocumentShare save(DocumentShare share) {
        return mapper.toDomain(jpa.save(mapper.toEntity(share)));
    }

    @Override
    public Optional<DocumentShare> findByResourceTypeAndResourceIdAndSharedWithUserId(
            ResourceType resourceType, UUID resourceId, UUID sharedWithUserId) {
        return jpa.findByResourceTypeAndResourceIdAndSharedWithUserId(resourceType, resourceId, sharedWithUserId)
                .map(mapper::toDomain);
    }

    @Override
    public List<DocumentShare> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId) {
        return jpa.findByResourceTypeAndResourceId(resourceType, resourceId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByResourceTypeAndResourceIdAndSharedWithUserId(
            ResourceType resourceType, UUID resourceId, UUID sharedWithUserId) {
        return jpa.existsByResourceTypeAndResourceIdAndSharedWithUserId(resourceType, resourceId, sharedWithUserId);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public void deleteByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId) {
        jpa.deleteByResourceTypeAndResourceId(resourceType, resourceId);
    }
}