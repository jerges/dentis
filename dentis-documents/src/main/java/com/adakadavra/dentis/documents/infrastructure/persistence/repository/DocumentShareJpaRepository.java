package com.adakadavra.dentis.documents.infrastructure.persistence.repository;

import com.adakadavra.dentis.documents.domain.model.ResourceType;
import com.adakadavra.dentis.documents.infrastructure.persistence.entity.DocumentShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentShareJpaRepository extends JpaRepository<DocumentShareEntity, UUID> {

    Optional<DocumentShareEntity> findByResourceTypeAndResourceIdAndSharedWithUserId(
            ResourceType resourceType, UUID resourceId, UUID sharedWithUserId);

    List<DocumentShareEntity> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    boolean existsByResourceTypeAndResourceIdAndSharedWithUserId(
            ResourceType resourceType, UUID resourceId, UUID sharedWithUserId);

    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentShareEntity s WHERE s.resourceType = :resourceType AND s.resourceId = :resourceId")
    void deleteByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);
}