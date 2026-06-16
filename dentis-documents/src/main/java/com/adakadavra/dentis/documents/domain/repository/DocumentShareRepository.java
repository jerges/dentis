package com.adakadavra.dentis.documents.domain.repository;

import com.adakadavra.dentis.documents.domain.model.DocumentShare;
import com.adakadavra.dentis.documents.domain.model.ResourceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentShareRepository {

    DocumentShare save(DocumentShare share);

    Optional<DocumentShare> findByResourceTypeAndResourceIdAndSharedWithUserId(
            ResourceType resourceType, UUID resourceId, UUID sharedWithUserId);

    List<DocumentShare> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    boolean existsByResourceTypeAndResourceIdAndSharedWithUserId(
            ResourceType resourceType, UUID resourceId, UUID sharedWithUserId);

    void deleteById(UUID id);

    void deleteByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);
}
