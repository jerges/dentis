package com.adakadavra.dentis.documents.domain.service;

import com.adakadavra.dentis.documents.domain.model.DocumentShare;
import com.adakadavra.dentis.documents.domain.model.ResourceType;
import com.adakadavra.dentis.documents.domain.repository.DocumentShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentShareService {

    private final DocumentShareRepository shareRepository;

    public DocumentShare share(ResourceType resourceType, UUID resourceId, UUID sharedWithUserId) {
        if (shareRepository.existsByResourceTypeAndResourceIdAndSharedWithUserId(
                resourceType, resourceId, sharedWithUserId)) {
            return shareRepository.findByResourceTypeAndResourceIdAndSharedWithUserId(
                    resourceType, resourceId, sharedWithUserId).orElseThrow();
        }
        DocumentShare share = DocumentShare.builder()
                .id(UUID.randomUUID())
                .resourceType(resourceType)
                .resourceId(resourceId)
                .sharedWithUserId(sharedWithUserId)
                .createdAt(LocalDateTime.now())
                .build();
        return shareRepository.save(share);
    }

    public List<DocumentShare> listShares(ResourceType resourceType, UUID resourceId) {
        return shareRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    public void removeShare(UUID shareId) {
        shareRepository.deleteById(shareId);
    }
}
